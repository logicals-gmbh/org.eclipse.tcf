/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * Interface to be implemented by objects representing a context for a step.
 */
public interface IStepContext extends IAdaptable {

	/**
	 * Returns the context id.
	 *
	 * @return The context id or <code>null</code>.
	 */
	public String getId();

	/**
	 * Returns the context secondary id.
	 *
	 * @return The context secondary id or <code>null</code>.
	 */
	public String getSecondaryId();

	/**
	 * Returns a name/label to be used within the UI to represent this context
	 * to the user.
	 *
	 * @return The name or <code>null</code>.
	 */
	public String getName();

	/**
	 * Returns the context object.
	 *
	 * @return The context Object. Must not be <code>null</code>.
	 */
	public Object getContextObject();

	/**
	 * Returns a possible multi-line string providing detail information
	 * about the context which shall be included in failure messages.
	 *
	 * @param data The step data. Must not be <code>null</code>.
	 * @return The context information or <code>null</code>.
	 */
	public String getInfo(IPropertiesContainer data);
}
