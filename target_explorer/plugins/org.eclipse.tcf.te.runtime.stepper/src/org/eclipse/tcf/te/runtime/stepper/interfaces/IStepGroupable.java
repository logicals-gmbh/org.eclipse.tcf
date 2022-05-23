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

import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;

/**
 * A step groupable.
 */
public interface IStepGroupable {

	/**
	 * Returns the grouped extension instance.
	 *
	 * @return The grouped extension instance.
	 */
	public IExecutableExtension getExtension();

	/**
	 * Returns the groupable secondary id. The primary id is the unique id of the extension.
	 *
	 * @return The groupable secondary id or <code>null</code>.
	 */
	public String getSecondaryId();

	/**
	 * Returns if or if not the step is a singleton. Singleton steps can occur in step groups only
	 * once. Multiple occurrences are forbidden.
	 *
	 * @return <code>True</code> if the step is a singleton, <code>false</code> otherwise.
	 */
	public boolean isSingleton();

	/**
	 * Returns if or if not the step can be removed from a step group by the user.
	 * <p>
	 * The default value is <code>true</code> and can be changed exactly once to <code>false</code>.
	 * Once set to <code>false</code> it must not be changeable anymore.
	 *
	 * @return <code>True</code> if the step can be removed, <code>false</code> otherwise.
	 */
	public boolean isRemovable();

	/**
	 * Returns if or if not the step is hidden from the user.
	 * <p>
	 * The default value is <code>false</code> and can be changed exactly once to <code>true</code>.
	 * Once set to <code>true</code> it must not be changeable anymore.
	 *
	 * @return <code>True</code> if the step is hidden, <code>false</code> otherwise.
	 */
	public boolean isHidden();

	/**
	 * Returns if or if not the step is disabled.
	 * <p>
	 * The default value is <code>false</code> and can be changed exactly once to <code>true</code>.
	 * Once set to <code>true</code> it must not be changeable anymore.
	 *
	 * @return <code>True</code> if the step is disable, <code>false</code> otherwise.
	 */
	public boolean isDisabled();

	/**
	 * If a reference is marked as savepoint, the step or step group will not
	 * be rolled back if it was executed successfully.
	 * @return <code>true</code> if rollback should be done after successful execution.
	 */
	public boolean isSavePoint();

	/**
	 * Returns the list of dependencies. The dependencies of a groupable are checked on execution.
	 * If one of the listed dependencies have not been executed before, the execution of the
	 * groupable will fail.
	 * <p>
	 * The step or step group id might be fully qualified using the form
	 * <code>&quot;primaryId##secondaryId</code>. The <code>secondaryId</code> is optional.
	 *
	 * @return The list of dependencies or an empty list.
	 */
	public String[] getDependencies();
}
