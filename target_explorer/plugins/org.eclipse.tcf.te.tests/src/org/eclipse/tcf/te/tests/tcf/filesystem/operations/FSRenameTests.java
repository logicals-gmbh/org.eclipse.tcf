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


public class FSRenameTests extends OperationTestBase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String path = testFolder.getLocation() + getPathSep() + "hello.txt"; //$NON-NLS-1$
		if (pathExists(path)) {
			FSTreeNode node = getFSNode(path);
			delete(node);
		}
	}

	public void testRename() throws Exception {
		testFile = rename(testFile, "hello.txt"); //$NON-NLS-1$
		String path = testFolder.getLocation() + getPathSep() + "hello.txt"; //$NON-NLS-1$
		assertTrue(pathExists(path));
	}

	@Override
	protected void tearDown() throws Exception {
		rename(testFile, "test.txt"); //$NON-NLS-1$
		super.tearDown();
	}

}
