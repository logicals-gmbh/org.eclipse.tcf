/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.controls;

/**
 * File system tree content provider implementation.
 */
public class FSTreeContentProvider extends FSNavigatorContentProvider {
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.controls.FSNavigatorContentProvider#isRootNodeVisible()
	 */
	@Override
    protected boolean isRootNodeVisible() {
	    return false;
    }
}