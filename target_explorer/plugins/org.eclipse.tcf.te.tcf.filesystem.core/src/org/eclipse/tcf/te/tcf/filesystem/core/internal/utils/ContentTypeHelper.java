/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)- [345387]Open the remote files with a proper editor
 * William Chen (Wind River)	  [360494]Provide an "Open With" action in the pop
 * 												up menu of file system nodes of Target Explorer.
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;

/**
 * The content type helper used to provide helping methods about the content
 * types of the files in the remote file system.
 */
public class ContentTypeHelper {
	// The binary content type's id.
	private static final String CONTENT_TYPE_BINARY_ID = "org.eclipse.cdt.core.binaryFile"; //$NON-NLS-1$

	/**
	 * Judges if the node is a binary file.
	 *
	 * @param node
	 *            The file node.
	 * @return true if the node is a binary file or else false.
	 */
	public static boolean isBinaryFile(FSTreeNode node) {
		IContentType contentType = getContentType(node);
		if (contentType != null) {
			IContentType binaryFile = Platform.getContentTypeManager()
					.getContentType(CONTENT_TYPE_BINARY_ID);
			if (binaryFile != null && contentType.isKindOf(binaryFile))
				return true;
		}
		return false;
	}

	/**
	 * Get the content type of the specified file node.
	 *
	 * @param node
	 *            The file node.
	 * @return The content type of the file node.
	 */
	public static IContentType getContentType(FSTreeNode node) {
		if (!node.isFile())
			return null;
		if (PersistenceManager.getInstance().isUnresovled(node))
			// If it is already known unresolvable.
			return null;
		IContentType contentType = PersistenceManager.getInstance().getResolved(node);
		if (contentType != null)
			// If it is already known to have a certain content type.
			return contentType;
		// First check the content type by its name.
		contentType = Platform.getContentTypeManager().findContentTypeFor(
				node.getName());
		if (contentType == null) { // Then find the content type by its stream.
			try {
				contentType = findContentTypeByStream(node);
			} catch (Exception e) {
			}
		}
		if (contentType != null) { // If it is resolved, cache it.
			PersistenceManager.getInstance().addResovled(node, contentType);
		} else { // Or else, remember it as an unresolvable.
			PersistenceManager.getInstance().addUnresolved(node);
		}
		return contentType;
	}

	/**
	 * Find the content type of the file using its content stream.
	 *
	 * @param node
	 *            The file node.
	 * @return The content type of the file.
	 * @throws CoreException
	 *             If the path of its local cache file couldn't be found.
	 * @throws IOException
	 *             If something goes wrong during the content type parsing.
	 */
	private static IContentType findContentTypeByStream(FSTreeNode node) throws CoreException, IOException {
		InputStream is = null;
		try {
			File file = CacheManager.getCacheFile(node);
			if (file.exists()) {
				// If the local cache file exits.
				IPath path = CacheManager.getCachePath(node);
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
				is = fileStore.openInputStream(EFS.NONE, null);
			} else {
				// Use its URL stream.
				URL url = node.getLocationURL();
				is = url.openStream();
			}
			return Platform.getContentTypeManager().findContentTypeFor(is, node.getName());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
