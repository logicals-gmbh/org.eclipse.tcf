/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.wizards;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeContentProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeViewerSorter;
import org.eclipse.tcf.te.tcf.filesystem.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.filesystem.ui.interfaces.IFSConstants;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns.FSTreeElementLabelProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.trees.FilterDescriptor;
import org.eclipse.tcf.te.ui.trees.ViewerStateManager;
import org.eclipse.tcf.te.ui.wizards.pages.AbstractValidatingWizardPage;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * The base wizard page class to create a new file/folder in the file system of Target Explorer.
 */
public abstract class NewNodeWizardPage extends AbstractValidatingWizardPage {
	// The form toolkit to create the content of the wizard page.
	private FormToolkit toolkit;
	// The control for the user to enter the new name.
	private BaseEditBrowseTextControl nameControl;
	// The control for the user to enter the parent directory
	private BaseEditBrowseTextControl folderControl;
	// The viewer of the file tree displaying the file system.
	private TreeViewer treeViewer;

	/**
	 * Create an instance page with the specified page name.
	 *
	 * @param pageName The page name.
	 */
	public NewNodeWizardPage(String pageName) {
		super(pageName);
	}

	/**
	 * Get the page's title.
	 *
	 * @return The page's title.
	 */
	protected abstract String getPageTitle();

	/**
	 * Get the page's description.
	 *
	 * @return The page's description.
	 */
	protected abstract String getPageDescription();

	/**
	 * Get the label of the text field to enter the new name.
	 *
	 * @return The label of the text field to enter the new name.
	 */
	protected abstract String getNameFieldLabel();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// Setup title and description
		setTitle(getPageTitle());
		setDescription(getPageDescription());

		// Create the forms toolkit
		toolkit = new FormToolkit(parent.getDisplay());

		// Create the main panel
		Composite mainPanel = toolkit.createComposite(parent);
		mainPanel.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainPanel.setBackground(parent.getBackground());

		setControl(mainPanel);

		// Setup the help
		PlatformUI.getWorkbench().getHelpSystem()
		                .setHelp(mainPanel, IContextHelpIds.FS_NEW_FILE_WIZARD_PAGE);

		// Do not validate the page while creating the controls
		boolean changed = setValidationInProgress(true);
		// Create the main panel sub controls
		createMainPanelControls(mainPanel);
		// Reset the validation in progress state
		if (changed) setValidationInProgress(false);

		// Adjust the font
		Dialog.applyDialogFont(mainPanel);

		// Validate the page for the first time
		setPageComplete(false);
	}

	/**
	 * Create the main panel of this wizard page.
	 *
	 * @param parent The parent composite in which the page is created.
	 */
	private void createMainPanelControls(Composite parent) {
		Assert.isNotNull(parent);

		// Create the client composite
		Composite client = toolkit != null ? toolkit.createComposite(parent) : new Composite(parent, SWT.NONE);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		client.setBackground(parent.getBackground());

		Label label = new Label(client, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(Messages.NewNodeWizardPage_PromptFolderLabel);

		folderControl = new BaseEditBrowseTextControl(this);
		folderControl.setIsGroup(false);
		folderControl.setHasHistory(false);
		folderControl.setHideBrowseButton(true);
		folderControl.setHideLabelControl(true);
		folderControl.setHideEditFieldControlDecoration(true);
		folderControl.setFormToolkit(toolkit);
		folderControl.setParentControlIsInnerPanel(true);
		folderControl.setupPanel(client);
		folderControl.setEditFieldValidator(new FolderValidator(this));
		NewNodeWizard wizard = getWizard();
		IFSTreeNode folder = wizard.getFolder();
		if (folder != null) folderControl.setEditFieldControlText(folder.getLocation());

		treeViewer = new TreeViewer(client, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		data.heightHint = 193;
		data.widthHint = 450;
		treeViewer.getTree().setLayoutData(data);
		treeViewer.setContentProvider(new FSTreeContentProvider());
		treeViewer.setLabelProvider(createDecoratingLabelProvider(new FSTreeElementLabelProvider()));
		treeViewer.setComparator(new FSTreeViewerSorter());
		treeViewer.addFilter(new DirectoryFilter());
		IPeerNode peer = wizard.getPeer();
		if (peer != null) {
			setInput(peer);
		}
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				onSelectionChanged();
			}
		});
		if (folder != null) treeViewer.setSelection(new StructuredSelection(folder));

		nameControl = new BaseEditBrowseTextControl(this);
		nameControl.setIsGroup(false);
		nameControl.setHasHistory(false);
		nameControl.setHideBrowseButton(true);
		nameControl.setEditFieldLabel(getNameFieldLabel());
		nameControl.setFormToolkit(toolkit);
		nameControl.setParentControlIsInnerPanel(true);
		nameControl.setupPanel(client);
		nameControl.setEditFieldValidator(new NameValidator(this));

		if (folder == null) folderControl.getEditFieldControl().setFocus();
		else nameControl.getEditFieldControl().setFocus();

		// restore the widget values from the history
		restoreWidgetValues();
	}

	/**
	 * The viewer filter to filter out files.
	 */
	static class DirectoryFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IFSTreeNode) {
				IFSTreeNode node = (IFSTreeNode) element;
				if(node.isFile()) return false;
			}
			return true;
		}
	}

	/**
	 * Set the input of the tree viewer and apply the appropriate filters.
	 *
	 * @param input The tree viewer's input.
	 */
    private void setInput(Object input) {
		treeViewer.setInput(input);
		FilterDescriptor[] filterDescriptors = ViewerStateManager.getInstance().getFilterDescriptors(IFSConstants.ID_TREE_VIEWER_FS, input);
		Assert.isNotNull(filterDescriptors);
		for (FilterDescriptor descriptor : filterDescriptors) {
			if (descriptor.isEnabled()) {
				treeViewer.addFilter(descriptor.getFilter());
			}
		}
	}

	/**
	 * Create a decorating label provider using the specified label provider.
	 *
	 * @param labelProvider The label provider that actually provides labels and images.
	 * @return The decorating label provider.
	 */
	private static ILabelProvider createDecoratingLabelProvider(ILabelProvider labelProvider) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IDecoratorManager manager = workbench.getDecoratorManager();
		ILabelDecorator decorator = manager.getLabelDecorator();
		return new DecoratingLabelProvider(labelProvider,decorator);
	}

	/**
	 * Event process handling method when the user select a new folder in the file tree.
	 */
	protected void onSelectionChanged() {
		if (treeViewer.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
			if (selection.getFirstElement() instanceof IFSTreeNode) {
				IFSTreeNode folder = (IFSTreeNode) selection.getFirstElement();
				folderControl.setEditFieldControlText(folder.getLocation());
			}
			else {
				folderControl.setEditFieldControlText(""); //$NON-NLS-1$
			}
		}

		// Update the wizard container UI elements
		IWizardContainer container = getContainer();
		if (container != null && container.getCurrentPage() != null) {
			container.updateWindowTitle();
			container.updateTitleBar();
			container.updateButtons();
		}
		validate();
	}

	/**
	 * Set a new peer as the input of the file tree. Called
	 * by the wizard to update the file tree when an alternative
	 * target peer is selected in the target selection page.
	 *
	 * @param peer The new target peer.
	 */
	public void setPeer(IPeerNode peer) {
		if (peer != null) {
			setInput(peer);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.pages.AbstractValidatingWizardPage#doValidate()
	 */
	@Override
	protected ValidationResult doValidate() {
		ValidationResult result = new ValidationResult();

		boolean valid = true;

		if (folderControl != null) {
			valid &= folderControl.isValid();
			result.setResult(folderControl);
		}

		if (nameControl != null) {
			valid &= nameControl.isValid();
			result.setResult(nameControl);
		}

		result.setValid(valid);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose() {
		if (nameControl != null) { nameControl.dispose(); nameControl = null; }
		if (toolkit != null) { toolkit.dispose(); toolkit = null; }
		super.dispose();
	}

	/**
	 * Get the entered name of this node.
	 *
	 * @return The entered name of this node.
	 */
	public String getNodeName() {
		return nameControl.getEditFieldControlTextForValidation();
	}

	/**
	 * Get the parent wizard. Override the parent method to
	 * cast the result to NewNodeWizard.
	 */
	@Override
	public NewNodeWizard getWizard() {
		return (NewNodeWizard) super.getWizard();
	}

	/**
	 * Get the currently input directory node. It parses
	 * the currently entered path and tries to find the
	 * corresponding directory node in the file system of
	 * the target peer.
	 *
	 * @return The directory node if it exists or else null.
	 */
	public IFSTreeNode getInputDir() {
		NewNodeWizard wizard = getWizard();
		IPeerNode peer = wizard.getPeer();
		if (peer == null) return null;
		final String text = folderControl.getEditFieldControlText();
		if (text != null) {
			String path = text.trim();
			IRuntimeModel rtm = ModelManager.getRuntimeModel(peer);
			if (rtm != null) {
				IResultOperation<IFSTreeNode> operation = rtm.operationRestoreFromPath(path);
				operation.run(null);
				return operation.getResult();
			}
		}
		return null;
	}
}
