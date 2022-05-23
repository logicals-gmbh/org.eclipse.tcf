/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)	[360494]Provide an "Open With" action in the pop
 * 								up menu of file system nodes of Target Explorer.
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.EditorSelectionDialog;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * A menu for opening files in the target explorer.
 * <p>
 * An <code>OpenWithMenu</code> is used to populate a menu with "Open With" actions. One action is
 * added for each editor which is applicable to the selected file. If the user selects one of these
 * items, the corresponding editor is opened on the file.
 * </p>
 *
 * @since 3.7 - Copied and modified based on org.eclipse.ui.actions.OpenWithMenu to avoid
 *        introducing org.eclipse.core.resources
 */
public class OpenWithMenu extends ContributionItem {
	private static final String DEFAULT_TEXT_EDITOR = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$

	/**
	 * The id of this action.
	 */
	public static final String ID = UIPlugin.getUniqueIdentifier() + ".OpenWithMenu";//$NON-NLS-1$

	/*
	 * Compares the labels from two IEditorDescriptor objects
	 */
	private static final Comparator<IEditorDescriptor> comparer = new Comparator<IEditorDescriptor>() {
		private Collator collator = Collator.getInstance();

		@Override
		public int compare(IEditorDescriptor arg0, IEditorDescriptor arg1) {
			String s1 = arg0.getLabel();
			String s2 = arg1.getLabel();
			return collator.compare(s1, s2);
		}
	};
	// The selected tree node.
	IFSTreeNode node;
	// The current workbench page.
	IWorkbenchPage page;
	// The editor registry.
	IEditorRegistry registry;

	/**
	 * Create an instance using the specified page and the specified IFSTreeNode.
	 *
	 * @param page The page to open the editor.
	 * @param node The IFSTreeNode to be opened.
	 */
	public OpenWithMenu(IWorkbenchPage page, IFSTreeNode node) {
		super(ID);
		this.node = node;
		this.page = page;
		this.registry = PlatformUI.getWorkbench().getEditorRegistry();
	}

	/**
	 * Returns an image to show for the corresponding editor descriptor.
	 *
	 * @param editorDesc the editor descriptor, or null for the system editor
	 * @return the image or null
	 */
	private Image getImage(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = getImageDescriptor(editorDesc);
		if (imageDesc == null) {
			return null;
		}
		return imageDesc.createImage();
	}

	/**
	 * Returns the image descriptor for the given editor descriptor, or null if it has no image.
	 */
	private ImageDescriptor getImageDescriptor(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = null;
		if (editorDesc == null) {
			imageDesc = registry.getImageDescriptor(node.getName());
			// TODO: is this case valid, and if so, what are the implications for content-simulator
			// editor bindings?
		}
		else {
			imageDesc = editorDesc.getImageDescriptor();
		}
		if (imageDesc == null) {
			if (editorDesc != null && editorDesc.getId().equals(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID)) {
				imageDesc = registry.getSystemExternalEditorImageDescriptor(node.getName());
			}
		}
		return imageDesc;
	}

	/**
	 * Creates the menu item for the editor descriptor.
	 *
	 * @param menu the menu to add the item to
	 * @param descriptor the editor descriptor, or null for the system editor
	 * @param preferredEditor the descriptor of the preferred editor, or <code>null</code>
	 */
	private void createMenuItem(Menu menu, final IEditorDescriptor descriptor, final IEditorDescriptor preferredEditor) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		boolean isPreferred = preferredEditor != null && descriptor.getId()
		                .equals(preferredEditor.getId());
		menuItem.setSelection(isPreferred);
		menuItem.setText(descriptor.getLabel());
		Image image = getImage(descriptor);
		if (image != null) {
			menuItem.setImage(image);
		}
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						syncOpen(descriptor, false);
					}
					break;
				default:
					break;
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	/**
	 * Creates the Other... menu item
	 *
	 * @param menu the menu to add the item to
	 */
	@SuppressWarnings("unused")
	private void createOtherMenuItem(final Menu menu) {
		new MenuItem(menu, SWT.SEPARATOR);
		final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(Messages.OpenWithMenu_OpenWith);
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					EditorSelectionDialog dialog = new EditorSelectionDialog(menu.getShell());
					dialog.setMessage(NLS
					                .bind(Messages.OpenWithMenu_ChooseEditorForOpening, node.getName()));
					if (dialog.open() == Window.OK) {
						IEditorDescriptor editor = dialog.getSelectedEditor();
						if (editor != null) {
							syncOpen(editor, editor.isOpenExternal());
						}
					}
					break;
				default:
					break;
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	/**
	 * Get the default editor for this IFSTreeNode.
	 *
	 * @return The descriptor of the default editor.
	 */
	private IEditorDescriptor getDefaultEditor() {
		// Try file specific editor.
		try {
			String editorID = node.getPreferredEditorID();
			if (editorID != null) {
				IEditorDescriptor desc = registry.findEditor(editorID);
				if (desc != null) {
					return desc;
				}
			}
		}
		catch (Exception e) {
			// do nothing
		}

		// Try lookup with filename
		return registry.getDefaultEditor(node.getName(), node.getContentType());
	}

	/*
	 * (non-Javadoc) Fills the menu with perspective items.
	 */
	@SuppressWarnings("unused")
	@Override
	public void fill(Menu menu, int index) {

		IEditorDescriptor defaultEditor = registry.findEditor(DEFAULT_TEXT_EDITOR);
		IEditorDescriptor preferredEditor = getDefaultEditor();

		IEditorDescriptor[] editors = registry.getEditors(node.getName(), node.getContentType());
		Collections.sort(Arrays.asList(editors), comparer);

		boolean defaultFound = false;

		// Check that we don't add it twice. This is possible
		// if the same editor goes to two mappings.
		ArrayList<IEditorDescriptor> alreadyMapped = new ArrayList<IEditorDescriptor>();

		for (int i = 0; i < editors.length; i++) {
			IEditorDescriptor editor = editors[i];
			if (!alreadyMapped.contains(editor)) {
				createMenuItem(menu, editor, preferredEditor);
				if (defaultEditor != null && editor.getId().equals(defaultEditor.getId())) {
					defaultFound = true;
				}
				alreadyMapped.add(editor);
			}
		}

		// Only add a separator if there is something to separate
		if (editors.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}

		// Add default editor. Check it if it is saved as the preference.
		if (!defaultFound && defaultEditor != null) {
			createMenuItem(menu, defaultEditor, preferredEditor);
		}

		// Add system editor (should never be null)
		IEditorDescriptor descriptor = registry
		                .findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		createMenuItem(menu, descriptor, preferredEditor);

		createDefaultMenuItem(menu);

		// add Other... menu item
		createOtherMenuItem(menu);
	}

	/*
	 * (non-Javadoc) Returns whether this menu is dynamic.
	 */
	@Override
	public boolean isDynamic() {
		return true;
	}

	/**
	 * Creates the menu item for clearing the current selection.
	 *
	 * @param menu the menu to add the item to
	 * @param file the file being edited
	 */
	private void createDefaultMenuItem(Menu menu) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setSelection(getDefaultEditor() == null);
		menuItem.setText(Messages.OpenWithMenu_DefaultEditor);

		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						node.setPreferredEditorID(null);
						try {
							syncOpen(getEditorDescriptor(), false);
						}
						catch (PartInitException e) {
							e.printStackTrace();
						}
					}
					break;
				default:
					break;
				}
			}
		};

		menuItem.addListener(SWT.Selection, listener);
	}

	/**
	 * Get an appropriate editor for the IFSTreeNode. If the default editor is not found, it will
	 * search the in-place editor, the external editor and finally the default text editor.
	 *
	 * @return An appropriate editor to open the node using "Default Editor".
	 * @throws PartInitException
	 */
	protected IEditorDescriptor getEditorDescriptor() throws PartInitException {
		IEditorDescriptor defaultDescriptor = getDefaultEditor();
		if (defaultDescriptor != null) {
			return defaultDescriptor;
		}

		IEditorDescriptor editorDesc = null;

		// next check the OS for in-place editor (OLE on Win32)
		if (registry.isSystemInPlaceEditorAvailable(node.getName())) {
			editorDesc = registry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		}

		// next check with the OS for an external editor
		if (editorDesc == null && registry.isSystemExternalEditorAvailable(node.getName())) {
			editorDesc = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		}

		// next lookup the default text editor
		if (editorDesc == null) {
			editorDesc = registry.findEditor(DEFAULT_TEXT_EDITOR);
		}

		// if no valid editor found, bail out
		if (editorDesc == null) {
			throw new PartInitException(Messages.OpenWithMenu_NoEditorFound);
		}

		return editorDesc;
	}

	/**
	 * Synchronize and open the file using the specified editor descriptor. If openUsingDescriptor
	 * is true, it will try to use an external editor to open it if an eclipse editor is not
	 * available.
	 *
	 * @param editorDescriptor The editor descriptor used to open the node.
	 * @param openUsingDescriptor If an external editor should be used to open the node.
	 */
	protected void syncOpen(IEditorDescriptor editorDescriptor, boolean openUsingDescriptor) {
		File file = node.getCacheFile();
		if (!file.exists() && !UiExecutor.execute(node.operationDownload(null)).isOK()) {
			return;
		}
		openInEditor(editorDescriptor, openUsingDescriptor);
	}

	/**
	 * Open the editor using the specified editor descriptor. If openUsingDescriptor is true, it
	 * will try to use an external editor to open it if an eclipse editor is not available.
	 *
	 * @param editorDescriptor The editor descriptor used to open the node.
	 * @param openUsingDescriptor If an external editor should be used to open the node.
	 */
	private void openInEditor(IEditorDescriptor editorDescriptor, boolean openUsingDescriptor) {
		try {
			IPath path = new Path(node.getCacheFile().getAbsolutePath());
			IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
			FileStoreEditorInput input = new FileStoreEditorInput(fileStore);
			if (openUsingDescriptor) {
				String editorId = editorDescriptor.getId();
				page.openEditor(input, editorId, true, IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
			}
			else {
				String editorId = IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID;
				if (editorDescriptor != null) editorId = editorDescriptor.getId();
				page.openEditor(input, editorId, true, IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
				node.setPreferredEditorID(editorId);
			}
		}
		catch (PartInitException e) {
			e.printStackTrace();
		}
	}
}
