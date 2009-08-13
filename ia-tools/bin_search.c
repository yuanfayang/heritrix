/* vim: set sw=2 et:
 *
 * $Id: whoiz.c 6450 2009-08-13 21:45:31Z nlevitt $
 *
 * bin_search.c: Perform binary search of sorted text file. Replacement for
 * alexa tools bin_search.
 *
 * Copyright (C) 2009 Internet Archive
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

/* 
 * Usage: bin_search [-all -any -d -f -fr -q -r] [string] [input file] <input file> ...
 * 
 * Options:
 * -all       Get ALL (not the default of get the first) occurrence of
 *            line(s) with the string.
 * -any       Get ANY (not the default of get the first) occurrence of a
 *            line with the string.
 * -d X       Use char X as delimeter (used w/ "-f" option).  Default is <TAB>.
 * -e         Only return exact matches (default is to match a prefix)
 * -f X       Use sorted column X for comparison.  Default is 1.
 * -q         Quiet(er).
 * -r (-fr)   The file is in "sort -r" order.  (Obsoleted variant "-fr" will
 *            work for backwards compatibility.)
 * 
 * Input file MUST have specified column in normal "sort" order, or
 * may be in "sort -r" order when using "-r" option.
 * 
 * Program will binary search the file, looking for a(ny) line that
 * begins with the string.
 * 
 * String chars can be "\t", which is the TAB character.
 */

 
#include <glib.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <locale.h>

static gboolean option_all = FALSE;
static gboolean option_any = FALSE;
static char *option_delim = "\t";
static gboolean option_exact = FALSE;
static int option_field = 1;

static GOptionEntry entries[] =
{
  { "all", 'a', 0, G_OPTION_ARG_NONE, &option_all, "Get ALL (not the default of get the first) occurrence of line(s) with the string.", NULL },
  { "any", 0, 0, G_OPTION_ARG_INT, &option_any, "Get ANY (not the default of get the first) occurrence of a line with the string.", NULL },
  { "delim", 'd', 0, G_OPTION_ARG_STRING, &option_delim, "Use char X as delimiter (used w/ -f option). Default is <TAB>.", "X" },
  { "exact", 'e', 0, G_OPTION_ARG_NONE, &option_exact, "Only return exact matches (default is to match a prefix).", NULL },
  { "field", 'f', 0, G_OPTION_ARG_INT, &option_field, "Use sorted column X for comparison. Default is 1.", "X" },
  { "quiet", 'q', 0, G_OPTION_ARG_NONE, &option_quiet, "Quiet(er).", NULL },
  { "reverse", 'r', 0, G_OPTION_ARG_NONE, &option_field, "The file is in \"sort -r\" order.", NULL },
  { NULL }
};

static void
parse_command_line (int    argc,
                    char **argv)
{
  GOptionContext *context = g_option_context_new ("STRING FILE...");
  GError *error = NULL;
  int i;

  *server = NULL;
  *query = NULL;

  g_option_context_set_summary (context, "Perform binary search of sorted text file.");
  g_option_context_add_main_entries (context, entries, NULL);

  if (!g_option_context_parse (context, &argc, &argv, &error))
    {
      g_printerr ("g_option_context_parse: %s\n", error->message);
      exit (1);
    }

  g_option_context_free (context);
}

int
main (int    argc,
      char **argv)
{
  setlocale (LC_ALL, "");
  g_type_init ();

  parse_command_line (argc, argv);

  exit (0);
}
