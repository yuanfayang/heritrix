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
#include <stdlib.h>

static char *host_option;

static GOptionEntry entries[] =
{
  { "host", 'h', 0, G_OPTION_ARG_STRING, &host_option, "Connect to server HOST", "HOST" },
  { NULL }
};

int
main (int    argc,
      char **argv)
{
  GOptionContext *context = g_option_context_new ("QUERY");
  GError *error = NULL;

  g_option_context_add_main_entries (context, entries, NULL);

  if (!g_option_context_parse (context, &argc, &argv, &error))
    {
      g_printerr ("%s\n\n", error->message);
      // g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL));
      exit (1);
    }

  if (argc != 2)
    {
      g_printerr ("Query not specified\n\n");
      g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL));
    }

  exit (0);
}
