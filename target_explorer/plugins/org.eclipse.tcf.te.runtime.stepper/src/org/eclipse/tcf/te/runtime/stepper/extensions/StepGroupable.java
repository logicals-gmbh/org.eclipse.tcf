/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable;

/**
 * Context step groupable implementation.
 */
public class StepGroupable implements IStepGroupable {
	private String secondaryId = null;
	private boolean disabled = false;
	private boolean hidden = false;
	private boolean removable = true;
	private boolean singleton = false;
	private boolean savePoint = false;
	private boolean optional = false;
	private final List<String> dependencies = new ArrayList<String>();

	private IExecutableExtension extension;

	/**
	 * Constructor.
	 *
	 * @param extension The grouped extension. Must not be <code>null</code>.
	 */
	public StepGroupable(IExecutableExtension extension) {
		this(extension, null);
	}

	/**
	 * Constructor.
	 *
	 * @param extension The grouped extension. Must not be <code>null</code>.
	 * @param secondaryId The groupable secondaryId or <code>null</code>.
	 */
	public StepGroupable(IExecutableExtension extension, String secondaryId) {
		super();
		setExtension(extension);
		setSecondaryId(secondaryId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#getExtension()
	 */
	@Override
	public IExecutableExtension getExtension() {
		return extension;
	}

	/**
	 * Set the grouped extension instance.
	 *
	 * @param extension The grouped extension instance. Must not be <code>null</code>.
	 */
	public void setExtension(IExecutableExtension extension) {
		Assert.isNotNull(extension);
		this.extension = extension;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#getSecondaryId()
	 */
	@Override
	public String getSecondaryId() {
		return secondaryId;
	}

	/**
	 * Steps the groupable secondary id. The primary id is the unique id of the extension.
	 *
	 * @param secondaryId The grouped extension secondary id or <code>null</code>.
	 */
	public void setSecondaryId(String secondaryId) {
		this.secondaryId = secondaryId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#isDisabled()
	 */
	@Override
	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * Sets if or if not the step is disabled.
	 * <p>
	 * The default value is <code>false</code> and can be changed exactly once to <code>true</code>.
	 * Once set to <code>true</code> it must not be changeable anymore.
	 *
	 * @param disabled Specify <code>true</code> if to disable the step, <code>false</code> otherwise.
	 */
	public void setDisabled(boolean disabled) {
		Assert.isTrue(this.disabled == false);
		this.disabled = disabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#isHidden()
	 */
	@Override
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * Sets if or if not the step is hidden from the user.
	 * <p>
	 * The default value is <code>false</code> and can be changed exactly once to <code>true</code>.
	 * Once set to <code>true</code> it must not be changeable anymore.
	 *
	 * @param hidden Specify <code>true</code> if to hide the step, <code>false</code> otherwise.
	 */
	public void setHidden(boolean hidden) {
		Assert.isTrue(this.hidden == false);
		this.hidden = hidden;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#isRemovable()
	 */
	@Override
	public boolean isRemovable() {
		return removable;
	}

	/**
	 * Sets if or if not the step can be removed from a step group by the user.
	 * <p>
	 * The default value is <code>true</code> and can be changed exactly once to <code>false</code>.
	 * Once set to <code>false</code> it must not be changeable anymore.
	 *
	 * @param removable Specify <code>True</code> if the step can be removed, <code>false</code> otherwise.
	 */
	public void setRemovable(boolean removable) {
		Assert.isTrue(this.removable == true);
		this.removable = removable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return singleton;
	}

	/**
	 * Sets if or if not the step is a singleton. Singleton steps can occur in step groups
	 * only once. Multiple occurrences are forbidden.
	 *
	 * @param singleton Specify <code>true</code> if the step is a singleton, <code>false</code> otherwise.
	 */
	public void setSingleton(boolean singleton) {
		Assert.isTrue(this.singleton == false);
		this.singleton = singleton;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#getDependencies()
	 */
	@Override
	public String[] getDependencies() {
		return dependencies.toArray(new String[dependencies.size()]);
	}

	/**
	 * Sets the list of dependencies. The dependencies of a groupable are checked on execution. If
	 * one of the listed dependencies have not been executed before, the execution of the groupable
	 * will fail.
	 * <p>
	 * The context step or context step group id might be fully qualified using the form
	 * <code>&quot;primaryId##secondaryId</code>. The <code>secondaryId</code> is optional.
	 *
	 * @param dependencies The list of dependencies. Must not be <code>null</code>.
	 */
	public void setDependencies(String[] dependencies) {
		Assert.isNotNull(dependencies);
		this.dependencies.clear();
		this.dependencies.addAll(Arrays.asList(dependencies));
	}

	/**
	 * Sets if or if not this reference is a savepoint.
	 * @param isSavePoint <code>true</code> if this reference should be a savepoint.
	 */
	public void setIsSavePoint(boolean isSavePoint) {
		savePoint = isSavePoint;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupable#isSavePoint()
	 */
	@Override
	public boolean isSavePoint() {
	    return savePoint;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isOptional() {
	    return optional;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(getClass().getSimpleName());
		buffer.append(" (" + getExtension().getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(": "); //$NON-NLS-1$
		buffer.append("id = " + getExtension().getId()); //$NON-NLS-1$
		buffer.append(", secondaryId = " + getSecondaryId()); //$NON-NLS-1$
		return buffer.toString();
	}
}
