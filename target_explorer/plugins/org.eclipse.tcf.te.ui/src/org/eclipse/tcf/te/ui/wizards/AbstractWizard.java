/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.wizards.pages.AbstractValidatingWizardPage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * An abstract wizard implementation.
 * <p>
 * This wizard implementation is adding dialog settings management.
 */
public abstract class AbstractWizard extends Wizard implements IWorkbenchWizard {
	// A marker to remember if the dialog settings got
	// initialized for this wizard
	private boolean dialogSettingsInitialized = false;

	// The workbench instance passed to the wizard via IWorkbenchWizard#init.
	private IWorkbench workbench = null;
	// The selection passed to the wizard via IWorkbenchWizard#init.
	private IStructuredSelection selection = null;

	protected IPropertiesContainer data;

	/**
	 * Initialize the dialog settings and associate them with the wizard.
	 */
	private final void initializeDialogSettings() {
		// Get the root dialog settings
		IDialogSettings rootSettings = getRootDialogSettings();
		// Get the wizards dialog settings section
		IDialogSettings section = rootSettings.getSection(getWizardSectionName());
		if (section == null) {
			// The section does not exist -> create it
			section = rootSettings.addNewSection(getWizardSectionName());
		}
		// Push the section to the wizard
		setDialogSettings(section);
		// Mark the dialog settings initialized
		dialogSettingsInitialized = true;
	}

	/**
	 * Returns the root dialog settings.
	 * <p>
	 * Typically, this are the dialog settings of the parent bundle. The
	 * default implementation returns the dialog settings of the bundle
	 * &quot;<code>org.eclipse.tcf.te.ui</code>&quot;. Overwrite to return
	 * different root dialog settings.
	 *
	 * @return The root dialog settings.
	 */
	protected IDialogSettings getRootDialogSettings() {
		return UIPlugin.getDefault().getDialogSettings();
	}

	/**
	 * Returns the name of the wizards associated dialog settings
	 * section.
	 * <p>
	 * The default implementation returns the simple name of the
	 * implementation class.
	 *
	 * @return The name of the wizards dialog settings section.
	 */
	protected String getWizardSectionName() {
		return getClass().getName();
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPageControls(Composite pageContainer) {
	    super.createPageControls(pageContainer);

	    data = getInitialData();
	    if (data == null) {
	    	data = new PropertiesContainer();
	    }
	    for (IWizardPage page : getPages()) {
	    	if (page instanceof IDataExchangeNode) {
	    		((IDataExchangeNode)page).setupData(data);
	    		if (page instanceof AbstractValidatingWizardPage) {
	    			((AbstractValidatingWizardPage)page).validate();
	    		}
	    	}
	    }
	}

	/**
	 * Get the data to initialize wizard pages.
	 * @return The initial data for all wizard pages or <code>null</code>.
	 */
	protected IPropertiesContainer getInitialData() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getDialogSettings()
	 */
	@Override
	public IDialogSettings getDialogSettings() {
		if (!dialogSettingsInitialized) {
			initializeDialogSettings();
		}
		return super.getDialogSettings();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	/**
	 * Returns the workbench instance.
	 * <p>
	 * <b>Note:</b> The workbench instance is set via {@link IWorkbenchWizard#init(IWorkbench, IStructuredSelection)}.
	 *
	 * @return The workbench instance or <code>null</code>.
	 */
	public final IWorkbench getWorkbench() {
		return workbench;
	}

	/**
	 * Returns the selection.
	 * <p>
	 * <b>Note:</b> The selection is set via {@link IWorkbenchWizard#init(IWorkbench, IStructuredSelection)}.
	 *
	 * @return The selection or <code>null</code>.
	 */
	public final IStructuredSelection getSelection() {
		return selection;
	}
}
