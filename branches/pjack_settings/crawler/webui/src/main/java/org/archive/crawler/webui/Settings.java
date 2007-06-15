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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspWriter;

import org.archive.settings.SheetManager;
import org.archive.settings.path.PathChanger;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.KeyTypes;

import static org.archive.settings.path.PathChanger.REFERENCE_TAG;
import static org.archive.settings.path.PathChanger.OBJECT_TAG;
import static org.archive.settings.path.PathChanger.MAP_TAG;
import static org.archive.settings.path.PathChanger.LIST_TAG;
import static org.archive.state.KeyTypes.ENUM_TAG;


/**
 * @author pjack
 *
 */
public class Settings {

    

    final static String TYPE_PREFIX = "type-";
    
    final static String VALUE_PREFIX = "value-";

    
    /** 
     * The name of the sheet being edited.
     */
    private String sheet;
    
    
    /**
     * The settings for that sheet.
     */
    private Map<String,Setting> settings;
    
    
    /**
     * Constructor.
     * 
     * @param sheet
     * @param settings
     */
    public Settings(String sheet, Map<String,Setting> settings) {
        this.settings = settings;
        this.sheet = sheet;
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


    public void printFormField(JspWriter out, Setting setting) 
    throws IOException {
        printHiddenTypeField(out, setting);
        String type = setting.getType();
        
        try {
            if (type.equals(OBJECT_TAG) || type.equals(REFERENCE_TAG)) {
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
        out.print(Text.attr(name));
        out.print("\" value=\"");
        out.print(Text.attr(value));
        if (!setting.getSheets()[0].equals(sheet)) {
            out.print("\" disabled=\"disabled");
        }
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
        if (!setting.getSheets()[0].equals(sheet)) {
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

    
    private void printReuseOptions(JspWriter out, Setting setting) 
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
    
    
    private Set<Setting> getReuseOptions(Setting setting) {
        Set<Setting> result = new HashSet<Setting>();
        Class<?> actualType = getActualType(setting);
        for (Setting s: settings.values()) {
            if (s == setting) {
                // Don't include objects that haven't been defined yet.
                return result;
            }
            if (s.getType().equals(OBJECT_TAG) && !s.getValue().equals("null")) {
                Class sclass = forName(s.getValue());
                if (actualType.isAssignableFrom(sclass)) {
                    result.add(s);
                }
            }
        }
        return result;        
    }
    
    
    private void printCreateOptions(JspWriter out, Setting setting) 
    throws IOException {
        // FIXME: Use some kind of "subclass database" here.
        // For now just using the existing value if it's a class name
        if (setting.getType().equals(OBJECT_TAG)) {
            out.print("<option value=\"");
            out.print(OBJECT_TAG);
            out.print(", ");
            out.print(Text.attr(setting.getValue()));
            out.print("\" selected=\"selected\">Create ");
            out.print(Text.html(Text.baseName(setting.getValue())));
            out.println("</option>");
        }
    }
    
    
    private Collection<String> getCreateOptions(Setting setting) {
        // FIXME: Use some kind of "subclass database" here.
        // For now just using the existing value if it's a class name
        if (setting.getType().equals(OBJECT_TAG)) {
            return Collections.singleton(setting.getValue());
        }
        
        return Collections.emptySet();        
    }
    
    
    private static boolean validParentType(String type) {
        return type.equals(OBJECT_TAG)
         || type.equals(MAP_TAG)
         || type.equals(LIST_TAG);
    }

    
    private Setting getParentSetting(Setting setting) {
        String path = setting.getPath();

        String parentPath = Text.parentPath(setting.getPath());
        Setting parent = settings.get(parentPath);
        if (parent == null) {
            throw new IllegalStateException("No parent for " + path);
        }
        
        return parent;        
    }


    private Class getActualType(Setting setting) {
        Class result = setting.getActualType();
        if (result != null) {
            return result;
        }
        
        String path = setting.getPath();
        if (path.equals("root")) {
            return Object.class;
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
        } else if (parentType.equals(OBJECT_TAG)) {
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
            if (type.equals(OBJECT_TAG) || type.equals(REFERENCE_TAG)) {
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
        
        boolean selected = setting.getType().equals(REFERENCE_TAG) 
         && reuse.contains(setting.getValue());
        radio(out, "ref_select", "Reuse an existing object:", selected);
        this.printSelectOpenTag(out, "ref_select", setting);
        this.printReuseOptions(out, setting);
        out.println("</select>\n</td>\n</tr>");
        
        selected = setting.getType().equals(OBJECT_TAG)
         && create.contains(setting.getValue());
        radio(out, "obj_select", "Create a new object:", selected);        
        this.printSelectOpenTag(out, "obj_select", setting);
        this.printCreateOptions(out, setting);
        out.println("</select>\n</td>\n</tr>");
        
        selected = setting.getType().equals(REFERENCE_TAG) 
         && !reuse.contains(setting.getValue());
        radio(out, "ref_manual", "Manually enter a path to reuse:", selected);
        String value = setting.getType().equals(REFERENCE_TAG) ? setting.getValue() : "";
        printTextField(out, setting, "ref_manual", value);
        out.println("</td>\n</tr>");
        
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
        try {
            String path = setting.getPath();
            if (path.equals("root")) {
                Map<String,Key<Object>> keys = KeyManager.getKeys(SheetManager.class);
                Key<Object> key = keys.get(path);
                return key.getDescription(Locale.ENGLISH);
            }
            
            Setting parent = getParentSetting(setting);
            String parentType = parent.getType();
            if (parentType.equals(OBJECT_TAG)) {
                Class parentClass = forName(parent.getValue());
                Map<String,Key<Object>> keys = KeyManager.getKeys(parentClass);                
                String keyName = Text.lastPath(path);
                Key<Object> key = keys.get(keyName);
                return key.getDescription(Locale.ENGLISH);
            }
        } catch (Error e) {
            e.printStackTrace();
        }
        
        return null;
    }

}
