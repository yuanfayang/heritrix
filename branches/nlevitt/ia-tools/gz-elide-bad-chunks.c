/* $Id: bin_search.c 6459 2009-08-14 23:17:08Z nlevitt $
 *
 * vim: set sw=2 et:
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
 
#include <stdio.h>
#include <locale.h>
#include <zlib.h>

int
main (int    argc,
      char **argv)
{
  setlocale (LC_ALL, "");

  /*
  z_stream z;
  z_stream z;
  char input_buffer[1024];
  char output_buffer[1024];
  FILE *fin = stdin;
  FILE *fout = stdout;
  int status;

  z.avail_in = 0;
  z.next_out = output_buffer;
  z.avail_out = 1024;
  for (;;) 
    {
      if ( z.avail_in == 0 ) 
        {
          z.next_in = input_buffer;
          z.avail_in = fread (input_buffer, 1, 1024, fin);
        }
      if (z.avail_in == 0)
        break;
      status = deflate(&z, Z_NO_FLUSH);
      int count = 1024 - z.avail_out;
      if (count)
        fwrite (output_buffer, 1, count, fout);
      z.next_out = output_buffer;
      z.avail_out = 1024;
    }
  */

  char buf[4096];
  gzFile gzin = gzdopen (STDIN_FILENO, "rb");
  /* XXX if (gzin == NULL) ... */

  int bytes_read;
  while ((bytes_read = gzread (gzin, buf, sizeof (buf))) > 0)
    {
      size_t bytes_written = fwrite (buf, 1, bytes_read, stdout);
      /* XXX if (bytes_written ...) */
    }
  /* XXX if (bytes_read ...) */

  return 0;
}
