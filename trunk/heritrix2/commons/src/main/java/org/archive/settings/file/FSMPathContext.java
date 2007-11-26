/**
 * 
 */
package org.archive.settings.file;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.archive.settings.CheckpointRecovery;
import org.archive.state.PathContext;


/**
 * Path context for {@link Path} settings defined by a {@link FileSheetManager}.
 * 
 * @author pjack
 */
class FSMPathContext implements PathContext, Serializable {

    /**
     * The name of the path variable containing the current job name.
     */
    final public static String CURRENT_JOB = "current.job";

    /**
     * The name of the path variable containing the original job name.
     */
    final public static String ORIGINAL_JOB = "original.job";
    
    /**
     * The name of the path variable containing the current job name's prefix.
     */
    final public static String JOB_PREFIX = "job.prefix";
    
    private static final long serialVersionUID = 1L;

    /**
     * Base directory for relative {@link Path} values.
     */
    private File baseDir;
    
    /**
     * Maps of path variable name to path variable value.
     */
    private Map<String,String> vars;
    
    /**
     * Constructor.
     * 
     * @param baseDir  Base directory for relative {@link Path} values.
     * @param jobName  The name of the job.
     */
    public FSMPathContext(File baseDir, String jobName) {
        this.baseDir = baseDir;
        this.vars = new HashMap<String,String>();
        vars.put(CURRENT_JOB, jobName);
        vars.put(ORIGINAL_JOB, jobName);
        vars.put(JOB_PREFIX, getPrefix(jobName));
    }

    /**
     * Returns the path variables defined by this path context.  Those 
     * variables are:
     * 
     * <dl>
     * <dt>current.job</dt>
     * <dd>The current job name, eg "basic_seed_sites-20071011120033".  The value 
     * will not include the "stage prefix" used by {@link CrawlJobManagerImpl}.
     * </dd>
     * <dt>original.job</dt>
     * <dd>The original job name, eg "basic_seed_sites-20071010120033".  This will
     * usually be the same as <code>current.job</code>, unless the current 
     * job was a checkpoint recovery.  In that case, this variable will contain
     * the name of the recovered job.</dd>
     * <dt>job.prefix</dt>
     * <dd>The job name without any suffix, eg "basic_seed_sites".</dd>
     * </dl>
     * 
     * @return the path variables
     */
    public Map<String,String> getPathVariables() {
        return Collections.unmodifiableMap(vars);
    }

    // JavaDoc inherited
    public File getBaseDir() {
        return baseDir;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream inp)
    throws IOException, ClassNotFoundException {
        inp.defaultReadObject();
        if (inp instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)inp;
            this.baseDir = new File(cr.translatePath(
                    baseDir.getAbsolutePath()));
            Map<String,String> newVars = new HashMap<String,String>();
            newVars.put(CURRENT_JOB, cr.getRecoveredJobName());
            newVars.put(ORIGINAL_JOB, vars.get(ORIGINAL_JOB));
            newVars.put(JOB_PREFIX, getPrefix(cr.getRecoveredJobName()));
            this.vars = newVars;
        }
    }

    private static String getPrefix(String job) {
        int p = job.lastIndexOf('-');
        return p > 0 ? job.substring(0, p) : job;
    }
}