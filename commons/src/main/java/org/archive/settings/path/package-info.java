/**

Representations of configurable objects using String paths.  The paths are
similar to file system paths.  Paths require a 
{@link org.archive.settings.Sheet} to be correctly interpreted.

<h2>Path Syntax</h2>

<p>FIXME: Use XPATH syntax per Stack's suggestion.

<p>Paths are constructed out of tokens separated by periods.  A path token is 
either a root object name, a key field name, or an integer index.  There is 
exactly one root object name per path; the root object token is always the very
first token.  Key field tokens are Java identifiers.  Index tokens are positive
integers.

<p>For example, the consider the following:

<pre>
http.DECIDE_RULES.RULES.5.REGEX
</pre>

The above is made out of five tokens:

<ol>
<li>The root object token, <code>http</code>.</li>
<li>A key field token, <code>DECIDE_RULES</code>.</li>
<li>Another key field token, <code>RULES</code>.</li>
<li>An index token, <code>5</code>.</li>
<li>A final key field token, <code>REGEX</code>.</li>
</ol>

<h2>Path Resolution</h2>

Paths require a Sheet object to be resolved into an object.  Resolution occurs
by first looking up the root object using the sheet's manager, and then 
applying each key field and index in the order they occur.

<p>To resolve a key field token, the previously resolved object's class is
looked up in the {@link org.archive.state.KeyManager} to find a key with the
given name.  The value for that key is found using the sheet's 
{@link org.archive.settings.Sheet#resolve(Object, org.archive.state.Key)}
method, passing in the previously resolved object as the processor and the 
found key.  The object returned from <code>resolve(Object,Key)</code> is the
resolution of the key field token.</p>

<p>To resolve an index token, the previously resolved object must be an 
instance of {@link java.util.List}.  The index token is converted to an integer,
and the element at that index is fetched from the list.  That element is 
the resolution of the index token.</p>

<p>For example, consider the following:

<pre>
http.DECIDE_RULES.RULES.5.REGEX
</pre>

<p>Given a Sheet <code>sheet</code>, the above path would be resolved in the 
following way:

<pre>
String[] tokens = path.split('.');
Object root = sheet.getSheetManager().getRoot(tokens[0]);

Key key1 = KeyManager.getKey(root.getClass(), tokens[1]);
Object decideRules = sheet.resolve(root, key1);

Key key2 = KeyManager.getKey(decideRules.getClass(), tokens[2]);
Object rules = sheet.resolve(decideRules, key2);

int index3 = Integer.parseInt(tokens[3]);
Object element = ((List)rules).get(index3);

Key key4 = KeyManager.getKey(element.getClass(), tokens[4]);
Object regex = sheet.resolve(element, key4);
return regex;
</pre>

The {@link PathValidator} class resolves paths into objects for particular
sheets.  The {@link PathLister} class performs the opposite operation; given
a particular sheet, the PathLister will produce path representations for all 
of the objects contained within that sheet.

<h2>Uses/Rationales</h2>

<p>PathLister and PathValidator can be used as a bridge between external 
processes and a SheetManager.  For instance, JMX operations can pass around
String paths, rather than requiring a separate ObjectName for every 
configurable thing.  To create a JMX bridge for the settings system, only 
the SheetManager would need an ObjectName.  This reduces the footprint of the
settings system.

<p>Also, this package can be used to easily create persistent settings stores.
Sheets can be stored as key/value pairs, where the key is a Path and the value
is the object that path should represent.


 */
package org.archive.settings.path;
