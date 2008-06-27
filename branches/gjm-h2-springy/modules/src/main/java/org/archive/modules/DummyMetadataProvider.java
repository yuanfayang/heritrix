/**
 * 
 */
package org.archive.modules;

import java.io.Serializable;

import org.archive.modules.writer.MetadataProvider;

public class DummyMetadataProvider implements MetadataProvider, Serializable {
    private static final long serialVersionUID = 1L;

    public String getAudience() {
        return null;
    }

    public String getFrom() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public String getJobName() {
        return null;
    }

    public String getOperator() {
        return null;
    }
    
    public String getOperatorFrom() {
        return null;
    }

    public String getOrganization() {
        return null;
    }

    public String getRobotsPolicyName() {
        return null;
    }

    public String getUserAgent() {
        return null;
    }
}