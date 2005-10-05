/* JeUtils.java
 *
 * $Id$
 *
 * Created Jun 24, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.util;

import com.sleepycat.je.BtreeStats;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;

/**
 * Bdb JE utils.
 * @author stack
 * @revision $Date$ $Revision$
 */
public class JeUtils {
    /**
     * Get count of db records.
     * Only reliable when db is quiescent. Should be inexpensive operation.
     * See tail of
     * http://www.sleepycat.com/blogs/bdb-je/archives/2004/12/17/12.46.07/
     * @return Count of all db records
     * @throws DatabaseException
     * @deprecated This seems to take a way long time -- if not forever
     * recovering databases.  If all dbs in an bdbje environment are small,
     * then it seems to work fine but if an environment holds large dbs, then
     * we seem to never come out of the getStats call.
     * @param db Db to get count for.
     */
    public static long getCount(final Database db) throws DatabaseException {
        BtreeStats bts = (BtreeStats)db.getStats(null);
        return bts.getLeafNodeCount();
    }
}