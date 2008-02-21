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
 * Module.java
 *
 * Created on Feb 1, 2007
 *
 * $Id:$
 */
package org.archive.state;

import java.io.Serializable;

/**
 * Tagging interface for modules that define static key fields.  If you
 * write a class with key fields, it should implement this interface.
 * It's niether required nor enforced by the framework, but makes it 
 * easier to find things.
 * 
 * <h3>Module Construction and Initialization</h3>
 * 
 * First and foremost, all module types must provide a public, no-argument
 * constructor.  Modules that require additional setup based on an initial
 * configuration should implement the {@link Initializable} interface.
 * 
 * <h3>Context-Sensitive Settings</h3>
 * 
 * Modules should declare static key fields for settings, so the framework
 * knows they exist.  Among other things, the static key fields allow a 
 * user interface to display all the configuration options for a particular
 * module.  See {@link Key} for more information about Key fields and how
 * to create them.
 * 
 * <p>All modules classes <i>must</i> register themselves with the 
 * {@link KeyManager} in order to work with the framework.  This applies even
 * to abstract classes.
 * 
 * <h3>Serialization and Checkpointing</h3>
 * 
 * All modules must implement {@link Serializable}, to allow applications to
 * be checkpointed.  Briefly, checkpointing occurs by serialization the complete
 * graph of configured modules to a file.  The application can then be 
 * restored by deserialization.  See 
 * {@link org.archive.settings.file.Checkpointable} and 
 * {@link org.archive.settings.CheckpointRecovery}
 * for more information.
 * 
 * <h3>Special Considerations for Modules that Need Files</h3>
 * 
 * If your module needs files or directories in order to operate, you should
 * consider using {@link FileModule} to represent them.  FileModule has a 
 * number of advantages:
 * 
 * <ol>
 * <li>It is itself a fully configurable module;</li>
 * <li>The path information of a FileModule is visible to a user interface
 * even if the application is offline;</li>
 * <li>It can gracefully recovers itself from a checkpoint, even if the 
 * directory structure of an application has changed since the checkpoint 
 * occurred.</li>
 * </ol>
 * 
 * <p>If you need to store the contents of a file or files during a checkpoint,
 * then your module should implement 
 * {@link org.archive.settings.file.Checkpointable} 
 * so it can copy the file to the checkpoint directory.  Your 
 * {@link org.archive.settings.file.Checkpointable#checkpoint(java.io.File, java.util.List)}
 * implementation should then register a {@link org.archive.settings.RecoverAction}
 * to copy the file to the new application directory during a checkpoint 
 * recovery.
 * 
 * <p>See {@link org.archive.settings.file.Checkpointable} and 
 * {@link org.archive.settings.CheckpointRecovery} for more information.
 * Examine the source code for {@link org.archive.settings.file.BdbModule} 
 * for an example class that copies files in this manner.
 * 
 * <h3>Metadata and Unit Testing</h3>
 * 
 * Your project should contain a resource file containing metadata descriptions
 * for your module's key fields.  (FIXME: It should also contain a class
 * description, implement and document that.)  See 
 * {@link org.archive.i18n.LocaleCache} for the name and format of this file.
 * 
 * <p>Your unit test for your module class should extend {@link ModuleTestBase},
 * which tests for framework compliance.  The ModuleTestBase class ensures 
 * that you have registered the key fields with the KeyManager (who will
 * in turn ensure that the Key fields are valid), that the module can be
 * serialized and deserialized, and that English-locale key field descriptions 
 * exist for every key in the metadata file.
 * 
 * <p>If the ModleTestBase class detects that the metadata file does not exist,
 * the test will attempt to create the metadata based on the Javadoc 
 * descriptions of the key fields.  The parsing used is naive, so you will 
 * want to edit the resulting file, but this can still prevent the vast majority
 * of duplicate description entry.  If the metadata file already exists, then
 * ModuleTestBase will never overwrite it.
 * 
 * @author pjack
 */
public interface Module extends Serializable {

}
