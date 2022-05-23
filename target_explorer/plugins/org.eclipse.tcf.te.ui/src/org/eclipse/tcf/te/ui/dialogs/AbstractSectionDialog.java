/*******************************************************************************
 * Copyright (c) 2013, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.forms.CustomFormToolkit;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.interfaces.IDataExchangeDialog;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.interfaces.data.IUpdatable;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Section dialog implementation.
 */
public abstract class AbstractSectionDialog extends CustomTitleAreaDialog implements IValidatingContainer, IDataExchangeDialog {
	// References to the sections
	private AbstractSection[] sections = null;

	private IPropertiesContainer data = null;
	private final boolean readOnly;
	private final String dialogTitle;
	private final String title;
	private final String message;

	/**
	 * Constructor.
	 *
	 * @param shell The active shell.
	 * @param dialogTitle The dialog window title.
	 * @param title The title area title.
	 * @param message The title aerea message.
	 * @param readOnly <code>True</code> to open the dialog in read-only mode, <code>false</code> otherwise.
	 * @param contextHelpId The context help id or <code>null</code>.
	 */
	public AbstractSectionDialog(Shell shell, String dialogTitle, String title, String message, boolean readOnly, String contextHelpId) {
		super(shell, contextHelpId);

		this.dialogTitle = dialogTitle;
		this.title = title;
		this.message = message;
		this.readOnly = readOnly;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#doGetDialogSettingsToInitialize()
	 */
	@Override
	protected IDialogSettings doGetDialogSettingsToInitialize() {
		return UIPlugin.getDefault().getDialogSettings();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return super.getDialogSettings();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create the sections for this dialog.
	 * @param form The managed form.
	 * @param parent The parent composite.
	 * @return The sections.
	 */
	protected abstract AbstractSection[] createSections(IManagedForm form, Composite parent);

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createDialogAreaContent(Composite parent) {
		super.createDialogAreaContent(parent);

		setDialogTitle(dialogTitle);
		setTitle(title);
		setMessage(message);

		FormToolkit toolkit = new FormToolkit(getShell().getDisplay());
		toolkit.setBackground(parent.getBackground());
		ScrolledForm scrolledForm = new CustomFormToolkit(toolkit).createScrolledForm(parent, null, true);
		scrolledForm.setLayoutData(getSectionAreaLayoutData());
		scrolledForm.getBody().setLayout(FormLayoutFactory.createClearGridLayout(false, 1));

		IManagedForm form = new ManagedForm(toolkit, scrolledForm) {
			@Override
			public void dirtyStateChanged() {
				updateSections();
				validate();
			}
			@Override
			public void staleStateChanged() {
				validate();
			}
		};

		sections = createSections(form, scrolledForm.getBody());
		if (sections != null) {
			for (AbstractSection section : sections) {
				section.setReadOnly(readOnly);
				form.addPart(section);
			}
		}

		restoreWidgetValues();
		setupData(data);

		applyDialogFont(scrolledForm.getBody());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#getDialogSettingsSectionName()
	 */
	@Override
	public String getDialogSettingsSectionName() {
		return getDialogSettingsSection(getClass());
	}

	protected String getDialogSettingsSection(Class<?> clazz) {
		String name = clazz.getSimpleName();
		Class<?> enclosing = clazz.getEnclosingClass();
		while ((name == null || name.trim().length() == 0) && enclosing != null) {
			name = enclosing.getSimpleName();
			if (name != null && name.trim().length() > 0) {
				name = name + "." + AbstractSectionDialog.class.getSimpleName(); //$NON-NLS-1$
			}
			enclosing = enclosing.getEnclosingClass();
		}

		return (name != null && name.trim().length() > 0) ? name : "SectionDialog"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		validate();
		return control;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#getInitialSize()
	 */
	@Override
	protected final Point getInitialSize() {
		return adjustInitialSize(super.getInitialSize());
	}

	/**
	 * Adjust the initial size of the dialog.
	 *
	 * @param size The current initial size. Must not be <code>null</code>.
	 * @return The new initial size.
	 */
	protected Point adjustInitialSize(Point size) {
		Assert.isNotNull(size);
    	if (Host.isLinuxHost()) {
    		size = new Point(size.x, size.y+5);
    	}
		return size;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {
		Assert.isNotNull(data);

		this.data = new PropertiesContainer();
		this.data.setProperties(data.getProperties());

		if (sections != null) {
			for (AbstractSection section : sections) {
				if (section instanceof IDataExchangeNode) {
					((IDataExchangeNode)section).setupData(data);
				}
				else if (Platform.inDebugMode()){
					Platform.getLog(UIPlugin.getDefault().getBundle()).log(new Status(IStatus.WARNING,
									UIPlugin.getUniqueIdentifier(),
									"Section "+section.getClass().getName()+" does not implement IDataExchangeNode!")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			updateSections();
		}
	}

	public void updateSections() {
		IPropertiesContainer workingData = new PropertiesContainer();
		workingData.setProperties(data.getProperties());
		if (sections != null) {
			// get working data
			internalExtractData(workingData);
			// update sections
			for (AbstractSection section : sections) {
				if (section instanceof IUpdatable) {
					((IUpdatable)section).updateData(workingData);
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		Assert.isNotNull(data);
		data.setProperties(this.data.getProperties());
	}

	protected void internalExtractData(IPropertiesContainer data) {
		for (AbstractSection section : sections) {
			if (section instanceof IDataExchangeNode) {
				((IDataExchangeNode)section).extractData(data);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#dispose()
	 */
	@Override
	protected void dispose() {
	    super.dispose();
		if (sections != null) {
			// get working data
			for (AbstractSection section : sections) {
				section.dispose();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		// Extract the properties
		if (data == null) {
			data = new PropertiesContainer();
		}
		if (sections != null) {
			for (AbstractSection section : sections) {
				if (section instanceof IDataExchangeNode) {
					((IDataExchangeNode)section).extractData(data);
				}
				else if (Platform.inDebugMode()){
					Platform.getLog(UIPlugin.getDefault().getBundle()).log(new Status(IStatus.WARNING,
									UIPlugin.getUniqueIdentifier(),
									"Section "+section.getClass().getName()+" does not implement IDataExchangeNode!")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		super.okPressed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#restoreWidgetValues()
	 */
	@Override
	protected void restoreWidgetValues() {
		super.restoreWidgetValues();
		if (sections != null) {
			for (AbstractSection section : sections) {
				section.restoreWidgetValues(getDialogSettings());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#saveWidgetValues()
	 */
	@Override
	protected void saveWidgetValues() {
		super.saveWidgetValues();
		if (sections != null) {
			for (AbstractSection section : sections) {
				section.saveWidgetValues(getDialogSettings());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer#validate()
	 */
	@Override
	public void validate() {
		boolean valid = true;
		if (sections != null) {
			ValidationResult result = new ValidationResult();
			for (AbstractSection section : sections) {
				valid &= section.isValid();
				result.setResult(section);
			}

			valid &= doAdditionalValidation(result);

			setMessage(result.getMessage(), result.getMessageType());
			if (!isMessageSet()) {
				setMessage(message);
			}
		}
		if (getButton(IDialogConstants.OK_ID) != null) {
			getButton(IDialogConstants.OK_ID).setEnabled(!readOnly && valid);
		}
	}

	protected boolean doAdditionalValidation(ValidationResult result) {
		return true;
	}

	protected GridData getSectionAreaLayoutData() {
		return new GridData(SWT.FILL, SWT.CENTER, true, false);
	}
}
