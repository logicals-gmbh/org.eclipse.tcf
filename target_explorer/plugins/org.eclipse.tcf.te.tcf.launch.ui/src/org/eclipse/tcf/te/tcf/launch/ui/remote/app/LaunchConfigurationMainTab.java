/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.remote.app;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.launch.ui.launchcontext.AbstractLaunchContextMainTab;
import org.eclipse.tcf.te.ui.forms.CustomFormToolkit;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Remote application main launch tab implementation.
 */
public class LaunchConfigurationMainTab extends AbstractLaunchContextMainTab {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorTab#doCreateAdditionalFormContent(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite, org.eclipse.tcf.te.ui.forms.CustomFormToolkit)
	 */
	@Override
	protected void doCreateAdditionalFormContent(IManagedForm form, Composite parent, CustomFormToolkit toolkit) {
		// Setup the main panel (using the table wrap layout)
		Composite panel = toolkit.getFormToolkit().createComposite(parent);
		GridLayout	layout = new GridLayout(1, false);
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		panel.setBackground(parent.getBackground());

		LaunchConfigurationMainTabSection section = new LaunchConfigurationMainTabSection(form, panel);
		section.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		form.addPart(section);

		LaunchConfigurationAdvancedTabSection advSection = new LaunchConfigurationAdvancedTabSection(form, panel);
		advSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		form.addPart(advSection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return DebugUITools.getImage(ILaunchTypes.REMOTE_APPLICATION);
	}
}
