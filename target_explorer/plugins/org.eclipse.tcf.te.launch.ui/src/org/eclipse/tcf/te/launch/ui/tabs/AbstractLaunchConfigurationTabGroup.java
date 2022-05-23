/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.tabs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.tcf.te.launch.core.lm.LaunchManager;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchManagerDelegate;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ILaunchSelection;
import org.eclipse.tcf.te.launch.ui.selection.LaunchSelectionManager;

/**
 * Abstract launch configuration tab group default implementation.
 * <p>
 * The implementation provided assures to forward launch configuration attributes defaults
 * initialization to the corresponding launch manager delegates before the single launch
 * configuration tabs might want to override these defaults based on their data.
 */
public abstract class AbstractLaunchConfigurationTabGroup extends org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup {
	private ILaunchConfigurationDialog dialog;
	private String mode;

	/**
	 * Constructor.
	 */
	public AbstractLaunchConfigurationTabGroup() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		this.dialog = dialog;
		this.mode = mode;

		// The list of tabs to be set to the launch tab group
		List<ILaunchConfigurationTab> tabs = new ArrayList<ILaunchConfigurationTab>();

		// Create the default launch tabs
		createContextSelectorTab(dialog, tabs, mode);

		// Create and add any additional launch tabs
		createAdditionalTabs(dialog, tabs, mode);

		// Apply the tabs
		setTabs(tabs.toArray(new ILaunchConfigurationTab[tabs.size()]));
	}

	/**
	 * Create the context selector tab.
	 *
	 * @param dialog The launch configuration dialog this tab group is contained in.
	 * @param tabs The list of launch configuration tabs. Must not be <code>null</code>.
	 * @param mode The mode the launch configuration dialog was opened in.
	 */
	public void createContextSelectorTab(ILaunchConfigurationDialog dialog, List<ILaunchConfigurationTab> tabs, String mode) {
		Assert.isNotNull(tabs);
	}

	/**
	 * Hook for subclasses to overwrite to add additional launch configuration tabs to the given
	 * tab list.
	 * <p>
	 * Called from {@link #createTabs(ILaunchConfigurationDialog, String)} before setting the tabs list
	 * to the launch configuration tab group.
	 *
	 * @param dialog The launch configuration dialog this tab group is contained in.
	 * @param tabs The list of launch configuration tabs. Must not be <code>null</code>.
	 * @param mode The mode the launch configuration dialog was opened in.
	 */
	public void createAdditionalTabs(ILaunchConfigurationDialog dialog, List<ILaunchConfigurationTab> tabs, String mode) {
		Assert.isNotNull(tabs);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#dispose()
	 */
	@Override
	public void dispose() {
		dialog = null;
		mode = null;

		super.dispose();
	}

	/**
	 * Returns the associated launch configuration dialog. The dialog is only valid between the
	 * calls of <code>createTabs(...)</code> and <code>dispose()</code>.
	 *
	 * @return The associated launch configuration dialog or <code>null</code>.
	 */
	protected final ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return dialog;
	}

	/**
	 * Returns the launch mode the launch configuration dialog has been opened in. The launch mode
	 * is only valid between the calls of <code>createTabs(...)</code> and <code>dispose()</code>.
	 *
	 * @return The launch mode the launch configuration dialog has been opened in or <code>null</code>.
	 */
	protected final String getLaunchMode() {
		return mode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public final void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		if (configuration != null && getLaunchMode() != null) {
			// extract the launch configuration type from the launch configuration and
			// pass the launch configuration to the corresponding launch manager
			// delegate (based on the extracted launch configuration type) for
			// initializing the defaults within the launch configuration.
			try {
				ILaunchConfigurationType launchConfigType = configuration.getType();
				String launchConfigTypeId = launchConfigType.getIdentifier();
				String launchMode = getLaunchConfigurationDialog().getMode();
				ILaunchSelection launchSelection = LaunchSelectionManager.getInstance().getLaunchSelection(launchConfigType, launchMode, null);
				ILaunchManagerDelegate delegate = LaunchManager.getInstance().getLaunchManagerDelegate(launchConfigType, launchMode);
				Assert.isNotNull(delegate);
				if (launchSelection != null) {
					// create an empty launch configuration specification to initialize all
					// attributes with their default defaults.
					ILaunchSpecification launchSpec = delegate.getLaunchSpecification(launchConfigTypeId, launchSelection);
					// initialize the new launch configuration.
					// ignore validation result of launch spec - init as much attributes as possible
					if (launchSpec != null) {
						delegate.initLaunchConfigAttributes(configuration, launchSpec);
					}
				}
			}
			catch (CoreException e) {
				// Ignore: If we run into the CoreException, we cannot get the launch configuration
				// type. So, what are we supposed to do then?
			}
		}
		// And finally, call the setDefaults methods from the single tabs.
		super.setDefaults(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public final void initializeFrom(ILaunchConfiguration configuration) {
		if (!(configuration instanceof ILaunchConfigurationWorkingCopy)) {
			try {
				configuration = configuration.getWorkingCopy();
			}
			catch (CoreException e) {
			}
		}
		super.initializeFrom(configuration);
	}
}
