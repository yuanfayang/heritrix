package org.archive.mojo;

import java.io.File;


/**
 * Configuration object for {@link Generator}.
 * 
 * @author pjack
 */
public class Config {

    private File inputDir;
    private File outputDir;


    public Config() {
    }


    public File getInputDir() {
        return inputDir;
    }


    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }


    public File getOutputDir() {
        return outputDir;
    }


    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    
    
    
}
