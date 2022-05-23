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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters.FSTreeNodeAdapterFactory.FSTreeNodePeerNodeProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display a folder's archive and index attributes on Windows.
 */
public class WindowsFolderAISection extends BaseTitledSection {

	// The original node.
	protected IFSTreeNode node;

	// The check box for archive attribute.
	protected Button archiveButton;
	// The check box for index attribute.
	protected Button indexButton;

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets
	 * .Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);

		archiveButton = getWidgetFactory().createButton(composite, getAchiveText(), SWT.CHECK);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		archiveButton.setLayoutData(data);

		indexButton = getWidgetFactory().createButton(composite, getIndexText(), SWT.CHECK);
		data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(archiveButton, ITabbedPropertyConstants.VSPACE);
		indexButton.setLayoutData(data);
	}

	/**
	 * Get the archive's label text.
	 *
	 * @return The archive's label text.
	 */
	protected String getAchiveText() {
		return Messages.AdvancedAttributesDialog_FolderArchive;
	}

	/**
	 * Get the index's label text.
	 *
	 * @return The index's label text.
	 */
	protected String getIndexText() {
		return Messages.AdvancedAttributesDialog_IndexFolder;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerNodeProvider input) {
        Assert.isTrue(input instanceof FSTreeNodePeerNodeProvider);
        this.node = ((FSTreeNodePeerNodeProvider)input).getFSTreeNode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		SWTControlUtil.setSelection(archiveButton, node != null &&
						node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_ARCHIVE));
		SWTControlUtil.setSelection(indexButton, node != null &&
						!node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.AdvancedAttributesDialog_ArchiveIndex;
	}
}
