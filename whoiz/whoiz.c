/*
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

static char *server = "whois.arin.net";
static int port = 43;

static GOptionEntry entries[] =
{
  { "host", 'h', 0, G_OPTION_ARG_STRING, &server, "Connect to server HOST", "HOST" },
  { "port", 'p', 0, G_OPTION_ARG_STRING, &server, "Connect to port PORT", "PORT" },
  { NULL }
};

static GSocket *
open_socket (char *server_colon_port,
             int   default_port)
{
  GSocketClient *client = g_socket_client_new ();

  GError *error = NULL;

  GSocketConnection *connection = g_socket_client_connect_to_host (client, server_colon_port, default_port, NULL, &error);
  if (connection == NULL) 
    {
      g_printerr ("g_socket_client_connect_to_host: %s\n", error->message);
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

  gssize bytes_sent = g_socket_send (socket, query, strlen (query), NULL, &error);
  if (bytes_sent == -1) 
    {
        g_printerr ("g_socket_send: %s\n", error->message);
        exit (4);
    }
}

static void
do_whois_lookup (char *server,
                 int   port,
                 char *query)
{
  GSocket *socket = open_socket (server, port);
  send_query (socket, query);

  GError *error = NULL;
  char buf[4096];
  gssize bytes_received;

  do 
    {
      bytes_received = g_socket_receive (socket, buf, sizeof (buf), NULL, &error);
      if (bytes_received < 0)
        {
          g_printerr ("g_socket_receive: %s\n", error->message);
          exit (5);
        }

      if (bytes_received > 0)
        fputs (buf, stderr);
    }
  while (bytes_received > 0);
}

int
main (int    argc,
      char **argv)
{
  g_type_init ();

  GOptionContext *context = g_option_context_new ("QUERY");
  GError *error = NULL;

  g_option_context_add_main_entries (context, entries, NULL);

  if (!g_option_context_parse (context, &argc, &argv, &error))
    {
      g_printerr ("g_option_context_parse: %s\n", error->message);
      // g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL));
      exit (1);
    }

  if (argc != 2)
    {
      g_printerr ("Query not specified\n\n");
      g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL));
      exit (2);
    }

  GString *query = g_string_new ("");
  g_string_printf (query, "%s\n", argv[1]);
  do_whois_lookup (server, port, query->str);

  exit (0);
}




/*
 * gboolean            g_hostname_is_ip_address            (const gchar *hostname);
 */
