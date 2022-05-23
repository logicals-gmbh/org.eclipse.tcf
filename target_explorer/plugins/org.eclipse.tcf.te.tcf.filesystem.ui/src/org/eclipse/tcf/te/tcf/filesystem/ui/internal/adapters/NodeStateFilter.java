/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.FsClipboard;
import org.eclipse.ui.IActionFilter;

/**
 * This action filter wraps an IFSTreeNode and test its attribute of "cache.state".
 * It serves as the expression filter of decorations of Target Explorer.
 */
public class NodeStateFilter implements IActionFilter {
	private IFSTreeNode node;

	/**
	 * Constructor.
	 *
	 * @param node
	 *            The wrapped tree node. Must not be <code>null</code>.
	 */
	public NodeStateFilter(IFSTreeNode node) {
		Assert.isNotNull(node);
		this.node = node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("cache.state") && node.isFile()) { //$NON-NLS-1$
			if(UIPlugin.isAutoSaving())
				return false;
			CacheState state = node.getCacheState();
			if (value == null)
				value = CacheState.consistent.name();
			return value.equals(state.name());
		} else if (name.equals("edit.cut")) { //$NON-NLS-1$
			FsClipboard cb = UIPlugin.getClipboard();
			if (!cb.isEmpty()) {
				if (cb.isCutOp()) {
					List<IFSTreeNode> files = cb.getFiles();
					for (IFSTreeNode file : files) {
						if (node == file) return true;
					}
				}
			}
		} else if (name.equals("hidden")) { //$NON-NLS-1$
			if (value == null) value = "true"; //$NON-NLS-1$
			boolean result = false;
			if (!node.isRootDirectory()) {
				if (node.isWindowsNode()) {
					result = node.isHidden();
				}
				else {
					result = node.getName().startsWith("."); //$NON-NLS-1$
				}
			}
			return Boolean.toString(result).equals(value);
		} else if (name.equals("isRevealOnConnect")) { //$NON-NLS-1$
			boolean expect = value == null || Boolean.parseBoolean(value);
			return expect == node.isRevealOnConnect();
		}
		return false;
	}
}
