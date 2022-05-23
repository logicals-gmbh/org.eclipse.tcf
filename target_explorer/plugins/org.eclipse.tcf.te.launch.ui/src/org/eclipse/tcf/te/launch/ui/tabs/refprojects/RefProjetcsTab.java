/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.tabs.refprojects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.launch.ui.nls.Messages;
import org.eclipse.tcf.te.launch.ui.tabs.AbstractFormsLaunchConfigurationTab;
import org.eclipse.tcf.te.ui.forms.CustomFormToolkit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.ide.IDE;

/**
 * Referenced projetcs launch configuration tab implementation.
 */
public class RefProjetcsTab extends AbstractFormsLaunchConfigurationTab {
	// References to the tab sub sections
	private RefProjectsSection section;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.AbstractFormsLaunchConfigurationTab#dispose()
	 */
	@Override
	public void dispose() {
		if (section != null) { section.dispose(); section = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.AbstractFormsLaunchConfigurationTab#doCreateFormContent(org.eclipse.swt.widgets.Composite, org.eclipse.tcf.te.ui.forms.CustomFormToolkit)
	 */
	@Override
	protected void doCreateFormContent(Composite parent, CustomFormToolkit toolkit) {
		Assert.isNotNull(parent);
		Assert.isNotNull(toolkit);

		// Setup the main panel (using the table wrap layout)
		Composite panel = toolkit.getFormToolkit().createComposite(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.makeColumnsEqualWidth = true;
		layout.numColumns = 1;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		panel.setBackground(parent.getBackground());

		section = new RefProjectsSection(getManagedForm(), panel);
		section.getSection().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP));
		getManagedForm().addPart(section);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	@Override
	public String getName() {
		return Messages.ReferencedProjectsTab_name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
	}
}
