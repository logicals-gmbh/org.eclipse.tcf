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


public class FSCopyTests extends OperationTestBase {
	protected FSTreeNode newFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String path = test1Folder + getPathSep() + test22File.getName();
		FSTreeNode node = getFSNode(path);
		if (node != null) {
			delete(node);
		}
	}

	public void testCopy() throws Exception {
		newFile = copy(test22File, test1Folder);
		String path = test1Folder.getLocation() + getPathSep() + test22File.getName();
		assertTrue(pathExists(path));
	}

	@Override
	protected void tearDown() throws Exception {
		if (newFile != null) {
			delete(newFile);
		}
		super.tearDown();
	}
}
