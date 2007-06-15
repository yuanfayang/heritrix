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

    @Operation(desc = "Creates a new sheet bundle.", impact = Bean.ACTION)
    void makeSheetBundle(
            @Parameter(name = "name", desc = "The name for the new sheet")
            String name,

            @Parameter(name = "sheets", desc = "The names of the sheets "
                    + "to include in the bundle, in order of priority.")
            String sheets);

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

    @Operation(desc = "Associates one or more SURTs with a sheet.")
    void associate(

            @Parameter(name = "sheetName", desc = "The name of the sheet to associate the SURTs with.")
            String sheetName,

            @Parameter(name = "surts", desc = "The surts to associate with that sheet.")
            String[] contexts);

    @org.archive.openmbeans.annotations.Attribute(desc = "The names of the sheets being managed.", def = "")
    String[] getSheets();

    @Operation(desc = "Saves all settings currently in memory to persistent storage.")
    void save();

    @Operation(desc = "Reloads all settings from persistent storage.")
    void reload();

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

    @Operation(desc = "Resolves the value of a setting.")
    String resolve(

            @Parameter(name = "sheetName", desc = "The name of the sheet whose setting to resolve.")
            String sheetName,

            @Parameter(name = "path", desc = "The path to the setting whose value to resolve.")
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

    @Operation(desc = "Returns the sheet associated with the given SURT prefix, if any.")
    String getSheetFor(

    @Parameter(name = "surt", desc = "The SURT whose sheet to return.")
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


}