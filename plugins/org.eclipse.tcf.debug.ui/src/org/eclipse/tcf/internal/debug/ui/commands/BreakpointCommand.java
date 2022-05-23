/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.internal.debug.model.TCFBreakpoint;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IWorkbenchPart;


public class BreakpointCommand implements IToggleBreakpointsTargetExtension {

    public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection) {
        if (selection.isEmpty()) return false;
        final Object obj = ((IStructuredSelection)selection).getFirstElement();
        if (!(obj instanceof TCFNode)) return false;
        TCFNode node = (TCFNode)obj;
        try {
            return new TCFTask<Boolean>(node.getChannel()) {
                public void run() {
                    TCFDataCache<BigInteger> addr_cache = null;
                    if (obj instanceof TCFNodeExecContext) addr_cache = ((TCFNodeExecContext)obj).getAddress();
                    if (obj instanceof TCFNodeStackFrame) addr_cache = ((TCFNodeStackFrame)obj).getAddress();
                    if (addr_cache != null) {
                        if (!addr_cache.validate(this)) return;
                        done(addr_cache.getData() != null);
                    }
                    else {
                        done(false);
                    }
                }
            }.getE();
        }
        catch (Error e) {
            if (node.isDisposed()) return false;
            throw e;
        }
    }

    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) {
        if (selection.isEmpty()) return;
        final Object obj = ((IStructuredSelection)selection).getFirstElement();
        if (!(obj instanceof TCFNode)) return;
        new TCFTask<Object>(((TCFNode)obj).getChannel()) {
            public void run() {
                TCFDataCache<BigInteger> addr_cache = null;
                if (obj instanceof TCFNodeExecContext) addr_cache = ((TCFNodeExecContext)obj).getAddress();
                if (obj instanceof TCFNodeStackFrame) addr_cache = ((TCFNodeStackFrame)obj).getAddress();
                if (addr_cache != null) {
                    if (!addr_cache.validate(this)) return;
                    BigInteger addr = addr_cache.getData();
                    if (addr != null) {
                        Map<String,Object> m = new HashMap<String,Object>();
                        m.put(IBreakpoints.PROP_ENABLED, Boolean.TRUE);
                        m.put(IBreakpoints.PROP_LOCATION, addr.toString());
                        TCFBreakpoint.createFromTCFProperties(m);
                    }
                }
                done(null);
            }
        }.getE();
    }

    public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
        return false;
    }

    public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        // TODO: breakpoint command: toggle line breakpoint
    }

    public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
        return false;
    }

    public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
    }

    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
        return false;
    }

    public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        // TODO: breakpoint command: toggle watchpoint
    }
}
