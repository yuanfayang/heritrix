package org.archive.crawler.admin;

/**
 * Wraps a long. Used in place of Long so that when we extract it from a 
 * Collection we can modify the long value without creating a new object. This way
 * we don't have to rewrite the Collection to update one of the stored longs.
 * 
 * @author Kristinn Sigurdsson
 */
public class LongWrapper{
    public long longValue;
    public LongWrapper(int initial){
        longValue = initial;
    }
}
