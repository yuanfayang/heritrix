/* DevNull
 *
 * $Id$
 *
 * Created on Jun 23, 2003
 *
 * Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.io;

import java.io.OutputStream;


/**
 * This class takes input and does nothing with it, sort of a /dev/null for the
 * rest of us.
 *
 * @author Parker Thompson
 */
public class DevNull extends OutputStream {

    public DevNull(){
        super();
    }

    public void write(int b) {
        // Do nothing.
    }

    public void write(byte[] b) {
        // Do nothing.
    }

    public void write(byte[] b, int off, int len) {
        // Do nothing.
    }
}
