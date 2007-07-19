/**
 * A settings system based on "settings sheets".  
 * 
 * <h2>Glossary</h2>
 * 
 * <dl>
 * <dt>context</dt>
 * <dd>A context for settings configuration.  A context is simply a string.  
 * Different applications might use different kinds of context strings; the 
 * Heritrix application uses {@link org.archive.util.SURT} strings.  The 
 * settings system can provide different configuration based on different 
 * configuration contexts, so Heritrix can be configured to behave differently 
 * for different URIs.  The settings system actually uses a prefix-matching
 * algorithm to determine the configuration for a context; see below for
 * details on this algorithm.
 * <dd>
 * 
 * <dt>module</dt>
 * <dd>An object that requires context-sensitive configuration.  Each module
 * advertises the settings that it understands; see the 
 * {@link org.archive.state} package for how to create a module that advertises
 * its settings.  (Basically, module classes must define static 
 * {@link org.archive.state.Key} fields.)
 * </dd>
 * 
 * <dt>sheet</dt>
 * <dd>A group of settings for one or more modules.  A sheet need not 
 * contain every setting advertised by a module, nor must a sheet
 * recognize all modules advertised by the manager.  See {@link Sheet}.
 * </dd>
 * 
 * <dt>association</dt>
 * <dd>An association of a context string prefix with a sheet.  When determining
 * the configuration for a particular context, the sheet manager will consider
 * all associations whose prefix matches that context.  See below for more 
 * details.
 * </dd>
 * 
 * <dt>global sheet</dt>
 * <dd>The settings that are used if no associations exist for a particular
 * context, or if associations do exist but those associations' sheets do not
 * specify a particular setting.  See below for more details.
 * </dd>
 * 
 * <dt>default sheet</dt>
 * <dd>The sheet that is consulted if even the global sheet does not specify
 * a value for a setting.  The values for the default sheet are hardcoded into
 * the module code.  See {@link org.archive.state.Key#getDefaultValue()}.
 * </dd>
 * 
 * <dt>sheet manager</dt>
 * <dd>A database of sheets, the modules those sheet configure, and the
 * associations for those sheets.  It is expected that a large web crawl
 * will have thousands of sheets with millions of associations.  See
 * {@link SheetManager}.</dd>
 * </dd>
 * 
 * <h2>Settings Resolution</h2>
 *
 * There are two phases of setting resolution, the <i>discovery</i> phase and 
 * the <i>mining</i> phase.  The discovery phase produces a list of sheets that 
 * is used by the mining phase to actually look up settings.  The discovery 
 * phase is potentially expensive; it may involve multiple BDB lookups.  The
 * mining phase is comparatively inexpensive since its data structures stay in
 * memory.
 * 
 * <p>In the discovery phase, a list of sheets are found that apply to a 
 * particular context.  The following steps are taken:
 * 
 * <ol>
 * <li>For each prefix of the context, from longest to shortest, the sheet 
 * manager is consulted to see if any associations exist for that prefix.
 * For instance, if the context were <code>HAPPY</code>, then the manager would 
 * check for associations for <code>HAPPY</code>, <code>HAPP</code>, 
 * <code>HAP</code>, <code>HA</code> and <code>H</code>, in that order.</li>
 * <li>The sheets from those associations are added to the result list in the 
 * order they were found.  For instance, if <code>HAPP</code> were associated 
 * with a sheet named <code>foo</code>, and <code>HA</code> were associated with
 * the two sheets <code>bar</code> and <code>baz</code>, then the list would
 * be <code>[foo, bar, baz]</code>.</li>
 * <li>The global sheet and the default sheet are added to the end of the list.
 * Continuing the example, the final sheet list for the context string 
 * <code>HAPPY</code> would be <code>[foo, bar, baz, global, default]</code>.
 * </li>
 * </ol>
 * 
 * After the sheet list is known, it can be consulted during the mining phase
 * to find a particular setting for a module in that context.   Each sheet in 
 * the list is asked if it has a value for setting <i>K</i> of module <i>M</i>.  
 * If so, that value is used.  Otherwise the next sheet is asked.  Note that 
 * the default sheet will always have a value for the setting, as the default 
 * values are hardcoded by the module source code.
 * 
 * <h2>Resolution for Lists and Maps</h2>
 * 
 * If the setting we're resolving is a {@link java.util.List} or a 
 * {@link java.util.Map}, then the mining phase is a little different.  
 * 
 * <h3>Maps</h3>
 * 
 * <p>For maps, each sheet in the discovered sheet list is asked as usual for
 * a value for <i>K</i> in <i>M</i>.  If a sheet reports that it has a value,
 * then that value (which is a Map) is merged into a result Map, instead of
 * overriding the previous value.  The sheets are consulted in the opposite
 * order, and the default sheet is only consulted if no other sheet provided
 * a map value for that setting.  
 * 
 * <p>Using the example above, the discovery phase reported <code>[foo, bar,
 * baz, global, default]</code> as the sheets for the context 
 * <code>HAPPY</code>.  Let's say that the global sheet, the foo sheet and
 * the baz sheet all contained values for a certain map setting.  In that case,
 * the resolved map would be built:
 * 
 * <ol>
 * <li>First by placing all of the global sheet's Map entries into a new map.
 * <li>Next by placing the baz sheet's Map entries into that map, possibly
 * replacing entries from the global map.
 * <li>And finally placing the foo sheet's Map entries into the result map,
 * possibly replacing entries from the baz and global maps.
 * </ol>
 * 
 * This new map is then returned.  Note that a consequence of this approach is
 * that sheets cannot remove entries that other sheets provided; they can only
 * replace those entries with some other value.
 * 
 * <h3>Lists</h3>
 * 
 * List mining is similar to Map mining, but the list entries from the various
 * sheets are appended to the result list.  The sheets are consulted in the
 * opposite order, and the default sheet is only consulted if no other sheet
 * provides list elements.
 * 
 * <p>Continuing the example, let's say that the global sheet, the foo sheet
 * and the baz sheet all contained values for a certain list setting.  Then 
 * the resolved list would contain the list elements from the global sheet,
 * followed by the list elements from the baz sheet, followed by the elements
 * in the foo sheet.
 * 
 * 
 */
package org.archive.settings;
