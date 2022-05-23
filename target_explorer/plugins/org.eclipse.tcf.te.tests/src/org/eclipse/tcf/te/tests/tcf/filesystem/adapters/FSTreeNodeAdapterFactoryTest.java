/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.adapters;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;
import org.eclipse.ui.IActionFilter;

public class FSTreeNodeAdapterFactoryTest extends FSPeerTestCase {
	public void testActionFilterAdapter() {
		IActionFilter filter = (IActionFilter) Platform.getAdapterManager().getAdapter(testFile, IActionFilter.class);
		assertNotNull(filter);
	}

	public void testLabelProviderAdapter() {
		ILabelProvider labelProvider = (ILabelProvider) Platform.getAdapterManager().getAdapter(testFile, ILabelProvider.class);
		assertNotNull(labelProvider);
	}
}
