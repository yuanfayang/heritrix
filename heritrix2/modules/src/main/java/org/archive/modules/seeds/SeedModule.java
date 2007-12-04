/**
 * 
 */
package org.archive.modules.seeds;

import org.archive.openmbeans.annotations.Operation;

/**
 * @author pjack
 *
 */
public interface SeedModule {

    @Operation(desc="Notifies all interested parties that the seeds file has changed.")
    void refreshSeeds();

}
