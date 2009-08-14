/* $Id$
 *
 * vim: set sw=2 et:
 *
 * bin_search.c: Perform binary search of sorted text file(s). Replacement for
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
 
#include <glib.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <locale.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

static struct
{
  gboolean  all;
  gboolean  any;
  char     *delim;
  gboolean  exact;
  int       field;
  gboolean  quiet;
  gboolean  reverse;
} 
options = { FALSE, FALSE, "\t", FALSE, 1, FALSE, FALSE };

static GOptionEntry entries[] =
{
  { "all", 0, 0, G_OPTION_ARG_NONE, &options.all, "Get ALL (not the default of get the first) occurrence of line(s) with the string.", NULL },
  { "any", 0, 0, G_OPTION_ARG_NONE, &options.any, "Get ANY (not the default of get the first) occurrence of a line with the string.", NULL },
  { "delim", 'd', 0, G_OPTION_ARG_STRING, &options.delim, "Use char X as delimiter (used w/ -f options). Default is <TAB>.", "X" },
  { "exact", 'e', 0, G_OPTION_ARG_NONE, &options.exact, "Only return exact matches (default is to match a prefix).", NULL },
  { "field", 'f', 0, G_OPTION_ARG_INT, &options.field, "Use sorted column X for comparison. Default is 1.", "X" },
  { "quiet", 'q', 0, G_OPTION_ARG_NONE, &options.quiet, "Quiet(er).", NULL },
  { "reverse", 'r', 0, G_OPTION_ARG_NONE, &options.reverse, "The file is in \"sort -r\" order.", NULL },
  { NULL }
};

static void
parse_command_line (int    *argc,
                    char ***argv)
{
  GOptionContext *context = g_option_context_new ("STRING FILE...");
  GError *error = NULL;

  g_option_context_add_main_entries (context, entries, NULL);
  g_option_context_set_summary (context, "Perform binary search of sorted text file(s).");
  g_option_context_set_description (context, "Input file MUST have specified column in normal \"sort\" order, or may be in \"sort -r\" order when using \"-r\" option."
      "\n\nStrings are compared according to their native byte values; therefore the files need to have been sorted with LC_COLLATE=C (or LC_ALL=C)."
      "\n\nProgram will binary search the file, looking for a(ny) line that begins with the string.\n");

  if (!g_option_context_parse (context, argc, argv, &error))
    {
      g_printerr ("bin_search: g_option_context_parse: %s\n", error->message);
      exit (1);
    }

  if (*argc < 3)
    {
      g_printerr ("bin_search: error: You must specify a search string and at least one file to search\n\n");
      g_printerr ("%s", g_option_context_get_help (context, TRUE, NULL));
      exit (2);
    }

  g_option_context_free (context);
}

/* Return value must be unref'd with g_io_channel_unref(). Exits on failure. */
static GIOChannel *
open_file (const char *filename)
{
  /* open the file */
  GError *error = NULL;
  GIOChannel *io_channel = g_io_channel_new_file (filename, "r", &error);
  if (io_channel == NULL)
    {
      g_printerr ("bin_search: g_io_channel_new_file (\"%s\"): %s\n", filename, error->message);
      exit (3);
    }

  /* treat as binary */
  error = NULL;
  GIOStatus status = g_io_channel_set_encoding (io_channel, NULL, &error);
  if (status == G_IO_STATUS_ERROR)
    {
      g_printerr ("bin_search: g_io_channel_set_encoding: %s\n", error->message);
      exit (4);
    }
  g_assert (status == G_IO_STATUS_NORMAL);

  /* make sure it's seekable */
  if ((g_io_channel_get_flags (io_channel) & G_IO_FLAG_IS_SEEKABLE) != G_IO_FLAG_IS_SEEKABLE)
    {
      g_printerr ("bin_search: File %s is not seekable (perhaps it's a directory?)\n", filename);
      exit (9);
    }

  return io_channel;
}

static gint64
file_size (const char *filename)
{
  struct stat buf;
  errno = 0;

  if (g_stat (filename, &buf) != 0)
    {
      g_printerr ("bin_search: stat (\"%s\"): %s\n", filename, g_strerror (errno));
      exit (2);
    }

  return buf.st_size;
}

static void
seek (GIOChannel *io_channel,
      gint64      offset)
{
  GError *error = NULL;
  GIOStatus status = g_io_channel_seek_position (io_channel, offset, G_SEEK_SET, &error);
  if (status == G_IO_STATUS_ERROR)
    {
      g_printerr ("bin_search: g_io_channel_seek_position: %s\n", error->message);
      exit (5);
    }
  else if (status != G_IO_STATUS_NORMAL)
    {
      g_printerr ("bin_search: g_io_channel_seek_position: non-normal status %d\n", status);
      exit (8);
    }
}

/* returns the byte read or -1 if EOF, exits on failure */
static int
read_byte (GIOChannel *io_channel)
{
  /* expect binary mode */
  g_assert (g_io_channel_get_encoding (io_channel) == NULL);

  GError *error = NULL;
  char c = 0;
  gsize bytes_read = -1;

  GIOStatus status = g_io_channel_read_chars (io_channel, &c, 1, &bytes_read, &error);
  g_assert (status != G_IO_STATUS_AGAIN); /* can't happen right? */
  if (status == G_IO_STATUS_ERROR)
    {
      g_printerr ("bin_search: g_io_channel_read_chars: %s\n", error->message);
      exit (6);
    }
  else if (status == G_IO_STATUS_EOF)
    return -1;

  /* success. sanity check */
  g_assert (status == G_IO_STATUS_NORMAL);
  g_assert (bytes_read == 1);

  return (int) c;
}

/* fills in line_buf with the line that offset is in the middle of */
static void
get_line_at_offset (GIOChannel *io_channel,
                    gint64      offset,
                    GString    *line_buf)
{
  gint64 pos;
  int c = -1;

  /* find beginning of line */
  for (pos = offset - 1; pos > 0 && c != '\n'; pos--)
    {
      seek (io_channel, pos);
      c = read_byte (io_channel); /* advances file pos by 1 */
    }

  gsize terminator_pos = -1;
  GError *error = NULL;

  /* read in the line */
  GIOStatus status = g_io_channel_read_line_string (io_channel, line_buf, &terminator_pos, &error);
  g_assert (status != G_IO_STATUS_AGAIN);
  if (status == G_IO_STATUS_ERROR)
    {
      g_printerr ("bin_search: g_io_channel_read_line_string: %s\n", error->message);
      exit (7);
    }

  /* all done, line_buf has been filled in */
}

/* respects options (XXX not yet) */
static void
bin_search (const char *string,
            const char *filename)
{
  gint64 left = 0l;
  gint64 right = file_size (filename) - 1;

  GIOChannel *io_channel = open_file (filename);
  GString *current_line = g_string_new ("");
  int len = strlen (string);

  while (right - left >= len)
    {
      gint64 pos = (left + right + 1) / 2;

      get_line_at_offset (io_channel, pos, current_line);

      int cmp;
      if (options.exact)
        cmp = strcmp (string, current_line->str);
      else
        cmp = strncmp (string, current_line->str, len);

      if (cmp == 0)
        {
          fputs (current_line->str, stdout);
          break;
        }
      else if (cmp < 0)
        right = pos - 1;
      else 
        left = pos + 1;
    }


  /* finished, clean up */
  g_io_channel_unref (io_channel);
  g_string_free (current_line, TRUE);
}

int
main (int    argc,
      char **argv)
{
  setlocale (LC_ALL, "");
  g_type_init ();

  parse_command_line (&argc, &argv);

  int i;
  for (i = 2; i < argc; i++)
    bin_search (argv[1], argv[i]);

  exit (0);
}
