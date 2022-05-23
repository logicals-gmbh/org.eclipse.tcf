/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River)- [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.compare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.compare.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * A <code>LocalTypedElement</code> extends <code>MergeTypedElement</code> and
 * wraps an <code>IFSTreeNode</code> so that it can be used as the left element
 * of a <code>MergeEditorInput</code>. It implements the interface
 * <code>IEditableContent</code> so that it is editable.
 */
public class LocalTypedElement extends MergeTypedElement implements
		IEditableContent, IAdaptable, ISharedDocumentAdapterListener {
	// If the current edited file is dirty.
	private boolean dirty;
	// The shared document adapter
	private EditableSharedDocumentAdapter documentAdapter;
	// The shared document listener.
	private ISharedDocumentAdapterListener documentListener;

	/**
	 * Creates a <code>LocalTypedElement</code> for the given resource.
	 *
	 */
	public LocalTypedElement(IFSTreeNode node) {
		super(node);
		setContent(getContent());
		dirty = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			if (documentAdapter == null)
				documentAdapter = new EditableSharedDocumentAdapter(this);
			return documentAdapter;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.BufferedContent#setContent(byte[])
	 */
	@Override
	public void setContent(byte[] contents) {
		dirty = true;
		super.setContent(contents);
	}

	/**
	 * Set the document listener.
	 *
	 * @param documentListener
	 *            the document listener.
	 */
	public void setDocumentListener(
			ISharedDocumentAdapterListener documentListener) {
		this.documentListener = documentListener;
	}

	/**
	 * Return an input stream that opens that locally cached file to provide the
	 * content.
	 *
	 * @return a buffered input stream containing the contents of this file
	 * @exception CoreException
	 *                if the contents of this storage could not be accessed
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		try {
			File cacheFile = node.getCacheFile();
			return new BufferedInputStream(new FileInputStream(cacheFile));
		} catch (FileNotFoundException e) {
			IStatus error = new Status(IStatus.ERROR,
					UIPlugin.getUniqueIdentifier(), e.getMessage(), e);
			throw new CoreException(error);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContent#isEditable()
	 */
	@Override
	public boolean isEditable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContent#replace(org.eclipse.compare.ITypedElement, org.eclipse.compare.ITypedElement)
	 */
	@Override
	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		return dest;
	}

	/**
	 * Save the shared document for this element. The save can only be performed
	 * if the element is connected to a shared document. If the element is not
	 * connected, <code>false</code> is returned.
	 *
	 * @param monitor
	 *            a progress monitor
	 * @throws CoreException
	 */
	public boolean store2Document(IProgressMonitor monitor)
			throws CoreException {
		if (isConnected()) {
			IEditorInput input = documentAdapter.getDocumentKey(this);
			documentAdapter.saveDocument(input, monitor);
			return true;
		}
		return false;
	}

	/**
	 * Judges whether the content has been changed.
	 *
	 * @return
	 */
	public boolean isDirty() {
		return dirty
				|| (documentAdapter != null && documentAdapter
						.hasBufferedContents());
	}

	/**
	 * Set the dirty action.
	 *
	 * @param dirty
	 *            The dirty action.
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * If the document adapter has been connected.
	 *
	 * @return true if it is not null and connected.
	 */
	public boolean isConnected() {
		return documentAdapter != null && documentAdapter.isConnected();
	}

	/**
	 * Return the path to the local file of this node. It is used to compute its
	 * hash code and as the title of the comparison editor.
	 */
	@Override
	public String toString() {
		File cacheFile = node.getCacheFile();
		return cacheFile.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.compare.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener#handleDocumentConnected()
	 */
	@Override
	public void handleDocumentConnected() {
		if (documentListener != null)
			documentListener.handleDocumentConnected();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.compare.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener#handleDocumentDeleted()
	 */
	@Override
	public void handleDocumentDeleted() {
		if (documentListener != null)
			documentListener.handleDocumentDeleted();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.compare.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener#handleDocumentDisconnected()
	 */
	@Override
	public void handleDocumentDisconnected() {
		if (documentListener != null)
			documentListener.handleDocumentDisconnected();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.compare.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener#handleDocumentFlushed()
	 */
	@Override
	public void handleDocumentFlushed() {
		fireContentChanged();
		if (documentListener != null)
			documentListener.handleDocumentFlushed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.compare.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener#handleDocumentSaved()
	 */
	@Override
	public void handleDocumentSaved() {
		if (documentListener != null)
			documentListener.handleDocumentSaved();
	}

	/**
	 * Get an editor input for this file.
	 *
	 * @return The editor input.
	 */
	public IEditorInput getEditorInput() {
		IPath path = new Path(node.getCacheFile().getAbsolutePath());
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
		return new FileStoreEditorInput(fileStore);
	}

	/**
	 * Save to its local file when the document has not been connected yet.
	 *
	 * @param monitor
	 *            The monitor that reports the progress.
	 */
	public void store2Cache(IProgressMonitor monitor) throws CoreException {
		File cacheFile = node.getCacheFile();
		monitor.beginTask(NLS.bind(Messages.LocalTypedElement_SavingFile, cacheFile.getName()), 100);
		InputStream is = getContents();
		BufferedOutputStream bos = null;
		try {
			long total = cacheFile.length();
			bos = new BufferedOutputStream(new FileOutputStream(cacheFile));
			byte[] data = new byte[10 * 1024];
			int length;
			long current = 0;
			int currProgress = 0;
			while ((length = is.read(data)) > 0) {
				bos.write(data, 0, length);
				bos.flush();
				current += length;
				int progress = (int) (current * 100 / total);
				if (currProgress != progress) {
					monitor.worked(progress - currProgress);
					currProgress = progress;
				}
			}
			setDirty(false);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					UIPlugin.getUniqueIdentifier(), e.getMessage(), e));
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException ex) {
				}
			if (bos != null) {
				try {
					bos.close();
				} catch (Exception e) {
				}
			}
			// Notify the local file element that the document has changed.
			fireContentChanged();
		}
	}
}
