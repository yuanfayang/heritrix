/**
 * A settings system based on "settings sheets".  
 * 
 * <p>Some terms:
 * 
 * <dl>
 * <dt>processor</dt>
 * <dd>An object that requires context-sensitive configuration.  For our 
 * purposes, the context is always a URI in SURT form.  Each processor 
 * advertises which settings it understands.</dd>
 * 
 * <dt>sheet</dt>
 * <dd>A group of settings for one or more processors.  A sheet need not 
 * contain every setting advertised by a processor, nor must a sheet
 * recognize all processors advertised by the manager.  Sheets may either
 * be single sheets or sheet bundles.</dd>
 * 
 * <dt>association</dt>
 * <dd>An association of a context string with a sheet.</dd>
 * 
 * <dt>default sheet</dt>
 * <dd>The sheet that is consulted if no association exists for a particular
 * context string.</dd>
 * 
 * <dt>sheet manager</dt>
 * <dd>A database of sheets, the processors those sheet configure, and the
 * associations for those sheets.  It is expected that a large web crawl
 * will have thousands of sheets with millions of associations.</dd>
 * 
 * </dl>
 *
 * The algorithm for looking up a setting value given the context string
 * (the URI in SURT form), the processor who needs the value, and the Key
 * that defines the property: 
 *
 * <ol>
 * <li>If the SURT string is empty, go to step #6.
 * <li>Check the associations for that exact SURT string.<li>
 * <li>If no association exists for the SURT string, knock off the last bit
 * of the SURT string and go to step 1.</li>
 * <li>Otherwise, consult the sheet indicated by the association.</li>
 * <li>If that sheet returns a non-null value for the processor/Key 
 * combination, then return that value.  Stop.</li>
 * <li>Otherwise, consult the default sheet.
 * <li>If the default sheet returns a non-null value for the processor/Key
 * combination, then return that value.  Stop.</li>
 * <li>Otherwise, return the default value defined by the Key itself.</li>
 * </ol>
 * 
 */
package org.archive.settings;
