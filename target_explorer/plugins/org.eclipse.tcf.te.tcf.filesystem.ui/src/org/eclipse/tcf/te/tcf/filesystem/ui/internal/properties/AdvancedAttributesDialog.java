/*********************************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)	- [345384]Provide property pages for remote file system nodes
 *********************************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;

/**
 * The dialog used to display the advanced attributes of a Windows file or
 * folder.
 */
public class AdvancedAttributesDialog extends Dialog {

	// The file or folder node whose advanced attributes are to be displayed.
	IFSTreeNodeWorkingCopy node;

	/**
	 * Create the advanced attributes dialog with the specified node and a
	 * parent shell.
	 *
	 * @param parentShell
	 *            The parent shell.
	 * @param node
	 *            The file or folder node to be displayed.
	 */
	public AdvancedAttributesDialog(Shell parentShell, IFSTreeNodeWorkingCopy node) {
		super(parentShell);
		this.node = node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite banner = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		banner.setLayout(layout);
		Label label = new Label(banner, SWT.NONE);
		Image bImg = getBannerImage();
		label.setImage(bImg);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		label = new Label(banner, SWT.NONE);
		if (node.isFile()) {
			label.setText(Messages.AdvancedAttributesDialog_FileBanner);
		} else if (node.isDirectory()) {
			label.setText(Messages.AdvancedAttributesDialog_FolderBanner);
		}
		createArchiveAndIndexGroup(composite);
		createCompressAndEncryptGroup(composite);
		return composite;
	}

	/**
	 * Get the image in the banner area.
	 *
	 * @return The image in the banner area.
	 */
	private Image getBannerImage() {
		return UIPlugin.getImage(ImageConsts.BANNER_IMAGE);
	}

	/**
	 * Create the compress and encrypt options group.
	 *
	 * @param parent
	 *            The parent composite where they are created.
	 */
	private void createCompressAndEncryptGroup(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText(Messages.AdvancedAttributesDialog_CompressEncrypt);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		createCompress(group);
		createEncrypt(group);
	}

	/**
	 * Create the archive and indexing options group.
	 *
	 * @param parent
	 *            The parent composite where they are created.
	 */
	private void createArchiveAndIndexGroup(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText(Messages.AdvancedAttributesDialog_ArchiveIndex);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		createArchive(group);
		createIndexField(group);
	}

	/**
	 * Create the indexing option field.
	 *
	 * @param group
	 *            The group widget where the field is created.
	 */
	private void createIndexField(Group group) {
		String label = node.isFile() ? Messages.AdvancedAttributesDialog_IndexFile
				: (node.isDirectory() ? Messages.AdvancedAttributesDialog_IndexFolder
						: null);
		boolean on = !node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED);
		createOptionField(group, label, IWindowsFileAttributes.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED, on);
	}

	/**
	 * Create the archive option field.
	 *
	 * @param group
	 *            The group widget where the field is created.
	 */
	private void createArchive(Group group) {
		String label = node.isFile() ? Messages.AdvancedAttributesDialog_FileArchive
				: (node.isDirectory() ? Messages.AdvancedAttributesDialog_FolderArchive
						: null);
		boolean on = node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_ARCHIVE);
		createOptionField(group, label, IWindowsFileAttributes.FILE_ATTRIBUTE_ARCHIVE, on);
	}

	/**
	 * Create the encrypt option field.
	 *
	 * @param group
	 *            The group widget where the field is created.
	 */
	private void createEncrypt(Group group) {
		String label = Messages.AdvancedAttributesDialog_Encrypt;
		boolean on = node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_ENCRYPTED);
		createOptionField(group, label, IWindowsFileAttributes.FILE_ATTRIBUTE_ENCRYPTED, on);
	}

	/**
	 * Create the compress option field.
	 *
	 * @param group
	 *            The group widget where the field is created.
	 */
	private void createCompress(Group group) {
		String label = Messages.AdvancedAttributesDialog_Compress;
		boolean on = node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_COMPRESSED);
		createOptionField(group, label, IWindowsFileAttributes.FILE_ATTRIBUTE_COMPRESSED, on);
	}

	/**
	 * Create an option field in the specified group, using the specified label,
	 * and with the specified boolean value.
	 *
	 * @param group
	 *            The group widget where the field is created.
	 * @param label
	 *            The label used by the field.
	 * @param bit
	 * 				The bit mask to be changed once the value is changed.
	 * @param on
	 *            The boolean value to be set.
	 */
	private void createOptionField(Group group, String label, final int bit, final boolean on) {
		final Button button = new Button(group, SWT.CHECK);
		button.setText(label);
		button.setSelection(on);
		// Only the owner can edit the properties
		button.setEnabled(node.isAgentOwner());
		button.addSelectionListener(new SelectionAdapter(){
			@Override
            public void widgetSelected(SelectionEvent e) {
				if (button.getSelection() != on) {
					node.setWin32Attr(bit, on);
				}
            }
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.AdvancedAttributesDialog_ShellTitle);
	}

	/**
	 * Get the result.
	 * @return The result.
	 */
	public IFSTreeNodeWorkingCopy getResult() {
	    return node;
    }
}
