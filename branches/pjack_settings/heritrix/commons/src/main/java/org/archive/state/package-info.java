/**
 * Defines processes that rely on context-sensitive state.  The 
 * {@link StateProvider} interface defines a generic mechanism for retrieving
 * context-sensitive properties from an object representing the context.
 * The {@link StateProcessor} interface defines a process that relies on
 * a StateProvider for context.
 * 
 * <p>The point of this package is to make it easy for developers to define the 
 * properties that a context-sensitive process needs.  The properties are
 * defined by creating {@link Key} fields and registering them with the
 * static {@link KeyManager}.  
 * 
 * <p>The {@link ExampleConcreteProcessor} exists for unit testing purposes,
 * but it does provide an example of what a concrete context-sensitive 
 * processor would look like.  Much better examples exist over in 
 * {@link org.archive.modules.extractor}, which implements processes that 
 * extract links from documents.
 * 
 * <h2>Rationales</h2>
 * 
 * <p>Package existence: The interfaces in this package are generic to the 
 * point of insolence, but I thought it best to place {@link Key} and 
 * {@link KeyManager} in their own package.</p>
 * 
 * <p>{@link Key}:  So named because it's three letters long.  I usually don't 
 * care about lexical issues like that, but developers are going to have to 
 * define hundreds of these damn things, so calling them
 * OpenMBeanAttributeInfoExtension was right out.  Also, a three-letter long
 * class name means that most Key declarations will fit on a single eighty
 * character line.  Also they really are property keys.
 * 
 * <p>{@link Key}, part 2:  The {@link Key#make(Object)} family of methods
 * makes the common case easy to implement -- by providing a default value,
 * the type of the Key is implied.  The other metadata for the Key is 
 * provided by the field (which provides a name and the owner), and 
 * automatically determined by the KeyManager.  So instead of something like
 * <code>new Key(one, two, three, four, five)</code>, one merely needs to do a 
 * <codde>Key.make(one)</code>, at least for the common case of unconstrained
 * simple keys.
 * 
 * <p>{@link KeyMaker}:  This exists to handle the uncommon case.  By forcing
 * developers to go through a KeyMaker, I think we'll end up with much more
 * legible code.  See the class description for an example of why I think 
 * this is a good idea.
 * 
 * <p>Internationalization:  This was not a stated goal for refactoring the
 * settings framework, but I think it's a good idea.  The Archive partners
 * with people all over the world, so it just seems polite.
 */
package org.archive.state;
