/* Copyright (C) 2006 Internet Archive.
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
 *
 * StateProcessor.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


/**
 * A processor of some state.  The {@link #process(StateProvider)} method
 * performs some process.  The parameters for the process should be drawn
 * from the given {@link StateProvider}.
 * 
 * <p>As a general rule, implementations should not define any instance 
 * fields.  Instead, the fields necessary to perform the process should be
 * declared as {@link Key} static fields.  This allows consumers to 
 * provide custom StateProvider instances that may (for example) store the
 * state in some external database, or share common state over many threads.
 * 
 * <p>Put another way, implementations should be safe for use with multiple
 * threads without relying on synchronization.  
 * 
 * @author pjack
 *
 * @param <S>  the state provider type
 * @param <R>  the return type of the process
 * @param <E>  the exception that the process can raise
 */
public interface StateProcessor<S extends StateProvider,R,E extends Throwable> {


    /**
     * Performs a process.
     * 
     * @param stateProvider  provides state for the process
     * @return  the result of the process
     * @throws E   if the process raises an exception
     */
    R process(S stateProvider) throws E;
    
}
