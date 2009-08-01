/* $Id$
 *
 * Copyright 2009 Noah Levitt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <glib.h>
#include <gio/gio.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <locale.h>

/* North America-centric, but it should refer us to the right server e.g.
 * "ReferralServer: whois://whois.apnic.net" */
static const char *DEFAULT_IP_WHOIS_SERVER = "whois.arin.net";

/* Look up "com" "net" "fr" "info" etc */
static const char *ULTRA_SUFFIX_WHOIS_SERVER = "whois.iana.org";

/* If whois.iana.org doesn't know of a server for a particular country code,
 * look it up here instead */
static const char *FALLBACK_CCTLD_WHOIS_SERVER = "whois.cocca.cx";

/*
 * [whois://whois.arin.net/192.102.239.53] ReferralServer: whois://whois.apnic.net
 * [whois://whois.arin.net/208.49.199.10] ReferralServer: rwhois://rwhois.gblx.net:4321
 * [whois://whois.arin.net/195.154.120.129] ReferralServer: whois://whois.ripe.net:43
 * [whois://whois.iana.org/fr] Whois Server (port 43): whois.nic.fr
 * [whois://whois.verisign-grs.com/domain%201stbattalion9thmarinesfirebase.net]    Whois Server: whois.fastdomain.com
 */
static const char *REFERRAL_SERVER_REGEX = "^\\s*(?:whois server|ReferralServer)[^:\r\n]*:.*?([a-zA-Z0-9.:-]+)$";

static char *user_specified_server = NULL;
static int user_specified_port = -1;

static GOptionEntry entries[] =
{
  { "host", 'h', 0, G_OPTION_ARG_STRING, &user_specified_server, "Connect to server HOST", "HOST" },
  { "port", 'p', 0, G_OPTION_ARG_INT, &user_specified_port, "Connect to port PORT", "PORT" },
  { NULL }
};

static GRegex *
referral_server_regex ()
{
  static GRegex *regex = NULL;

  if (regex == NULL)
    {
      GError *error = NULL;
      regex = g_regex_new (REFERRAL_SERVER_REGEX, 
                           G_REGEX_CASELESS|G_REGEX_MULTILINE|G_REGEX_RAW, 
                           0, &error);
      if (regex == NULL || error != NULL)
        {
          g_printerr ("g_regex_new (%s): %s\n", REFERRAL_SERVER_REGEX, error->message);
          exit (6);
        }
    }

  return regex;
}

static GSocket *
open_socket (char *server_colon_port,
             int   default_port)
{
  GSocketClient *client = g_socket_client_new ();

  GError *error = NULL;

  GSocketConnection *connection = g_socket_client_connect_to_host (client, server_colon_port, default_port, NULL, &error);
  if (connection == NULL) 
    {
      g_printerr ("g_socket_client_connect_to_host (%s): %s\n", server_colon_port, error->message);
      exit (3);
    }

  return g_socket_connection_get_socket (connection);
}

static void 
send_query (GSocket *socket,
            char    *query)
{
  g_assert (g_socket_is_connected (socket));

  GError *error = NULL;

  /* XXX this needs to be in a loop... */
  gssize bytes_sent = g_socket_send (socket, query, strlen (query), NULL, &error);
  if (bytes_sent == -1) 
    {
      g_printerr ("g_socket_send: %s\n", error->message);
      exit (4);
    }
}

/* return value must be freed */
static char *
get_response (GSocket *socket)
{
  GString *response = g_string_new ("");
  /* static char buf[4096]; */
  static char buf[439];
  GError *error = NULL;
  gssize bytes_received;

  do 
    {
      bytes_received = g_socket_receive (socket, buf, sizeof (buf), NULL, &error);

      if (bytes_received > 0)
        g_string_append_len (response, buf, bytes_received);
      else if (bytes_received < 0)
        {
          g_printerr ("g_socket_receive: %s\n", error->message);
          exit (5);
        }
    }
  while (bytes_received > 0);

  return g_string_free (response, FALSE);
}

/* returns whois response which must be freed */
static char *
simple_lookup (char *server_colon_port,
               int   port,
               char *query)
{
  g_print ("----- [server: %s] [query: \"%s\"] -----\n", server_colon_port, query);
  GString *query_plus_newline = g_string_new (query);
  g_string_append_c (query_plus_newline, '\n');

  GSocket *socket = open_socket (server_colon_port, port);
  send_query (socket, query_plus_newline->str);
  return get_response (socket);
}

/* return value must be freed */
static char *
smart_query_for_server (const char *server_colon_port,
                        const char *query)
{
  GString *smart_query = g_string_new (query);

  if (g_strcmp0 (server_colon_port, "whois.verisign-grs.com") == 0 || 
      g_strcmp0 (server_colon_port, "whois.verisign-grs.com:43") == 0)
    g_string_printf (smart_query, "domain %s", query);

  return g_string_free (smart_query, FALSE);
}

/* Assumes query is either an ip address or domain name. If not, user should
 * specify server on the command line with -h. */
static void
smart_lookup (char *query)
{
  char *next_server;
  char *next_query;
  int next_port = 43;

  if (g_hostname_is_ip_address (query))
    {
      next_server = g_strdup (DEFAULT_IP_WHOIS_SERVER);
      next_query = g_strdup (query);
    }
  else
    {
      next_server = g_strdup (ULTRA_SUFFIX_WHOIS_SERVER);
      char *last_dot = strrchr (query, '.');
      if (last_dot != NULL)
        next_query = g_strdup (last_dot + 1);
      else
        next_query = g_strdup (query);
    }

  while (next_server != NULL)
    {
      char *response = simple_lookup (next_server, next_port, next_query);
      puts (response);

      GMatchInfo *match_info;
      if (g_regex_match (referral_server_regex (), response, 0, &match_info))
        {
          g_free (next_server);
          g_free (next_query);
          next_server = g_match_info_fetch (match_info, 1);
          next_query = smart_query_for_server (next_server, query);
        }
      else if (strlen (next_query) == 2)
        {
          /* country code without its own whois server */
          g_free (next_server);
          g_free (next_query);
          next_server = g_strdup (FALLBACK_CCTLD_WHOIS_SERVER);
          next_query = smart_query_for_server (next_server, query);
        }
      else
        {
          g_free (next_server);
          g_free (next_query);
          next_server = NULL;
        }

      g_match_info_free (match_info);
      g_free (response);
    }
}

int
main (int    argc,
      char **argv)
{
  setlocale (LC_ALL, "");
  g_type_init ();

  GOptionContext *context = g_option_context_new ("QUERY");
  GError *error = NULL;

  g_option_context_add_main_entries (context, entries, NULL);

  if (!g_option_context_parse (context, &argc, &argv, &error))
    {
      g_printerr ("g_option_context_parse: %s\n", error->message);
      /* g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL)); */
      exit (1);
    }

  if (argc != 2)
    {
      g_printerr ("whoiz: error: nothing to look up, whois query not specified\n\n");
      g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL));
      exit (2);
    }

  if (user_specified_server == NULL && user_specified_port != -1)
    g_printerr ("whoiz: warning: you specified a port (%d) on the command line,"
                " but no server; the port setting will be ignored\n", user_specified_port);

  if (user_specified_server != NULL)
    {
      int port = user_specified_port > 0 ? user_specified_port : 43;
      char *response = simple_lookup (user_specified_server, port, argv[1]);
      puts (response);
    }
  else
    smart_lookup (argv[1]);

  exit (0);
}
