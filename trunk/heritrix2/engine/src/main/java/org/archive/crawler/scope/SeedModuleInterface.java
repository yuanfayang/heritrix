/**
 * 
 */
package org.archive.crawler.scope;

import org.archive.openmbeans.annotations.Operation;

/**
 * @author pjack
 *
 */
public interface SeedModuleInterface {

    @Operation(desc="Notifies all interested parties that the seeds file has changed.")
    void refreshSeeds();

}
