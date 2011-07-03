/**
 * This file is licensed under the University of Illinois/NCSA Open Source License. See LICENSE.TXT for details.
 */
package edu.illinois.bundleupdater.client;

import org.eclipse.ui.IStartup;

import edu.illinois.bundleupdater.Updater;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		new Updater("updateSite", "pluginID").checkForUpdates();
	}

}
