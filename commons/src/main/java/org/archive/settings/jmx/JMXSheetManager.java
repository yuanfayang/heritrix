package org.archive.settings.jmx;

import java.util.Set;

import javax.management.openmbean.CompositeData;

import org.archive.openmbeans.annotations.Attribute;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;

public interface JMXSheetManager {

    Set<String> getSheetNames();

    @Operation(desc = "Removes the sheet with the given name.")
    void removeSheet(
            @Parameter(name = "sheetName", desc = "The name of the sheet to remove.")
            String sheetName) throws IllegalArgumentException;

    @Operation(desc = "Renames a sheet.")
    void renameSheet(

    @Parameter(name = "oldName", desc = "The old name of the sheet.")
    String oldName,

    @Parameter(name = "newName", desc = "The new name for the sheet.")
    String newName);

    @Operation(desc = "Creates a new single sheet.", impact = Bean.ACTION)
    void makeSingleSheet(
            @Parameter(name = "name", desc = "The name for the new sheet")
            String name);

    @Operation(desc = "Returns the settings overriden by the given single sheet.", type = "org.archive.settings.jmx.Types.GET_DATA_ARRAY")
    CompositeData[] getAll(
            @Parameter(name = "name", desc = "The name of the single sheet whose overrides to return.")
            String name);

    @Operation(desc = "Resolves all settings defined by the given sheet.", type = "org.archive.settings.jmx.Types.GET_DATA_ARRAY")
    CompositeData[] resolveAll(
            @Parameter(name = "name", desc = "The name of the single sheet whose overrides to return.")
            String name);

    @Operation(desc = "Alters one or more settings in a single sheet.")
    void setMany(
            @Parameter(name = "sheetName", desc = "The name of the single sheet "
                    + "whose settings to change.")
            String sheetName,
            
            @Parameter(name = "clearErrors", desc = "If true, the manager's list of errors for the " +
            		"sheet will be cleared before making the changes.  Only set to true if you're " +
                        "modifying the entire sheet.")
            boolean clearErrors,

            @Parameter(name = "setData", desc = "An array of path/values to set.", type = "org.archive.settings.jmx.Types.SET_DATA_ARRAY")
            CompositeData[] setData);

    @Operation(desc = "Moves an element in a list up one position.")
    void moveElementUp(
            @Parameter(name = "sheetName", desc = "The name of the sheet containing the list.")
            String sheetName,

            @Parameter(name = "path", desc = "The path to the element to move.")
            String path);

    @Operation(desc = "Moves an element in a list down one position.")
    void moveElementDown(
            @Parameter(name = "sheetName", desc = "The name of the sheet containing the list.")
            String sheetName,

            @Parameter(name = "path", desc = "The path to the element to move.")
            String path);


    @org.archive.openmbeans.annotations.Attribute(desc = "The names of the sheets being managed.", def = "")
    String[] getSheets();

    @Operation(desc = "Sets one setting in a SingleSheet.")
    void set(

            @Parameter(name = "sheetName", desc = "The name of the sheet whose setting to change.")
            String sheet,

            @Parameter(name = "path", desc = "The path to the setting to change.")
            String path,

            @Parameter(name = "type", desc = "The type of that setting.")
            String type,

            @Parameter(name = "value", desc = "The new value for the setting at that path.")
            String value);

    @Operation(desc = "Returns the value of a setting as a string.")
    String get(

            @Parameter(name = "sheetName", desc = "The name of the sheet whose setting to resolve.")
            String sheetName,

            @Parameter(name = "path", desc = "The path to the setting whose value to resolve.")
            String path);
    
    @Operation(desc = "Resolves the value of a setting.", type="org.archive.settings.jmx.Types.GET_DATA")
    CompositeData resolve(

            @Parameter(name = "sheetName", desc = "The name of the sheet whose setting to resolve.")
            String sheetName,

            @Parameter(name = "path", desc = "The path to the setting whose value to resolve.")
            String path);

    @Operation(desc="Returns true if the given sheet contains an override for the given setting.")
    boolean contains(
            @Parameter(name = "sheetName", desc = "The name of the sheet whose setting to check.")
            String sheetName,

            @Parameter(name = "path", desc = "The path to the setting to check.")
            String path);

    

    @Operation(desc = "Associates a sheet with a SURT prefix.")
    void associate(

            @Parameter(name = "sheetName", desc = "The name of the sheet to associate with the SURT.")
            String sheetName,

            @Parameter(name = "surt", desc = "The SURT to associate with that sheet.")
            String surt);

    @Operation(desc = "Disassociates a SURT from a sheet.")
    void disassociate(

            @Parameter(name = "sheetName", desc = "The name of the sheet to disassociate from the SURT.")
            String sheetName,

            @Parameter(name = "surt", desc = "The SURT to disassociate from that sheet.")
            String surt);



    @Operation(desc = "Resolves all settings in the given sheet.")
    String resolveAllAsString(

            @Parameter(name = "sheetName", desc = "The name of the sheet whose settings to resolve.")
            String sheetName);

    @Operation(desc = "Returns only those settings that are overriden by the given sheet.")
    String getAllAsString(

            @Parameter(name = "sheetName", desc = "The name of the sheet whose settings to resolve.")
            String sheetName);

    @Operation(desc="Checks out a sheet for editing.")
    void checkout(
            @Parameter(name="sheetName", desc="The name of the sheet to check out.")
            String sheetName);
    
    @Operation(desc="Commits the changes made to a previously checked out sheet.")
    void commit(
            @Parameter(name="sheetName", desc="The name of the sheet to commit.")
            String sheetName);

    
    @Operation(desc="Cancels any changes made to a checked out sheet.")
    void cancel(
            @Parameter(name="sheetName", desc="The name of the sheet whose changes to cancel.")
            String sheetName);


    @Attribute(desc="The list of currently checked out sheets.", def="")
    String[] getCheckedOutSheets();

    @Attribute(desc="Returns the list of problems encountered while loading sheets.",
            def="")
    String[] getProblemSingleSheetNames();


    @Operation(desc="Returns the list of problems encountered when a sheet was loaded.",
            type="org.archive.settings.jmx.Types.SET_RESULT_ARRAY")
    CompositeData[] getSingleSheetProblems(
            @Parameter(name="sheet", desc="The name of the sheet whose problems to return.")
            String sheet);

    
    @Operation(desc="Returns the settings that will be applied for the given URI.",
            type="org.archive.settings.jmx.Types.GET_DATA_ARRAY")
    CompositeData[] findConfig(
            @Parameter(name="uri", desc="The URI whose settings to return.")
            String uri);


    @Operation(desc="Returns the names of the sheets used to populate the " +
                "settings for the given URI.")
    String[] findConfigNames(
            @Parameter(name="uri", desc="The URI whose sheets to return.")
            String uri);

    @Operation(desc="Returns true if the given sheet is a single sheet, or" +
                "false if the given sheet is a sheet bundle.  Throws an " +
                "exception if the given sheet does not exist.")
    boolean isSingleSheet(           
            @Parameter(name="sheetName", desc="The name of the sheet to test.")
            String sheetName);


    @Attribute(desc="Returns true if the sheet manager is live.", def="false")
    boolean isLive();


    @Operation(desc="Lists the context prefixes associated with a given sheet.")
    String[] listContexts(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose contexts to list.")
            String sheetName, 

            @Parameter(name="start", desc="The number of contexts to skip before listing.")
            int start);

    @Operation(desc="Cleans up a stub manager.")
    void stubCleanup();


    @Operation(desc="Removes a setting from a sheet.")
    void remove(
            
            @Parameter(name="sheetName", desc="The name of the sheet whose setting to remove.")
            String sheetName, 
            
            @Parameter(name="path", desc="The path of the setting to remove.")
            String path);

    @Operation(desc="Returns an absolute file path given a settings path to a FileModule.")
    String getFilePath(
            @Parameter(name="setting", desc="The settings path to the FileModule.")
            String setting);


}
