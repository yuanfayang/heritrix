package org.archive.settings;

public class JobHome {
    // TODO: determine if org.archive.state.Path is usable here or instead of JobHome
    String path;
    String name;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
}
