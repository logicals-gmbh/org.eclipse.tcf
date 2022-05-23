/*******************************************************************************
 * Copyright (c) 2007, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.tcf.internal.debug.ui.commands.BreakpointCommand;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.ui.views.properties.IPropertySource;


public class TCFNodeAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] adapter_list = {
        IToggleBreakpointsTarget.class,
        IToggleBreakpointsTargetExtension.class,
        IPropertySource.class,
    };

    private final BreakpointCommand breakpoint_command = new BreakpointCommand();

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Object obj, Class cls) {
        if (obj instanceof TCFNode) {
            if (cls == IToggleBreakpointsTarget.class) return breakpoint_command;
            if (cls == IToggleBreakpointsTargetExtension.class) return breakpoint_command;
            if (cls == IPropertySource.class) return new TCFNodePropertySource((TCFNode)obj);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Class[] getAdapterList() {
        return adapter_list;
    }
}
