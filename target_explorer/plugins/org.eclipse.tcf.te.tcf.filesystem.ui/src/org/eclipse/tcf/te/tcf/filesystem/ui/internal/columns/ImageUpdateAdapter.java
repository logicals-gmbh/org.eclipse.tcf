/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns;

import java.io.File;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * The interface to adapt the process of providing the image for two kinds of
 * files, one which has a local copy or one which does not.
 */
public interface ImageUpdateAdapter {

	/**
	 * Get an extension key as the image registry key for the
	 * specified node.
	 *
	 * @param node The node to get the key for.
	 * @return The key used to cache the image descriptor in the registry.
	 */
	public String getImageKey(IFSTreeNode node);

	/**
	 * Return a mirror file that will be used to retrieve the image from.
	 *
	 * @param node The file system tree node.
	 * @return The corresponding mirror file.
	 */
	public File getMirrorFile(IFSTreeNode node);

	/**
	 * Get the image file object for the specified temporary file name.
	 *
	 * @return The file object.
	 */
	public File getImageFile(IFSTreeNode node);
}
