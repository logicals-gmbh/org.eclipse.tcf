/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.tabbed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters.FSTreeNodeAdapterFactory.FSTreeNodePeerNodeProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section for displaying the permissions of a linux file/folder.
 */
public class LinuxPermissionsSection extends BaseTitledSection {
	// The original node.
	protected IFSTreeNode node;
	// The copy node to be edited.
	protected IFSTreeNodeWorkingCopy clone;
	// The button of "Permissions"
	protected Button[] btnPermissions;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		btnPermissions = new Button[9];
		Composite comp1 = createPermissionGroup(null, composite, 0, Messages.PermissionsGroup_UserPermissions);
		Composite comp2 = createPermissionGroup(comp1, composite, 3, Messages.PermissionsGroup_GroupPermissions);
		createPermissionGroup(comp2, composite, 6, Messages.PermissionsGroup_OtherPermissions);
	}

	/**
	 * Create a permission group for a role, such as a user, a group or others.
	 *
	 * @param prev The previous permission group to align with.
	 * @param parent The parent composite.
	 * @param bit The permission bit index.
	 * @param header The group's header label.
	 */
	protected Composite createPermissionGroup(Composite prev, Composite parent, int bit, String header) {
		Composite group = getWidgetFactory().createFlatFormComposite(parent);
		FormLayout layout = (FormLayout) group.getLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.spacing = 0;

		FormData data = new FormData();
		data.left = new FormAttachment(0, STANDARD_LABEL_WIDTH);
		data.right = new FormAttachment(100, 0);
		if (prev == null) data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		else data.top = new FormAttachment(prev, ITabbedPropertyConstants.VSPACE);
		group.setLayoutData(data);

		createPermissionButton(Messages.PermissionsGroup_Readable, bit, group);
		createPermissionButton(Messages.PermissionsGroup_Writable, bit + 1, group);
		createPermissionButton(Messages.PermissionsGroup_Executable, bit + 2, group);

		CLabel groupLabel = getWidgetFactory().createCLabel(parent, header);
		data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(group, -ITabbedPropertyConstants.HSPACE);
		data.top = new FormAttachment(group, 0, SWT.TOP);
		groupLabel.setLayoutData(data);

		return group;
	}

	/**
	 * Create a check-box field for a single permission item.
	 *
	 * @param label The label of the permission.
	 * @param index The index of current permission bit mask index.
	 * @param parent The parent to hold the check-box field.
	 */
	private void createPermissionButton(String label, final int index, Composite parent) {
		btnPermissions[index] = getWidgetFactory().createButton(parent, label, SWT.CHECK);
		FormData data = new FormData();
		if ((index % 3) == 0) data.left = new FormAttachment(0, 0);
		else data.left = new FormAttachment(btnPermissions[index - 1], ITabbedPropertyConstants.HSPACE);
		data.right = new FormAttachment(((index % 3) + 1) * 33, 0);
		if ((index % 3) == 0) data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		else data.top = new FormAttachment(btnPermissions[index - 1], 0, SWT.CENTER);
		btnPermissions[index].setLayoutData(data);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerNodeProvider input) {
        Assert.isTrue(input instanceof FSTreeNodePeerNodeProvider);
        this.node = ((FSTreeNodePeerNodeProvider)input).getFSTreeNode();
		this.clone = node.createWorkingCopy();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		for (int i = 0; i < 9; i++) {
			final int bit = 1 << (8 - i);
			SWTControlUtil.setSelection(btnPermissions[i], clone.getPermission(bit));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
    protected String getText() {
	    return Messages.LinuxPermissionsSection_Permissions;
    }
}
