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

import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSNavigatorContentProvider;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;

public class ContentProviderTest extends FSPeerTestCase {
	public void testGetChildren() {
		FSNavigatorContentProvider contentProvider = new FSNavigatorContentProvider();
		Object[] children = contentProvider.getChildren(testFolder);
		assertNotNull(children);
		assertTrue(children.length > 0);
	}
	public void testGetParent() {
		FSNavigatorContentProvider contentProvider = new FSNavigatorContentProvider();
		Object parent = contentProvider.getParent(testFile);
		assertEquals(parent, testFolder);
	}
	public void testHasChildren() {
		FSNavigatorContentProvider contentProvider = new FSNavigatorContentProvider();
		assertTrue(contentProvider.hasChildren(testFolder));
	}
}
