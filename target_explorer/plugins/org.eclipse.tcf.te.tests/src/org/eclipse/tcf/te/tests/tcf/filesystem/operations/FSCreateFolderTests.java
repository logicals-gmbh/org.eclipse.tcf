/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.operations;

import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;


public class FSCreateFolderTests extends OperationTestBase {
	protected FSTreeNode newFolder;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String path = test1Folder.getLocation() + getPathSep() + "newfolder"; //$NON-NLS-1$
		FSTreeNode node = getFSNode(path);
		if (node != null) {
			delete(node);
		}
	}

	public void testCreate() throws Exception {
		newFolder = createFolder("newfolder", test1Folder); //$NON-NLS-1$
		String path = test1Folder.getLocation() + getPathSep() + "newfolder"; //$NON-NLS-1$
		assertTrue(pathExists(path));
	}

	@Override
	protected void tearDown() throws Exception {
		if (newFolder != null) {
			delete(newFolder);
		}
		super.tearDown();
	}

}
