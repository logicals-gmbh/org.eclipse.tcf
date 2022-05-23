/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.ui.filetransfer;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.services.filetransfer.FileTransferItem;
import org.eclipse.tcf.te.runtime.services.interfaces.filetransfer.IFileTransferItem;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.dialogs.FSFolderSelectionDialog;
import org.eclipse.tcf.te.tcf.filesystem.ui.dialogs.FSOpenFileDialog;
import org.eclipse.tcf.te.tcf.launch.core.filetransfer.FileTransferItemValidator;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

/**
 * AddEditFileTransferDialog
 */
public class AddEditFileTransferDialog extends CustomTitleAreaDialog {

	final private IFileTransferItem item;
	final IModelNode launchContext;

	private BaseEditBrowseTextControl host;
	private BaseEditBrowseTextControl target;
	private BaseEditBrowseTextControl options;

	private Button toTarget;
	private Button toHost;

	private boolean modeNew = true;


	/**
	 * Constructor.
	 *
	 * @param item The file transfer item to edit or <code>null</code> to create a new one.
	 * @param shell The shell.
	 * @param contextHelpId The context help id.
	 */
	public AddEditFileTransferDialog(Shell shell, String contextHelpId, IFileTransferItem item, IModelNode launchContexts) {
		super(shell, contextHelpId);

		Assert.isNotNull(item);

		this.item = item;
		this.launchContext = launchContexts;
		this.modeNew = item.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createDialogAreaContent(Composite parent) {
	    super.createDialogAreaContent(parent);

		// Set dialog title and default message
		setDialogTitle(modeNew ? Messages.AddEditFileTransferDialog_add_dialogTitle :  Messages.AddEditFileTransferDialog_edit_dialogTitle);
		setTitle(modeNew ? Messages.AddEditFileTransferDialog_add_title :  Messages.AddEditFileTransferDialog_edit_title);
		setDefaultMessage(modeNew ? Messages.AddEditFileTransferDialog_add_message :  Messages.AddEditFileTransferDialog_edit_message, IMessageProvider.INFORMATION);

		// Create the inner panel
		Composite panel = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		@SuppressWarnings("unused")
		Label spacer = new Label(panel, SWT.NONE);

		toTarget = new Button(panel, SWT.RADIO);
		toTarget.setText(Messages.AddEditFileTransferDialog_toTarget_checkbox);
		toTarget.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});

		spacer = new Label(panel, SWT.NONE);

		toHost = new Button(panel, SWT.RADIO);
		toHost.setText(Messages.AddEditFileTransferDialog_toHost_checkbox);
		toHost.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});

		// Create the section sub controls
		host = new BaseEditBrowseTextControl(null) {
			@Override
			protected void onButtonControlSelected() {
				@SuppressWarnings("synthetic-access")
				int direction = toTarget.getSelection() ? IFileTransferItem.HOST_TO_TARGET : IFileTransferItem.TARGET_TO_HOST;
				if (direction == IFileTransferItem.HOST_TO_TARGET) {
					FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
					fileDialog.setFilterPath(getEditFieldControlText());
					fileDialog.setFileName(getEditFieldControlText());
					String file = fileDialog.open();
					if (file != null) {
						setEditFieldControlText(file);
					}
				}
				else {
					DirectoryDialog directoryDialog = new DirectoryDialog(getShell(), SWT.OPEN);
					directoryDialog.setFilterPath(getEditFieldControlText());
					String directory = directoryDialog.open();
					if (directory != null) {
						setEditFieldControlText(directory);
					}
				}
			}
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		host.setEditFieldLabel(Messages.AddEditFileTransferDialog_host_label);
		host.setIsGroup(false);
		host.setHideBrowseButton(false);
		host.setAdjustBackgroundColor(true);
		host.setParentControlIsInnerPanel(true);
		host.setupPanel(panel);
		host.doCreateControlDecoration(host.getEditFieldControl(), panel);

		// Create the section sub controls
		target = new BaseEditBrowseTextControl(null) {
			@Override
			protected void onButtonControlSelected() {
				@SuppressWarnings("synthetic-access")
				int direction = toTarget.getSelection() ? IFileTransferItem.HOST_TO_TARGET : IFileTransferItem.TARGET_TO_HOST;
				ElementTreeSelectionDialog dialog = direction == IFileTransferItem.HOST_TO_TARGET ? new FSFolderSelectionDialog(getShell()) : new FSOpenFileDialog(getShell());
				dialog.setInput(getEditFieldControlText());
				dialog.setInput(launchContext);
				if (dialog.open() == Window.OK) {
					Object candidate = dialog.getFirstResult();
					if (candidate instanceof IFSTreeNode) {
						String absPath = ((IFSTreeNode) candidate).getLocation();
						if (absPath != null) {
							setEditFieldControlText(absPath);
						}
					}
				}
			}
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		target.setEditFieldLabel(Messages.AddEditFileTransferDialog_target_label);
		target.setIsGroup(false);
		target.setHideBrowseButton(false);
		target.setAdjustBackgroundColor(true);
		target.setParentControlIsInnerPanel(true);
		target.setupPanel(panel);
		target.doCreateControlDecoration(target.getEditFieldControl(), panel);

		spacer = new Label(panel, SWT.NONE);
		spacer = new Label(panel, SWT.NONE);

		options = new BaseEditBrowseTextControl(null) {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		options.setEditFieldLabel(Messages.AddEditFileTransferDialog_options_label);
		options.setIsGroup(false);
		options.setHideBrowseButton(true);
		options.setHasHistory(false);
		options.setAdjustBackgroundColor(true);
		options.setParentControlIsInnerPanel(true);
		options.setupPanel(panel);
		options.doCreateControlDecoration(target.getEditFieldControl(), panel);

		applyDialogFont(panel);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);

		restoreWidgetValues();
		validate();

		return control;
	}

	protected void validate() {
		IFileTransferItem wc = new FileTransferItem();
		saveWidgetValues(wc);
		Map<String,String> invalid = FileTransferItemValidator.validate(wc);
		boolean valid = true;

		if (invalid != null && invalid.containsKey(IFileTransferItem.PROPERTY_HOST)) {
			host.updateControlDecoration(invalid.get(IFileTransferItem.PROPERTY_HOST), IMessageProvider.ERROR);
			if (valid) {
				setErrorMessage(invalid.get(IFileTransferItem.PROPERTY_HOST));
			}
			valid = false;
		}
		else {
			host.updateControlDecoration(null, IMessageProvider.NONE);
		}

		if (invalid != null && invalid.containsKey(IFileTransferItem.PROPERTY_TARGET_STRING)) {
			target.updateControlDecoration(invalid.get(IFileTransferItem.PROPERTY_TARGET_STRING), IMessageProvider.ERROR);
			if (valid) {
				setErrorMessage(invalid.get(IFileTransferItem.PROPERTY_TARGET_STRING));
			}
			valid = false;
		}
		else {
			target.updateControlDecoration(null, IMessageProvider.NONE);
		}

		if (valid) {
			setErrorMessage(null);
		}

		getButton(IDialogConstants.OK_ID).setEnabled(valid);
	}

	private void saveWidgetValues(IFileTransferItem wc) {
		wc.setProperty(IFileTransferItem.PROPERTY_HOST, new Path(host.getEditFieldControlText()).toPortableString());
		wc.setProperty(IFileTransferItem.PROPERTY_TARGET_STRING, target.getEditFieldControlText().trim());
		wc.setProperty(IFileTransferItem.PROPERTY_OPTIONS, options.getEditFieldControlText());

		int direction = toTarget.getSelection() ? IFileTransferItem.HOST_TO_TARGET : IFileTransferItem.TARGET_TO_HOST;
		wc.setProperty(IFileTransferItem.PROPERTY_DIRECTION, ""+direction); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#restoreWidgetValues()
	 */
	@Override
	protected void restoreWidgetValues() {
		String hostPath = item.getStringProperty(IFileTransferItem.PROPERTY_HOST);
		host.setEditFieldControlText(hostPath != null ? new Path(hostPath).toOSString() : ""); //$NON-NLS-1$

		String targetPath = item.getStringProperty(IFileTransferItem.PROPERTY_TARGET_STRING);
		target.setEditFieldControlText(targetPath != null ? targetPath : ""); //$NON-NLS-1$
		target.getButtonControl().setEnabled(launchContext != null);

		String optionsString = item.getStringProperty(IFileTransferItem.PROPERTY_OPTIONS);
		options.setEditFieldControlText(optionsString != null ? optionsString : ""); //$NON-NLS-1$

		int direction = item.getIntProperty(IFileTransferItem.PROPERTY_DIRECTION);
		toTarget.setSelection(direction != IFileTransferItem.TARGET_TO_HOST);
		toHost.setSelection(direction == IFileTransferItem.TARGET_TO_HOST);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#saveWidgetValues()
	 */
	@Override
	protected void saveWidgetValues() {
		saveWidgetValues(item);
	}
}
