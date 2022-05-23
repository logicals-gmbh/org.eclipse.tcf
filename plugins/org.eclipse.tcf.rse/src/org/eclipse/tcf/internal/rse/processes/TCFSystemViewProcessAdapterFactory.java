/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.rse.processes;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.rse.core.subsystems.IRemoteObjectIdentifier;
import org.eclipse.rse.core.subsystems.ISystemDragDropAdapter;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

public class TCFSystemViewProcessAdapterFactory implements IAdapterFactory {

    private final TCFSystemViewRemoteProcessAdapter adapter =
        new TCFSystemViewRemoteProcessAdapter();

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        assert adaptableObject instanceof TCFRemoteProcess;
        if (adapterType == IPropertySource.class) {
            ((ISystemViewElementAdapter)adapter).setPropertySourceInput(adaptableObject);
        }
        return adapter;
    }

    @SuppressWarnings("rawtypes")
    public Class[] getAdapterList() {
        return new Class[] {
            ISystemViewElementAdapter.class,
            ISystemDragDropAdapter.class,
            ISystemRemoteElementAdapter.class,
            IPropertySource.class,
            IWorkbenchAdapter.class,
            IActionFilter.class,
            IDeferredWorkbenchAdapter.class,
            IRemoteObjectIdentifier.class,
        };
    }
}
