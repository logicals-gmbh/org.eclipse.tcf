/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.panels;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel;

/**
 * Abstract base implementation of the <code>IWizardConfigurationPanel</code> interface.
 */
public abstract class AbstractWizardConfigurationPanel implements IWizardConfigurationPanel {
	private final BaseDialogPageControl parentControl;
	private Composite topControl = null;

	private String message = null;
	private int messageType = IMessageProvider.NONE;

	private boolean enabled = true;

	/**
	 * Constructor.
	 *
	 * @param parentControl The parent control. Must not be <code>null</code>!
	 */
	public AbstractWizardConfigurationPanel(BaseDialogPageControl parentControl) {
		super();
		Assert.isNotNull(parentControl);
		this.parentControl = parentControl;
	}

	/**
	 * Returns the associated parent control.
	 *
	 * @return The associated parent control. Must be never <code>null</code>!
	 */
	public BaseDialogPageControl getParentControl() {
		return parentControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessage()
	 */
	@Override
	public final String getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessageType()
	 */
	@Override
	public final int getMessageType() {
		return messageType;
	}

	/**
	 * Set the message and the message type this control wants to display in
	 * the outer control or panel.
	 *
	 * @param message The message from this control.
	 * @param messageType The type o the message (NONE, INFORMATION, WARNING, ERROR).
	 */
	protected final void setMessage(String message, int messageType) {
		this.message = message;
		this.messageType = messageType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#dispose()
	 */
	@Override
	public void dispose() {
	}

	/**
	 * Sets the top control.
	 *
	 * @param topControl The top control or <code>null</code>.
	 */
	protected void setControl(Composite topControl) {
		this.topControl = topControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#getControl()
	 */
	@Override
	public Composite getControl() {
		return topControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#getDialogSettingsSectionName()
	 */
	@Override
	public String getDialogSettingsSectionName() {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * @return Returns the enabled state.
     */
    public boolean isEnabled() {
	    return enabled;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#isValid()
	 */
	@Override
	public boolean isValid() {
		setMessage(null, NONE);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#activate()
	 */
	@Override
	public void activate() {
	}
}
