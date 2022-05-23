/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.extensions.manager;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy;
import org.eclipse.tcf.te.runtime.stepper.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.stepper.extensions.StepGroup;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroup;

/**
 * Step group extension proxy implementation.
 */
public final class StepGroupExtensionProxy extends ExecutableExtensionProxy<IStepGroup> {
	private List<IConfigurationElement> groupExtensions = new ArrayList<IConfigurationElement>();

	/**
	 * Constructor.
	 *
	 * @param element The configuration element. Must not be <code>null</code>.
	 */
	public StepGroupExtensionProxy(IConfigurationElement element) throws CoreException {
		super(element);
	}

	/**
	 * Add a duplicate group extension that should be used to extend the launch group.
	 *
	 * @param element The configuration element. Must not be <code>null</code>.
	 */
	public void addGroupExtension(IConfigurationElement element) {
		Assert.isNotNull(element);
		groupExtensions.add(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy#newInstance()
	 */
	@Override
	public IStepGroup newInstance() {
		// Create the instance
		StepGroup instance = new StepGroup();
		// and initialize
		try {
			instance.setInitializationData(getConfigurationElement(), getConfigurationElement().getName(), null);
			for (IConfigurationElement groupExtension : groupExtensions) {
				instance.doSetInitializationData(groupExtension, groupExtension.getName(), null);
			}
		} catch (CoreException e) {
			// initialization failed -> reset instance
			Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(e.getStatus());
			instance = null;
		}
		return instance;
	}
}
