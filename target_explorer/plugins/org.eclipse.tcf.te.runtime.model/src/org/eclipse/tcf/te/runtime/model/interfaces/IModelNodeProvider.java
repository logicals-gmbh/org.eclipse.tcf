/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.model.interfaces;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Interface to implement from objects which are associated
 * with model nodes and providing access to them.
 */
public interface IModelNodeProvider extends IAdaptable {

	/**
	 * Returns the associated model node.
	 *
	 * @return The model node or <code>null</code>.
	 */
	public IModelNode getModelNode();
}
