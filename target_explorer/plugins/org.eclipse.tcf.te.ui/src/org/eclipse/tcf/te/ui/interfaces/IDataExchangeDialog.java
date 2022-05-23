/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.ui.interfaces;

import org.eclipse.jface.window.Window;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;

/**
 * IDataExchangeDialog
 */
public interface IDataExchangeDialog extends IDataExchangeNode {

	/**
	 * Open the dialog.
	 * @return @see {@link Window}
	 */
	public int open();
}
