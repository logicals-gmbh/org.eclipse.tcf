/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.views.scriptpad.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.views.scriptpad.ScriptPad;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.IViewPart;

/**
 * Peer toggle action implementation.
 */
public class PeerAction extends Action {
	// Static reference to a label provider delegate providing the action label and image
	private final static DelegatingLabelProvider delegate = new DelegatingLabelProvider();

	// Reference to the peer model
	private IPeerNode peerNode;
	// Reference to the parent view part
	private IViewPart view;

	/**
     * Constructor.
     *
     * @param view The parent view part. Must not be <code>null</code>.
     * @param peerNode The peer model. Must not be <code>null</code>.
     */
    public PeerAction(IViewPart view, IPeerNode peerNode) {
    	super("", AS_CHECK_BOX); //$NON-NLS-1$

    	Assert.isNotNull(peerNode);
    	this.peerNode = peerNode;

    	String label = delegate.getText(peerNode);
    	if (label != null) {
    		setText(delegate.decorateText(label, peerNode));
    	}

    	Assert.isNotNull(view);
    	this.view = view;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
    	setChecked(true);

    	// Pass on the selected peer model to the parent view
    	if (view instanceof ScriptPad) {
    		((ScriptPad)view).setPeerModel(peerNode);
    	}
    }
}
