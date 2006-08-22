package org.archive.monkeys.controller;

/**
 * The factory is used to create Controller instances.  
 * @author Eugene Vahlis [evahlis at cs.toronto.edu]
 */
public class ControllerFactory {
	private static DefaultController dc = null;
	
	/**
	 * Returns an instance of DefaultController. The same instance will
	 * be returned every time in the same VM.
	 */
	public synchronized static Controller getController() {
		if (dc == null) {
			dc = new DefaultController();
		}
		return dc;
	}
}
