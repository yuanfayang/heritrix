package org.archive.settings.path;

import java.lang.reflect.Constructor;

import org.archive.settings.Sheet;

class Construction {


    private static Object getReference(Sheet sheet, String value) {
        if (value.equals("sheetManager")) {
            return sheet.getSheetManager();
        } else {
            return PathValidator.validate(sheet, value);
        }
    }


    public static Object construct(Sheet sheet, String value) {
        if (!value.startsWith("new ")) {
            return getReference(sheet, value);
        }
        
        value = value.substring(4);
        int p = value.indexOf("(");
        if (p < 0) {
            return constructDefault(sheet, value);
        }
        
        String className = value.substring(0, p);
        if (!value.endsWith(")")) {
            throw new PathChangeException("Unterminated constructor.");
        }
        value = value.substring(p +  1, value.length() - 1).trim();
        if (value.length() == 0) {
            return constructDefault(sheet, className);
        }
        
        Class cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new PathChangeException(e);
        }
        
        
        String[] paths = value.split(",");
        Object[] params = new Object[paths.length];
        for (int i = 0; i < paths.length; i++) {
            params[i] = construct(sheet, paths[i].trim());
        }
        
        Constructor c = findConstructor(cls, params);
        if (c == null) {
            throw new PathChangeException("No such constructor.");
        }
        try {
            return c.newInstance(params);
        } catch (Exception e) {
            throw new PathChangeException(e);
        }
    }
    
    
    private static Object constructDefault(Sheet sheet, String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new PathChangeException(e);
        }
    }
    
    
    private static Constructor findConstructor(Class cls, Object[] params) {
        Constructor[] all = cls.getConstructors();
        for (Constructor c: all) {
            if (applies(c.getParameterTypes(), params)) {
                return c;
            }
        }
        return null;
    }
    
    
    private static boolean applies(Class[] formal, Object[] actual) {
        if (formal.length != actual.length) {
            return false;
        }
        for (int i = 0; i < formal.length; i++) {
            if (!formal[i].isInstance(actual[i])) {
                return false;
            }
        }
        return true;
    }
}
