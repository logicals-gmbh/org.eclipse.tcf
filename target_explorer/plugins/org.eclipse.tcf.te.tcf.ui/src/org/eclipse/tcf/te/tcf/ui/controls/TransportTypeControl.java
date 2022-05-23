/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.tcf.core.interfaces.ITransportTypes;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;

/**
 * Transport type control implementation.
 */
public class TransportTypeControl extends BaseEditBrowseTextControl {

	private final static List<String> TRANSPORT_TYPES = Arrays.asList(new String[] {
																		ITransportTypes.TRANSPORT_TYPE_TCP,
																		ITransportTypes.TRANSPORT_TYPE_SSL,
																		ITransportTypes.TRANSPORT_TYPE_PIPE,
																		ITransportTypes.TRANSPORT_TYPE_CUSTOM
															});

	/**
	 * Constructor.
	 *
	 * @param parentPage The parent dialog page this control is embedded in.
	 *                   Might be <code>null</code> if the control is not associated with a page.
	 */
	public TransportTypeControl(IDialogPage parentPage) {
		super(parentPage);
		setIsGroup(false);
		setReadOnly(true);
		setHideBrowseButton(true);
		setEditFieldLabel(Messages.TransportTypeControl_label);
	}

	/**
	 * Returns the list of transport types supported by this control.
	 *
	 * @return The list of supported transport types.
	 */
	public String[] getTransportTypes() {
		return TRANSPORT_TYPES.toArray(new String[TRANSPORT_TYPES.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#setupPanel(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void setupPanel(Composite parent) {
		super.setupPanel(parent);

		List<String> transportTypeLabels = new ArrayList<String>();
		for (String transportType : getTransportTypes()) {
			String label = getTransportTypeLabel(transportType);
			if (label != null) transportTypeLabels.add(label);
		}

		setEditFieldControlHistory(transportTypeLabels.toArray(new String[transportTypeLabels.size()]));
		SWTControlUtil.select(getEditFieldControl(), 0);
		SWTControlUtil.setEnabled(getEditFieldControl(), transportTypeLabels.size() > 1);
	}

	/**
	 * Returns the label of the given transport type.
	 *
	 * @param transportType The transport type. Must not be <code>null</code>.
	 * @return The corresponding label or <code>null</code> if the transport type is unknown.
	 */
	protected String getTransportTypeLabel(String transportType) {
		Assert.isNotNull(transportType);

		if (ITransportTypes.TRANSPORT_TYPE_TCP.equals(transportType)) return Messages.TransportTypeControl_tcpType_label;
		else if (ITransportTypes.TRANSPORT_TYPE_SSL.equals(transportType)) return Messages.TransportTypeControl_sslType_label;
		else if (ITransportTypes.TRANSPORT_TYPE_PIPE.equals(transportType)) return Messages.TransportTypeControl_pipeType_label;
		else if (ITransportTypes.TRANSPORT_TYPE_CUSTOM.equals(transportType)) return Messages.TransportTypeControl_customType_label;

		return null;
	}

	/**
	 * Returns the currently selected transport type.
	 *
	 * @return The currently selected transport type.
	 */
	public String getSelectedTransportType() {
		String type = getEditFieldControlText();

		if (Messages.TransportTypeControl_tcpType_label.equals(type)) type = ITransportTypes.TRANSPORT_TYPE_TCP;
		else if (Messages.TransportTypeControl_sslType_label.equals(type)) type = ITransportTypes.TRANSPORT_TYPE_SSL;
		else if (Messages.TransportTypeControl_pipeType_label.equals(type)) type = ITransportTypes.TRANSPORT_TYPE_PIPE;
		else if (Messages.TransportTypeControl_customType_label.equals(type)) type = ITransportTypes.TRANSPORT_TYPE_CUSTOM;

		return type;
	}

	/**
	 * Sets the selected transport type to the specified one.
	 *
	 * @param transportType The transport type. Must not be <code>null</code>.
	 */
	public void setSelectedTransportType(String transportType) {
		Assert.isNotNull(transportType);

		// Get the transport type label for given transport type
		String label = getTransportTypeLabel(transportType);
		int index = SWTControlUtil.indexOf(getEditFieldControl(), label);
		if (index != -1) SWTControlUtil.select(getEditFieldControl(), index);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
		// The widget is not user editable and the history is used
		// for presenting the available transport types. Neither save
		// or restore the history actively.
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		// The widget is not user editable and the history is used
		// for presenting the available transport types. Neither save
		// or restore the history actively.
	}
}
