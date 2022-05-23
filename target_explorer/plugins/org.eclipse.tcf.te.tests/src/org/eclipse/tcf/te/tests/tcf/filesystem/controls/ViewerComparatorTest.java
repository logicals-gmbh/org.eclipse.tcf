/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.controls;

import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeViewerSorter;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;

public class ViewerComparatorTest extends FSPeerTestCase {
	public void testCompare() {
		FSTreeViewerSorter sorter = new FSTreeViewerSorter();
		assertTrue(sorter.compare(null, testFile, test1Folder) > 0);
		assertTrue(sorter.compare(null, test1Folder, test2Folder) < 0);
	}
}
