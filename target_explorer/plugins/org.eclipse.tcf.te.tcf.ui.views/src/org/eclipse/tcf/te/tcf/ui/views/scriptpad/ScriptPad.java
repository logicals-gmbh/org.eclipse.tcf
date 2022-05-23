/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.views.scriptpad;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.views.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.views.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions.CopyAction;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions.CutAction;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions.DeleteAction;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions.PasteAction;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions.PeerAction;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions.SelectAllAction;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

/**
 * Script Pad view implementation.
 */
public class ScriptPad extends ViewPart implements ISelectionProvider, SelectionListener, ISaveablePart {
	// Reference to the header line
	private Label head;
	// Reference to the Text widget
	/* default */ StyledText text;
	// Reference to the line style listener
	private ScriptPadLineStyleListener lineStyleListener;

	// The list of registered selection changed listeners
	private final List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

	// Reference to the selected peer model
	/* default */ IPeerNode peerNode;

	// References to the global action handlers
	private CutAction cutHandler;
	private CopyAction copyHandler;
	private PasteAction pasteHandler;
	/* default */ DeleteAction deleteHandler;
	private SelectAllAction selectAllHandler;

	// If the user loaded a script either via the "Open" action or DnD, remember the file name
	// so the user can save it back.
	private String fileLoaded = null;

	// The dirty state
	private boolean dirty = false;

	/**
     * Constructor.
     */
    public ScriptPad() {
    	super();
    	listeners.clear();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
    	if (text != null && !text.isDisposed()) {
    		text.removeSelectionListener(this);
    		if (lineStyleListener != null) {
    			text.removeLineStyleListener(lineStyleListener);
    			lineStyleListener.dispose();
    			lineStyleListener = null;
    		}
    	}
        listeners.clear();
        fileLoaded = null;
        super.dispose();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Create the head label
		head = new Label(panel, SWT.HORIZONTAL);
		head.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Create the StyledText widget
		text = new StyledText(panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.setFont(JFaceResources.getTextFont());
		text.addSelectionListener(this);
		text.addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				if (deleteHandler != null) deleteHandler.updateEnabledState();
			}
		});
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isDirty()) markDirty(true);
			}
		});

		lineStyleListener = new ScriptPadLineStyleListener();
		text.addLineStyleListener(lineStyleListener);

		// Register ourselves as selection provider
		getViewSite().setSelectionProvider(this);

		// Create the context menu
		createContextMenu();
		// Create the toolbar
		createToolbar();

		// Hook the global actions
		hookGlobalActions();

		// Setup Drag n Drop
		setupDnD();

		// Update the action bars
		getViewSite().getActionBars().updateActionBars();
	}

	/**
	 * Creates the views context menu and register it to enable contributions.
	 */
	private void createContextMenu() {
		// Create menu manager.
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
            public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});

		// Create menu.
		Menu menu = menuMgr.createContextMenu(text);
		text.setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, this);
	}

	/**
	 * Fill the context menu.
	 *
	 * @param manager The menu manager. Must not be <code>null</code>.
	 */
	protected void fillContextMenu(IMenuManager manager) {
		Assert.isNotNull(manager);
	}

	/**
	 * Hook the global actions.
	 */
	protected void hookGlobalActions() {
		IActionBars actionBars = getViewSite().getActionBars();

		cutHandler = new CutAction(this);
		actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutHandler);

		copyHandler = new CopyAction(this);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyHandler);

		pasteHandler = new PasteAction(this);
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteHandler);

		deleteHandler = new DeleteAction(this);
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteHandler);

		selectAllHandler = new SelectAllAction(this);
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllHandler);

		updateActionEnablements();
	}

	/**
	 * Creates the views toolbar.
	 */
	private void createToolbar() {
		IToolBarManager manager = getViewSite().getActionBars().getToolBarManager();
		manager.add(new Separator());
		manager.add(new GroupMarker("open")); //$NON-NLS-1$
		manager.add(new GroupMarker("save")); //$NON-NLS-1$
		manager.add(new Separator());
		manager.add(new GroupMarker("clear")); //$NON-NLS-1$
		manager.add(new Separator());
		manager.add(new GroupMarker("play")); //$NON-NLS-1$
		manager.add(new Separator());
		manager.add(new GroupMarker("peers")); //$NON-NLS-1$
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * Setup Drag n Drop support.
	 */
	private void setupDnD() {
		DropTarget target = new DropTarget(text, DND.DROP_DEFAULT | DND.DROP_COPY);
		target.setTransfer(new Transfer[] { TextTransfer.getInstance(), FileTransfer.getInstance() });
		target.addDropListener(new DropTargetAdapter() {

			@Override
			public void drop(DropTargetEvent event) {
				if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
					String[] names = (String[]) FileTransfer.getInstance().nativeToJava(event.currentDataType);
					if (names != null && names.length > 0) {
						openFile(names[0]);
					}
				}
				else if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
					String content = (String) TextTransfer.getInstance().nativeToJava(event.currentDataType);
					if (content != null && !"".equals(content)) { //$NON-NLS-1$
						if (text != null && !text.isDisposed()) text.setText(content);
					}
				}
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
		        if (event.detail == DND.DROP_DEFAULT) event.detail = DND.DROP_COPY;
			}

			@Override
			public void dragEnter(DropTargetEvent event) {
		        if (event.detail == DND.DROP_DEFAULT) event.detail = DND.DROP_COPY;
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		if (text != null) text.setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	@Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
		Assert.isNotNull(listener);
		if (!listeners.contains(listener)) listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	@Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		Assert.isNotNull(listener);
		listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		String selected = text.getSelectionText();
	    return selected != null ? new StructuredSelection(selected) : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void setSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof String) {
				int start = text.getText().indexOf((String)element);
				if (start != -1) {
					text.setSelection(start, start + ((String)element).length());
				}
			}
		} else {
			text.setSelection(text.getCaretOffset(), text.getCaretOffset());
		}

		// Fire the selection changed event
		fireSelectionChanged();
		updateActionEnablements();
	}

	/**
	 * Notify the registered selection changed listener about a changed selection.
	 */
	private void fireSelectionChanged() {
		if (getSelection() == null) return;

		SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
		Iterator<ISelectionChangedListener> iterator = listeners.iterator();
		while (iterator.hasNext()) {
			ISelectionChangedListener listener = iterator.next();
			listener.selectionChanged(event);
		}
	}

	/**
	 * Update the action enablements
	 */
	public void updateActionEnablements() {
		if (cutHandler != null) cutHandler.updateEnabledState();
		if (copyHandler != null) copyHandler.updateEnabledState();
		if (pasteHandler != null) pasteHandler.updateEnabledState();
		if (deleteHandler != null) deleteHandler.updateEnabledState();
		if (selectAllHandler != null) selectAllHandler.updateEnabledState();
	}

	/**
	 * Update the head label
	 */
	protected void updateHeadLabel() {
		StringBuilder label = new StringBuilder();

		if (fileLoaded == null) {
			label.append("<no file>"); //$NON-NLS-1$
		} else {
			IPath path = new Path(fileLoaded);
			label.append(path.lastSegment());
		}

		label.append(" - "); //$NON-NLS-1$

		if (peerNode == null) {
			label.append("<no peer>"); //$NON-NLS-1$
		} else {
			PeerAction action = new PeerAction(this, peerNode);
			label.append(action.getText());
		}

		SWTControlUtil.setText(head, label.toString());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		fireSelectionChanged();
		updateActionEnablements();
	}

	/**
	 * Sets the peer model.
	 *
	 * @param peerNode The peer model or <code>null</code>.
	 */
	public void setPeerModel(IPeerNode peerNode) {
		this.peerNode = peerNode;
		// Update the head label
		updateHeadLabel();
		// Update the action bars
		getViewSite().getActionBars().updateActionBars();
		// Fire a property change (in the UI Thread)
		DisplayUtil.safeAsyncExec(new Runnable() {
	        @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
	    		firePropertyChange(IWorkbenchPartConstants.PROP_INPUT);
	        }
        });
	}

	/**
	 * Returns the associated peer model.
	 *
	 * @return The associated peer model.
	 */
	public IPeerNode getPeerModel() {
		return peerNode;
	}

	/**
	 * Returns the styled text widget.
	 *
	 * @return The styled text widget or <code>null</code>.
	 */
	public StyledText getStyledText() {
		return text;
	}

	/**
	 * Sets the dirty state.
	 *
	 * @param dirty <code>True</code> to mark the view dirty, <code>false</code> otherwise.
	 */
	public void markDirty(boolean dirty) {
		if (this.dirty != dirty) {
			this.dirty = dirty;
			firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	@Override
    public boolean isDirty() {
		return dirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		if (fileLoaded != null) saveFile(fileLoaded);
		else doSaveAs();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.tcf", "*" }); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterPath(fileLoaded != null ? fileLoaded : System.getProperty("user.home")); //$NON-NLS-1$
		String file = dialog.open();
		if (file != null) {
			saveFile(file);
			// Save succeeded ?
			if (!isDirty()) {
				fileLoaded = file;
				updateHeadLabel();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	@Override
	public boolean isSaveOnCloseNeeded() {
	    return isDirty();
	}

	/**
	 * Clears all content and reset the loaded file path.
	 */
	public void clear() {
		this.fileLoaded = null;
		updateHeadLabel();
		if (text != null && !text.isDisposed()) text.setText(""); //$NON-NLS-1$
	}

	/**
	 * Opens the file.
	 *
	 * @param file The absolute file name of the script to load. Must not be <code>null</code>.
	 */
	public void openFile(String file) {
		Assert.isNotNull(file);

		// The file name must be absolute and denote a readable file
		File f = new File(file);
		if (f.isAbsolute() && f.isFile() && f.canRead()) {
			// Remember the file name
			this.fileLoaded = file;
			updateHeadLabel();
			// Clear out the old text
			text.setText(""); //$NON-NLS-1$

			FileReader reader = null;
			try {
				reader = new FileReader(f);
				StringBuilder buffer = new StringBuilder();
				int c;
				while ((c = reader.read()) != -1) {
					buffer.append((char)c);
				}
				text.setText(buffer.toString());
				markDirty(false);
			} catch (IOException e) {
				String template = NLS.bind(Messages.ScriptPad_error_openFile, file, Messages.ScriptPad_error_possibleCause);
				StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), this, template,
												Messages.ScriptPad_error_title, IContextHelpIds.SCRIPT_PAD_ERROR_OPEN_FILE, this, null);
			} finally {
				if (reader != null) try { reader.close(); } catch (IOException e) { /* ignored on purpose */ }
			}
		}
		updateActionEnablements();
	}

	/**
	 * Saves the file.
	 *
	 * @param file The absolute file name to save the script to. Must not be <code>null</code>.
	 */
	public void saveFile(String file) {
		Assert.isNotNull(file);

		// The file name must be absolute and denote a writable file
		File f = new File(file);
		if (f.isAbsolute() && ((f.exists() && f.isFile() && f.canWrite() || !f.exists()))) {
			String content = text.getText();

			FileWriter writer = null;
			try {
				writer = new FileWriter(f);
				writer.write(content);
				markDirty(false);
			} catch (Exception e) {
				String template = NLS.bind(Messages.ScriptPad_error_saveFile, file, Messages.ScriptPad_error_possibleCause);
				StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), this, template,
												Messages.ScriptPad_error_title, IContextHelpIds.SCRIPT_PAD_ERROR_SAVE_FILE, this, null);
			} finally {
				if (writer != null) try { writer.close(); } catch (IOException e) { /* ignored on purpose */ }
			}
		}
		updateActionEnablements();
	}
}
