package org.archive.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

public class StringLinesIterator implements Iterator<String>, Iterable<String> {
    protected BufferedReader r;
    protected String nextLine;
    
    public StringLinesIterator(String s) {
        r = new BufferedReader(new StringReader(s));
        readNextLine();
    }

    protected void readNextLine() {
        try {
            nextLine = r.readLine();
        } catch (IOException e) {
            // should never happen (right?)
            nextLine = null;
        }
    }

    public boolean hasNext() {
        return nextLine != null;
    }

    public String next() {
        try {
            return nextLine;
        } finally {
            if (nextLine != null)
                readNextLine();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Iterator<String> iterator() {
        return this;
    }
}
