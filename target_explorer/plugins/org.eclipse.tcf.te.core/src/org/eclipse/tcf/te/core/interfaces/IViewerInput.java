/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.core.interfaces;

/**
 * A viewer input is an input of the AbstractTreeControl.
 * <p>
 * If the input of AbstractTreeControl is an instance of or adapted to IViewerInput,
 * AbstractTreeControl adds a property change listener to the input and update
 * its UI including the tree viewer and the tool bar when the properties of the input
 * have changed.
 * 
 * @see AbstractTreeControl
 */
public interface IViewerInput {

	/**
	 * Get the id of the input used to persist the state of the tree viewer
	 * as the persistence id.
	 */
	String getInputId();
}
