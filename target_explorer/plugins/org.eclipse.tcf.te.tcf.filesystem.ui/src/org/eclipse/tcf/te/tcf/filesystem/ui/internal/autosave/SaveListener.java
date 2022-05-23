/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River) - [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.autosave;

import java.io.File;
import java.net.URI;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The execution listener of command "SAVE", which synchronizes the local file
 * with the one on the target server after it is saved.
 */
public class SaveListener implements IExecutionListener {
	// Dirty node that should be committed or merged.
	IFSTreeNode dirtyNode;

	/**
	 * Create a SaveListener listening to command "SAVE".
	 */
	public SaveListener() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
	 */
	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {
		if (dirtyNode != null) {
			if (UIPlugin.isAutoSaving()) {
				UiExecutor.execute(dirtyNode.operationUploadContent(dirtyNode.getCacheFile()));
			}
			else {
				SafeRunner.run(new SafeRunnable(){
					@Override
                    public void handleException(Throwable e) {
						// Ignore exception
                    }
					@Override
                    public void run() throws Exception {
						if (dirtyNode != null)
							dirtyNode.operationRefresh(false).runInJob(null);
                    }
				});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#preExecute(java.lang.String, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
		dirtyNode = null;
		IEditorInput input = HandlerUtil.getActiveEditorInput(event);
		if (input instanceof IURIEditorInput) {
			IURIEditorInput fileInput = (IURIEditorInput) input;
			URI uri = fileInput.getURI();
			try {
				IFileStore store = EFS.getStore(uri);
				File localFile = store.toLocalFile(0, new NullProgressMonitor());
				if (localFile != null) {
					IResultOperation<IFSTreeNode> parser = ModelManager.operationRestoreFromPath(localFile.getCanonicalPath());
					parser.run(null);
					dirtyNode = parser.getResult();
				}
			}catch(Exception e){
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#notHandled(java.lang.String, org.eclipse.core.commands.NotHandledException)
	 */
	@Override
	public void notHandled(String commandId, NotHandledException exception) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteFailure(java.lang.String, org.eclipse.core.commands.ExecutionException)
	 */
	@Override
	public void postExecuteFailure(String commandId, ExecutionException exception) {
	}
}
