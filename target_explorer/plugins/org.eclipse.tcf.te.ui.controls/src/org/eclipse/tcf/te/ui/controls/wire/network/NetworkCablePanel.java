/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.wire.network;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.core.nodes.interfaces.wire.IWireTypeNetwork;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.controls.nls.Messages;
import org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode3;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Network cable wire type wizard configuration panel.
 */
public class NetworkCablePanel extends AbstractWizardConfigurationPanel implements IDataExchangeNode3 {
	private NetworkAddressControl addressControl = null;
	private NetworkPortControl portControl = null;

	private boolean isAutoPort = false;

	/**
	 * Constructor.
	 *
	 * @param parentPageControl The parent control. Must not be <code>null</code>!
	 */
	public NetworkCablePanel(BaseDialogPageControl parentPageControl) {
		super(parentPageControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#dispose()
	 */
	@Override
	public void dispose() {
		if (addressControl != null) { addressControl.dispose(); addressControl = null; }
		if (portControl != null) { portControl.dispose(); portControl = null; }
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

		// Create the wire type section
		Section section = toolkit.createSection(panel, ExpandableComposite.TITLE_BAR);
		Assert.isNotNull(section);
		section.setText(Messages.NetworkCablePanel_section);
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		if (adjustBackgroundColor) section.setBackground(panel.getBackground());

		Composite client = toolkit.createComposite(section);
		Assert.isNotNull(client);
		client.setLayout(new GridLayout());
		client.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (adjustBackgroundColor) client.setBackground(section.getBackground());
		section.setClient(client);

		addressControl = doCreateAddressControl(this);
		addressControl.setFormToolkit(toolkit);
		addressControl.setHasHistory(hasHistory());
		addressControl.setupPanel(client);

		portControl = doCreatePortControl(this);
		portControl.setFormToolkit(toolkit);
		portControl.setHasHistory(hasHistory());
		portControl.setParentControlIsInnerPanel(true);
		portControl.setupPanel(addressControl.getInnerPanelComposite());
		portControl.setEditFieldControlText(getDefaultPort());
	}

	/**
	 * Creates the address control instance.
	 *
	 * @param parentPanel The parent network cable panel. Must not be <code>null</code>.
	 * @return The address control instance.
	 */
	protected NetworkAddressControl doCreateAddressControl(NetworkCablePanel parentPanel) {
		Assert.isNotNull(parentPanel);
		return new NetworkAddressControl(parentPanel);
	}

	/**
	 * Returns the address control.
	 *
	 * @return The address control or <code>null</code>.
	 */
	protected final NetworkAddressControl getAddressControl() {
		return addressControl;
	}

	/**
	 * Creates the port control instance.
	 *
	 * @param parentPanel The parent network cable panel. Must not be <code>null</code>.
	 * @return The port control instance.
	 */
	protected NetworkPortControl doCreatePortControl(NetworkCablePanel parentPanel) {
		Assert.isNotNull(parentPanel);
		return new NetworkPortControl(parentPanel);
	}

	/**
	 * Returns the port control.
	 *
	 * @return The port control or <code>null</code>.
	 */
	protected final NetworkPortControl getPortControl() {
		return portControl;
	}

	/**
	 * Returns the default port to set to the port control.
	 *
	 * @return The default port to set or <code>null</code>.
	 */
	protected String getDefaultPort() {
		return null;
	}

	/**
	 * Returns if or if not the panel controls should be created with history
	 * (combo) or not (text).
	 *
	 * @return <code>True</code> to create the panel controls with history, <code>false</code> otherwise.
	 */
	protected boolean hasHistory() {
		return false;
	}

	/**
	 * Set the auto port state.
	 *
	 * @param value <code>True</code> if the port is an "auto port", <code>false</code> otherwise.
	 */
	protected final void setIsAutoPort(boolean value) {
		isAutoPort = value;

		if (portControl != null) {
	    	if (keepLabelsAlwaysEnabled()) {
	    		SWTControlUtil.setEnabled(portControl.getEditFieldControl(), !isAutoPort);
	    		SWTControlUtil.setEnabled(portControl.getButtonControl(), !isAutoPort);
	    	} else {
	    		portControl.setEnabled(!isAutoPort);
	    	}
		}
	}

	/**
	 * Returns the auto port state.
	 *
	 * @return <code>True</code> if the port is an "auto port", <code>false</code> otherwise.
	 */
	protected final boolean isAutoPort() {
		return isAutoPort;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
	    super.setEnabled(enabled);
	    if (addressControl != null) {
	    	if (keepLabelsAlwaysEnabled()) {
	    		SWTControlUtil.setEnabled(addressControl.getEditFieldControl(), enabled);
	    		SWTControlUtil.setEnabled(addressControl.getButtonControl(), enabled);
	    	} else {
	    		addressControl.setEnabled(enabled);
	    	}
	    }
	    if (portControl != null) {
	    	if (keepLabelsAlwaysEnabled()) {
	    		SWTControlUtil.setEnabled(portControl.getEditFieldControl(), enabled && !isAutoPort);
	    		SWTControlUtil.setEnabled(portControl.getButtonControl(), enabled && !isAutoPort);
	    	} else {
	    		portControl.setEnabled(enabled && !isAutoPort);
	    	}
	    }
	}

	/**
	 * Returns if or if not the control labels shall be kept enabled even
	 * if the state of the control is set to disabled.
	 *
	 * @return <code>True</code> to keep control labels enabled, <code>false</code> otherwise.
	 */
	protected boolean keepLabelsAlwaysEnabled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#isValid()
	 */
	@Override
	public boolean isValid() {
		boolean valid = super.isValid();
		if (!valid) return false;

		valid = addressControl.isValid();
		setMessage(addressControl.getMessage(), addressControl.getMessageType());

		valid &= portControl.isValid();
		if (portControl.getMessageType() > getMessageType()) {
			setMessage(portControl.getMessage(), portControl.getMessageType());
		}

		return valid;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#dataChanged(org.eclipse.tcf.te.runtime.interfaces.nodes.IPropertiesContainer, org.eclipse.swt.events.TypedEvent)
	 */
	@Override
    public boolean dataChanged(IPropertiesContainer data, TypedEvent e) {
		Assert.isNotNull(data);

		boolean isDirty = false;

        @SuppressWarnings("unchecked")
        Map<String, Object> container = (Map<String, Object>)data.getProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME);
		if (container == null) container = new HashMap<String, Object>();

		if (addressControl != null) {
			String address = addressControl.getEditFieldControlText();
			if (address != null) isDirty |= !address.equals(container.get(IWireTypeNetwork.PROPERTY_NETWORK_ADDRESS) != null ? container.get(IWireTypeNetwork.PROPERTY_NETWORK_ADDRESS) : ""); //$NON-NLS-1$
		}

		if (portControl != null) {
			String port = portControl.getEditFieldControlText();
			if (port != null) isDirty |= !port.equals(container.get(IWireTypeNetwork.PROPERTY_NETWORK_PORT) != null ? container.get(IWireTypeNetwork.PROPERTY_NETWORK_PORT) : getDefaultPort() != null ? getDefaultPort() : ""); //$NON-NLS-1$
		}

		String autoPort = (String)container.get(IWireTypeNetwork.PROPERTY_NETWORK_PORT_IS_AUTO);
		if (autoPort == null) autoPort = Boolean.FALSE.toString();
		isDirty |= isAutoPort != Boolean.parseBoolean(autoPort);

		return isDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.interfaces.ISharedDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.nodes.IPropertiesContainer)
	 */
	@Override
    public void setupData(IPropertiesContainer data) {
		if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> container = (Map<String, Object>)data.getProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME);
		if (container == null) container = new HashMap<String, Object>();

		if (addressControl != null) {
			addressControl.setEditFieldControlText((String)container.get(IWireTypeNetwork.PROPERTY_NETWORK_ADDRESS));
		}

		if (portControl != null) {
			String port = (String)container.get(IWireTypeNetwork.PROPERTY_NETWORK_PORT);
			portControl.setEditFieldControlText(port != null && !"".equals(port) ? port : getDefaultPort()); //$NON-NLS-1$
		}

		String autoPort = (String)container.get(IWireTypeNetwork.PROPERTY_NETWORK_PORT_IS_AUTO);
		if (autoPort != null) setIsAutoPort(Boolean.parseBoolean(autoPort));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.interfaces.ISharedDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.nodes.IPropertiesContainer)
	 */
	@Override
    public void extractData(IPropertiesContainer data) {
		if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> container = (Map<String, Object>)data.getProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME);
		if (container == null) container = new HashMap<String, Object>();

		if (addressControl != null) {
			container.put(IWireTypeNetwork.PROPERTY_NETWORK_ADDRESS, addressControl.getEditFieldControlText());
		}

		if (portControl != null) {
			container.put(IWireTypeNetwork.PROPERTY_NETWORK_PORT, portControl.getEditFieldControlText());
		}

		if (isAutoPort) {
			container.put(IWireTypeNetwork.PROPERTY_NETWORK_PORT_IS_AUTO, Boolean.TRUE.toString());
		} else {
			container.remove(IWireTypeNetwork.PROPERTY_NETWORK_PORT_IS_AUTO);
		}

		data.setProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME, !container.isEmpty() ? container : null);
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
		data.setProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode3#copyData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void copyData(IPropertiesContainer src, IPropertiesContainer dst) {
		Assert.isNotNull(src);
		Assert.isNotNull(dst);

        @SuppressWarnings("unchecked")
        Map<String, Object> srcContainer = (Map<String, Object>)src.getProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME);
        Map<String, Object> dstContainer = null;

        if (srcContainer != null) {
        	dstContainer = new HashMap<String, Object>(srcContainer);
        }

        dst.setProperty(IWireTypeNetwork.PROPERTY_CONTAINER_NAME, dstContainer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		super.doSaveWidgetValues(settings, idPrefix);
		if (addressControl != null) addressControl.doSaveWidgetValues(settings, idPrefix);
		if (portControl != null && !portControl.getEditFieldControlText().equals(getDefaultPort())) portControl.doSaveWidgetValues(settings, idPrefix);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
		super.doRestoreWidgetValues(settings, idPrefix);
		if (addressControl != null) addressControl.doRestoreWidgetValues(settings, idPrefix);
		if (portControl != null) portControl.doRestoreWidgetValues(settings, idPrefix);
	}
}
