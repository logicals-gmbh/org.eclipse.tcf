/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.model;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;

/**
 * Peer model location utility implementation.
 */
public final class ModelLocationUtil {

	/**
	 * Returns the local static peers storage root location.
	 *
	 * @return The root location or <code>null</code> if the location cannot be determined.
	 */
	public static IPath getStaticPeersRootLocation() {
		try {
			File file = CoreBundleActivator.getDefault().getStateLocation().append(".peers").toFile(); //$NON-NLS-1$
			boolean exists = file.exists();
			if (!exists) exists = file.mkdirs();
			if (exists && file.canRead() && file.isDirectory()) {
				return new Path(file.toString());
			}
		} catch (IllegalStateException e) {
			/* ignored on purpose */
		}

		// The users local peers lookup directory is $HOME/.tcf/.peers.
		File file = new Path(System.getProperty("user.home")).append(".tcf/.peers").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
		if (file.canRead() && file.isDirectory()) {
			return new Path(file.toString());
		}

		return null;
	}

	/**
	 * Returns the local static locators storage root location.
	 *
	 * @return The root location or <code>null</code> if the location cannot be determined.
	 */
	public static IPath getStaticLocatorsRootLocation() {
		try {
			File file = CoreBundleActivator.getDefault().getStateLocation().append(".locators").toFile(); //$NON-NLS-1$
			boolean exists = file.exists();
			if (!exists) exists = file.mkdirs();
			if (exists && file.canRead() && file.isDirectory()) {
				return new Path(file.toString());
			}
		} catch (IllegalStateException e) {
			/* ignored on purpose */
		}

		// The users local peers lookup directory is $HOME/.tcf/.peers.
		File file = new Path(System.getProperty("user.home")).append(".tcf/.locators").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
		if (file.canRead() && file.isDirectory()) {
			return new Path(file.toString());
		}

		return null;
	}
}
