/* 
 * Copyright (C) 2007 Internet Archive.
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
 * Settings.java
 *
 * Created on Jun 8, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import static org.archive.settings.path.PathChanger.AUTO_TAG;
import static org.archive.settings.path.PathChanger.LIST_TAG;
import static org.archive.settings.path.PathChanger.MAP_TAG;
import static org.archive.settings.path.PathChanger.OBJECT_TAG;
import static org.archive.settings.path.PathChanger.PRIMARY_TAG;
import static org.archive.settings.path.PathChanger.REFERENCE_TAG;
import static org.archive.state.KeyTypes.ENUM_TAG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.jsp.JspWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.archive.settings.SheetManager;
import org.archive.settings.file.FileSheetManager;
import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathValidator;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.KeyTypes;


/**
 * @author pjack
 *
 */
public class Settings {

    public enum Editability {
        /** The setting can be edited. */
        EDITABLE,
        
        /** The setting was set by the sheet manager and shouldn't change. */
        BOOTSTRAP,
        
        /** The setting marked as immutable, and the crawl is running. */
        IMMUTABLE,
        
        /** The setting is marked as global, and it's not the global sheet. */
        GLOBAL,
        
        /** 
         * The setting can't be edited because it hasn't been overridden in 
         * the sheet yet.
         */
        NOT_OVERRIDDEN,
        
        /**
         * The job is completed, and the settings are for historical reference.
         */
        COMPLETED,
        
        /**
         * The path for the setting isn't valid; perhaps a typo from a 
         * hand-modified sheet file.
         */
        PATH_INVALID
    }
    
    
    final static private Map<String,Set<String>> subclasses = 
        new HashMap<String,Set<String>>();

    

    final static String TYPE_PREFIX = "type-";
    
    final static String VALUE_PREFIX = "value-";

    
    /** 
     * The name of the sheet being edited.
     */
    final private String sheet;

    
    final private boolean online;
    
    final private boolean completed;
    
    
    /**
     * The settings for that sheet.
     */
    final private Map<String,Setting> settings;
    
    
    /**
     * Constructor.
     * 
     * @param sheet
     * @param settings
     */
    public Settings(String sheet, boolean online, boolean completed,
            Map<String,Setting> settings) {
        this.settings = settings;
        this.sheet = sheet;
        this.completed = completed;
        this.online = online;
    }
    

    /**
     * Returns a collection view of the settings in the order they were 
     * specified in the sheet.
     * 
     * @return  the collection of settings
     */
    public Collection<Setting> getSettings() {
        return settings.values();
    }
    
    
    public Setting getSetting(String path) {
        return settings.get(path);
    }
    
    
    /**
     * If the given path resolves to a list or a map, return a setting that
     * could be an element of that list or map.
     * 
     * @param path
     * @return
     */
    public Setting getElementPrototype(String path) {
        Setting setting = getSetting(path);
        if (!setting.getType().equals(PathChanger.MAP_TAG) 
                && !setting.getType().equals(PathChanger.LIST_TAG)) {
            return setting;
        }
        
        String parentPath = setting.getPath();
        Class<?> elementType = forName(setting.getValue());

        setting = new Setting();
        setting.setPath(parentPath);
        setting.setValue("");
        setting.setActualType(elementType);
        setting.setSheets(new String[] { sheet } );
        if (KeyTypes.isSimple(elementType)) {
            setting.setType(KeyTypes.getSimpleTypeTag(elementType));
        } else {
            Set<String> options = getSubclasses(elementType);
            setting.setType(PathChanger.OBJECT_TAG);
            if (options.isEmpty()) {
                setting.setValue("");
            } else {
                setting.setValue(options.iterator().next());
            }
        }
        
        return setting;
    }


    public void printFormField(JspWriter out, Setting setting) 
    throws IOException {
        printHiddenTypeField(out, setting);
        String type = setting.getType();
        
        try {
            if (PathChanger.isObjectTag(type)) {
                printObjectSelect(out, setting);
            } else if (type.equals(ENUM_TAG)) {
                printEnumSelect(out, setting);   
            } else if (type.equals(KeyTypes.getSimpleTypeTag(Boolean.class))) {
                printBooleanSelect(out, setting);
            } else if (type.equals(MAP_TAG) || type.equals(LIST_TAG)) { 
                printContainerLabel(out, setting);
            } else {
                printTextField(out, setting);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            out.println("ERROR: " + e);
        }
    }
    
    
    private void printContainerLabel(JspWriter out, Setting setting) 
    throws IOException {
        out.print(setting.getType());
        out.print(" of ");
        out.print(Text.baseName(setting.getValue()));
    }
    
    
    private void printHiddenTypeField(JspWriter out, Setting setting) 
    throws IOException {
        out.print("<input class=\"textbox\" type=\"hidden\" name=\"");
        out.print(TYPE_PREFIX);
        out.print(Text.attr(setting.getPath()));
        out.print("\" value=\"");
        out.print(Text.attr(setting.getType()));
        out.println("\">");        
    }
    
    
    private void printTextField(JspWriter out, Setting setting) 
    throws IOException {
        printTextField(out, setting, VALUE_PREFIX + setting.getPath(), 
                setting.getValue());
    }

    
    private void printTextField(
            JspWriter out, 
            Setting setting,
            String name,
            String value)
    throws IOException {
        out.print("<input class=\"textbox\" type=\"text\" name=\"");
        if (isEnabled(setting)) {
            out.print(Text.attr(name));
        } else {
            out.print("\" readonly=\"readonly");
        }
        out.print("\" value=\"");
        out.print(Text.attr(value));
        out.println("\">");
    }

    
    private void printSelectOpenTag(JspWriter out, Setting setting) 
    throws IOException {
        printSelectOpenTag(out, VALUE_PREFIX + setting.getPath(), setting);
    }
    
    
    private void printSelectOpenTag(JspWriter out, String name, Setting setting) 
    throws IOException {
        out.print("<select name=\"");
        out.print(Text.attr(name));
        if (!isEnabled(setting)) {
            out.print("\" disabled=\"disabled");
        }
        out.println("\">");        
    }
    

    private void printBooleanSelect(JspWriter out, Setting setting) 
    throws IOException {
        printSelectOpenTag(out, setting);
        boolean value = Boolean.valueOf(setting.getValue());
        out.print("<option value=\"true");
        if (value) {
            out.print("\" selected=\"selected");
        }
        out.println("\">true</option>");
 
        out.print("<option value=\"false");
        if (!value) {
            out.print("\" selected=\"selected");
        }
        out.println("\">false</option>");
 
        out.println("</select>");
    }
    
    private void printEnumSelect(JspWriter out, Setting setting) 
    throws IOException {
        String value = setting.getValue();
        int p = value.lastIndexOf('-');
        String enumClassName = value.substring(0, p);
        String enumValue = value.substring(p + 1);
        Class enumClass = forName(enumClassName);

        printSelectOpenTag(out, setting);
        
        enumClass.getEnumConstants();
        for (Object o: enumClass.getEnumConstants()) {
            String v = o.toString();
            out.print("<option value=\"");
            out.print(enumClassName);
            out.print('-');
            out.print(v);
            if (v.equals(enumValue)) {
                out.print("\" selected=\"selected");
            }
            out.print("\">");
            out.print(v);
            out.println("</option>");
        }
        
        out.println("</select>");
    }


    private void printObjectSelect(JspWriter out, Setting setting) 
    throws IOException {
        printSelectOpenTag(out, setting);

        printReuseOptions(out, setting);
        printCreateOptions(out, setting);
        
        out.println("</select>");
    }

    
    public void printReuseOptions(JspWriter out, Setting setting) 
    throws IOException {
        Collection<Setting> reuse = getReuseOptions(setting);
        boolean foundSelected = false;
        for (Setting s: reuse) {
            out.print("<option value=\"");
            out.print(REFERENCE_TAG);
            out.print(", ");
            out.print(Text.attr(s.getPath()));
            if (setting.getValue().equals(s.getPath())) {
                foundSelected = true;
                out.print("\" selected=\"selected");
            }
            out.print("\">Reuse ");
            out.print(Text.html(s.getPath()));
            out.println("</option>");
        }
        if (!foundSelected && setting.getType().equals(REFERENCE_TAG)) {
            out.print("<option value=\"");
            out.print(REFERENCE_TAG);
            out.print(", ");
            out.print(Text.attr(setting.getValue()));
            out.print("\" selected=\"selected");
            out.print("\">Reuse ");
            out.print(Text.html(setting.getValue()));
            out.println("</option>");            
        }
    }
    
    
    public Set<Setting> getReuseOptions(Setting setting) {
        Set<Setting> result = new HashSet<Setting>();
        Class<?> actualType = getActualType(setting);
        for (Setting s: settings.values()) {
            if (s == setting) {
                // Don't include objects that haven't been defined yet.
                return result;
            }
            if (PathChanger.isCreateTag(s.getType()) 
                    && !s.getValue().equals("null")) {
                Class sclass = forName(s.getValue());
                if (actualType.isAssignableFrom(sclass)) {
                    result.add(s);
                }
            }
        }
        return result;
    }
    
    
    public void printCreateOptions(JspWriter out, Setting setting) 
    throws IOException {
        Collection<String> options = getCreateOptions(setting);
        for (String option: options) {
            out.print("<option value=\"");
            if (setting.getType().equals(PRIMARY_TAG)) {
                out.print(PRIMARY_TAG);
            } else {
                // Setting.getType might be map or list, and we are trying
                // to create an element of that map or list.  Force this to
                // be object.
                out.print(OBJECT_TAG);
            }
            out.print(", ");
            out.print(Text.attr(option));
            out.print("\"");
            if (option.equals(setting.getValue())) {
                out.print(" selected=\"selected\"");
            }
            out.print(">Create ");
            out.print(Text.html(Text.baseName(option)));
            out.println("</option>");
        }
    }
    
    
    public Collection<String> getCreateOptions(Setting setting) {
        Class<?> actual = getActualType(setting);
        Set<String> options = getSubclasses(actual);
        if (PathChanger.isCreateTag(setting.getType()) && 
                !options.contains(setting.getValue())) {
            Collection<String> old = options;
            options = new TreeSet<String>(new BaseNameComp());
            options.addAll(old);
            options.add(setting.getValue());
        }
        return options;
    }
    
    
    private static boolean validParentType(String type) {
        return PathChanger.isCreateTag(type)
         || type.equals(MAP_TAG)
         || type.equals(LIST_TAG);
    }

    
    private Setting getParentSetting(Setting setting) {
        String path = setting.getPath();

        String parentPath = Text.parentPath(setting.getPath());
        Setting parent = settings.get(parentPath);
        if (parent == null) {
            parent = settings.get("manager");
        }
        if (parent == null) {
            throw new IllegalStateException("No parent for " + path);
        }
        
        return parent;        
    }


    public Class getActualType(Setting setting) {
        Class result = setting.getActualType();
        if (result != null) {
            return result;
        }
        
        String path = setting.getPath();
        if (path.indexOf(PathValidator.DELIMITER) < 0) {
            Key<?> key = KeyManager.getKeys(FileSheetManager.class).get(path);
            if (key.getType() == Map.class || key.getType() == List.class) {
                result = key.getElementType();
            } else {
                result = key.getType();
            }
            setting.setActualType(result);
            return result;
        }
        
        Setting parent = getParentSetting(setting);
        // type of parent must be object, list or map
        String parentType = parent.getType();
        if (!validParentType(parentType)) {
            throw new IllegalArgumentException(parent.getPath() + 
                    " can't have children (it's a " + parentType + ".)");
        }
        
        // All children of a list or map have the same type (the element
        // type that was specified.
        if (parentType.equals(MAP_TAG) || parentType.equals(LIST_TAG)) {
            result = forName(parent.getValue());
        } else if (PathChanger.isCreateTag(parentType)){
            // A child of an object will have a type dicated by one of that
            // object's keys.
            
            Class parentClass = forName(parent.getValue());
            String keyName = Text.lastPath(path);
            Key<Object> key = KeyManager.getKeys(parentClass).get(keyName);
            if (key == null) {
                throw new IllegalStateException(path 
                        + " uses nonexistent key " + keyName);
            }
            result = key.getType();
        } else {
            throw new IllegalStateException(path + " had weird parent " + parentType);
        }
        
        setting.setActualType(result);
        return result;
    }


    private static Class forName(String s) {
        try {
            return Class.forName(s);
        } catch (Exception e) {
            throw new IllegalStateException("No such class: " + s);
        }
    }

    
    public void printElementFormField(JspWriter out, Setting container)
    throws IOException {
        String parentPath = container.getPath();
        Class elementType = forName(container.getValue());

        Setting setting = new Setting();
        setting.setPath(parentPath);
        setting.setValue("");
        setting.setSheets(new String[] { sheet } );
        if (KeyTypes.isSimple(elementType)) {
            setting.setType(KeyTypes.getSimpleTypeTag(elementType));
        } else {
            setting.setType(PathChanger.OBJECT_TAG);
        }
        
        printDetailFormField(out, setting);        
    }
    

    public void printDetailFormField(JspWriter out, Setting setting) 
    throws IOException {
        printHiddenTypeField(out, setting);
        String type = setting.getType();
        
        try {
            if (PathChanger.isObjectTag(type)) {
                printDetailObjectFields(out, setting);
            } else if (type.equals(ENUM_TAG)) {                
                printEnumSelect(out, setting);   
            } else if (type.equals(KeyTypes.getSimpleTypeTag(Boolean.class))) {
                printBooleanSelect(out, setting);
            } else {
                printTextField(out, setting);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            out.println("ERROR: " + e);
        }
    }
    
    
    private void printDetailObjectFields(JspWriter out, Setting setting) 
    throws IOException {
        Collection<Setting> reuse = getReuseOptions(setting);
        Collection<String> create = getCreateOptions(setting);

        out.println("<table>");
        
        boolean selected = setting.getType().equals(AUTO_TAG);
        radio(out, "ref_auto", "Automatically reuse the primary object.", 
                selected);
        out.println("</td>\n</tr>");
        
        selected = setting.getType().equals(REFERENCE_TAG) 
         && reuse.contains(setting.getValue());
        radio(out, "ref_select", "Reuse an existing object:", selected);
        this.printSelectOpenTag(out, "ref_select", setting);
        this.printReuseOptions(out, setting);
        out.println("</select>\n</td>\n</tr>");
        
        selected = setting.getType().equals(REFERENCE_TAG) 
        && !reuse.contains(setting.getValue());
        radio(out, "ref_manual", "Manually enter a path to reuse:", selected);
        String value = setting.getType().equals(REFERENCE_TAG) ? setting.getValue() : "";
        printTextField(out, setting, "ref_manual", value);
        out.println("</td>\n</tr>");
        
        selected = setting.getType().equals(OBJECT_TAG)
         && create.contains(setting.getValue());
        radio(out, "obj_select", "Create a new object:", selected);        
        this.printSelectOpenTag(out, "obj_select", setting);
        this.printCreateOptions(out, setting);
        out.println("</select>\n</td>\n</tr>");
        
        
        selected = setting.getType().equals(OBJECT_TAG) 
         && !reuse.contains(setting.getValue());
        radio(out, "obj_manual", "Manually enter a Java class name to create:", selected);
        value = setting.getType().equals(OBJECT_TAG) ? setting.getValue() : "";
        printTextField(out, setting, "obj_manual", value);
        out.println("</td>\n</tr>");
        
        out.println("<table>");
    }

    
    private void radio(JspWriter out, String choice, String label, 
            boolean selected) 
    throws IOException {
        out.println("<tr>");
        out.println("<td>");
        out.print("<input type=\"radio\" id=\"value_kind_");
        out.print(choice);
        out.print("\" name=\"value_kind\" value=\"");
        out.print(Text.attr(choice));
        out.print('\"');
        if (selected) {
            out.print(" checked");
        }
        out.println(">");
        out.println("</td>");

        out.println("<td>");
        out.print("<label for=\"value_kind_");
        out.print(choice);
        out.println("\">");
        out.println(label);
        out.println("</label>");
        out.println("</td>");
        out.println("<td>");
    }

    
    public String getDescription(Setting setting) {
        Key key = getKey(setting);
        if (key == null) {
            return "No description."; 
        } else {
            return key.getDescription(Locale.ENGLISH);
        }
    }

    
    public String getModuleDescription(Setting setting) {
        Key<?> key = getKey(setting);
        if (key == null) {
            return null;
        }
        Class<?> owner = key.getOwner();
        return KeyManager.getModuleDescription(owner, Locale.ENGLISH);
    }
    
    public String getOwnerBaseName(Setting setting) {
        Key<?> key = getKey(setting);
        if (key == null) {
            return null;
        }
        Class<?> owner = key.getOwner();
        return Text.baseName(owner.getName());
    }
    
    public Key getKey(Setting setting) {
        if (setting.isKeySet()) {
            return setting.getKey();
        }

        Setting parent = getParentSetting(setting);
        String parentType = parent.getType();
        if (PathChanger.isCreateTag(parentType)) {
            String path = setting.getPath();
            Class parentClass = forName(parent.getValue());
            Map<String,Key<Object>> keys = KeyManager.getKeys(parentClass);                
            String keyName = Text.lastPath(path);
            Key<Object> key = keys.get(keyName);
            setting.setKey(key);
            return key;
        } else {
            setting.setNoKey();
            return null;
        }
    }

    
    public boolean isEnabled(Setting setting) {
        return getEditability(setting) == Editability.EDITABLE;
    }

    
    public boolean canOverride(Setting setting) {
        Editability e = getEditability(setting);
        return e == (Editability.EDITABLE) || (e == Editability.NOT_OVERRIDDEN);
    }
    
    public Editability getEditability(Setting setting) {
        if (completed) {
            return Editability.COMPLETED;
        }
        if (setting.isPathInvalid()) {
            return Editability.PATH_INVALID;
        }
        if (!setting.getPath().startsWith(StringUtils.defaultString(
                SheetManager.ROOT.getFieldName()))) {
            return Editability.BOOTSTRAP;
        }
        Key key = getKey(setting);
        if (key == null) {
            if (setting.getSheets()[0].equals(sheet)) {
                return Editability.EDITABLE;
            } else {
                return Editability.NOT_OVERRIDDEN;
            }
        }

        if (online && key.isImmutable()) {
            return Editability.IMMUTABLE;
        }

        if (key.isGlobal() && !sheet.equals(SheetManager.GLOBAL_SHEET_NAME)) {
            return Editability.GLOBAL;
        }
        
        if (!setting.getSheets()[0].equals(sheet)) {
            return Editability.NOT_OVERRIDDEN;
        }
        
        
        return Editability.EDITABLE;
    }
    
    
    public boolean isObjectElementType(Setting setting) {
        Class<?> c = this.getActualType(setting);
        Key<?> key = getKey(setting);
        if (key == null) {
            return true;
        }
        return !KeyTypes.isSimple(key.getElementType());
    }


    public static Set<String> getSubclasses(Class clz) {
        synchronized (subclasses) {
            Set<String> result = subclasses.get(clz.getName());
            if (result != null) {
                return result;
            }
            try {
                String path = clz.getName().replace('.', '/') + ".subclasses";
                Enumeration<URL> resources = ClassLoader.getSystemResources(path);
                
                if (!resources.hasMoreElements()) {
                    result = Collections.emptySet();
                    subclasses.put(clz.getName(), result);
                    return result;
                }
                
                result = new TreeSet<String>(new BaseNameComp());
                while(resources.hasMoreElements()) {
                    URL rez = resources.nextElement();
                    BufferedReader br = null; 
                    try {
                        br = new BufferedReader(
                                new InputStreamReader(rez.openStream()));
                        for (String s = br.readLine(); s != null; s = br.readLine()) {
                            s = s.trim();
                            if (s.length() > 0 && !s.startsWith("#")) {
                                result.add(s);
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(br);
                    }
                }
                subclasses.put(clz.getName(), result);
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                result = Collections.emptySet();
                subclasses.put(clz.getName(), result);
                return result;                
            } 
        }
    }
    
    static class BaseNameComp implements Comparator<String> {

        public int compare(String one, String two) {
            String base1 = Text.baseName(one); 
            String base2 = Text.baseName(two);
            return base1.compareTo(base2);
        }

    }

}
