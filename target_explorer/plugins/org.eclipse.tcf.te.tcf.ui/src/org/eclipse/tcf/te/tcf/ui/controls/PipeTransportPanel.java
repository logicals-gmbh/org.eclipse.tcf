/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.controls.validator.RegexValidator;
import org.eclipse.tcf.te.ui.controls.validator.Validator;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode3;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Pipe transport simulator panel implementation.
 */
public class PipeTransportPanel extends AbstractWizardConfigurationPanel implements IDataExchangeNode3 {

	private PipeNameControl pipeNameControl;

	/**
	 * Local pipe name control implementation.
	 */
	protected class PipeNameControl extends BaseEditBrowseTextControl {

		/**
		 * Constructor.
		 *
		 * @param parentPage The parent dialog page this control is embedded in.
		 *                   Might be <code>null</code> if the control is not associated with a page.
		 */
        public PipeNameControl(IDialogPage parentPage) {
	        super(parentPage);
	        setIsGroup(false);
	        setHasHistory(false);
	        setHideBrowseButton(true);
	        setEditFieldLabel(Messages.PipeNameControl_label);
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doCreateEditFieldValidator()
         */
        @Override
        protected Validator doCreateEditFieldValidator() {
            return new RegexValidator(Validator.ATTR_MANDATORY, ".*"); //$NON-NLS-1$
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#configureEditFieldValidator(org.eclipse.tcf.te.ui.controls.validator.Validator)
         */
        @Override
        protected void configureEditFieldValidator(Validator validator) {
        	if (validator instanceof RegexValidator) {
        		validator.setMessageText(RegexValidator.INFO_MISSING_VALUE, Messages.PipeNameControl_information_missingValue);
        		validator.setMessageText(RegexValidator.ERROR_INVALID_VALUE, Messages.PipeNameControl_error_invalidValue);
        	}
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.ui.controls.BaseDialogPageControl#getValidatingContainer()
         */
        @Override
        public IValidatingContainer getValidatingContainer() {
			return PipeTransportPanel.this.getParentControl().getValidatingContainer();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(ModifyEvent e) {
			super.modifyText(e);
			if (PipeTransportPanel.this.getParentControl() instanceof ModifyListener) {
				((ModifyListener)PipeTransportPanel.this.getParentControl()).modifyText(e);
			}
		}
	}

	/**
	 * Constructor.
	 *
	 * @param parentPageControl The parent control. Must not be <code>null</code>!
	 */
	public PipeTransportPanel(BaseDialogPageControl parentControl) {
		super(parentControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#dispose()
	 */
	@Override
	public void dispose() {
		if (pipeNameControl != null) { pipeNameControl.dispose(); pipeNameControl = null; }
	    super.dispose();
	}

	/**
	 * Returns if or if not to adjust the panels background color.
	 *
	 * @return <code>True</code> to adjust the panels background color, <code>false</code> if not.
	 */
	protected boolean isAdjustBackgroundColor() {
		return getParentControl().getParentPage() != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#setupPanel(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	public void setupPanel(Composite parent, FormToolkit toolkit) {
		Assert.isNotNull(parent);
		Assert.isNotNull(toolkit);

		boolean adjustBackgroundColor = isAdjustBackgroundColor();

		Composite panel = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (adjustBackgroundColor) panel.setBackground(parent.getBackground());

		setControl(panel);

		pipeNameControl = doCreatePipeNameControl(getParentControl().getParentPage());
		pipeNameControl.setFormToolkit(toolkit);
		pipeNameControl.setupPanel(panel);
	}

	/**
	 * Creates the pipe name control instance.
	 *
	 * @param parentPage The parent dialog page or <code>null</code>.
	 * @return The pipe name control instance.
	 */
	protected PipeNameControl doCreatePipeNameControl(IDialogPage parentPage) {
		return new PipeNameControl(parentPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#isValid()
	 */
	@Override
	public boolean isValid() {
		boolean valid = super.isValid();
		if (!valid) return false;

		if (pipeNameControl != null) {
			valid = pipeNameControl.isValid();
			setMessage(pipeNameControl.getMessage(), pipeNameControl.getMessageType());
		}

		return valid;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#dataChanged(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.swt.events.TypedEvent)
	 */
	@Override
	public boolean dataChanged(IPropertiesContainer data, TypedEvent e) {
		Assert.isNotNull(data);

		boolean isDirty = false;

		if (pipeNameControl != null) {
			String pipeName = pipeNameControl.getEditFieldControlText();
			if (pipeName != null) isDirty |= !pipeName.equals(data.getStringProperty("PipeName")); //$NON-NLS-1$
		}

		return isDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.interfaces.ISharedDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {
		if (data == null) return;

		if (pipeNameControl != null) {
			pipeNameControl.setEditFieldControlText(data.getStringProperty("PipeName")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.interfaces.ISharedDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		if (data == null) return;

		if (pipeNameControl != null) {
			String pipeName = pipeNameControl.getEditFieldControlText();
			data.setProperty("PipeName", !"".equals(pipeName) ? pipeName : null); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode2#initializeData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void initializeData(IPropertiesContainer data) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode3#removeData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void removeData(IPropertiesContainer data) {
		if (data == null) return;
		data.setProperty("PipeName", null); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode3#copyData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void copyData(IPropertiesContainer src, IPropertiesContainer dst) {
		Assert.isNotNull(src);
		Assert.isNotNull(dst);
        dst.setProperty("PipeName", src.getProperty("PipeName")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		super.doSaveWidgetValues(settings, idPrefix);
		if (pipeNameControl != null) pipeNameControl.doSaveWidgetValues(settings, idPrefix);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
		super.doRestoreWidgetValues(settings, idPrefix);
		if (pipeNameControl != null) pipeNameControl.doRestoreWidgetValues(settings, idPrefix);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		if (pipeNameControl != null) {
			SWTControlUtil.setEnabled(pipeNameControl.getEditFieldControl(), enabled);
		}
	}
}
