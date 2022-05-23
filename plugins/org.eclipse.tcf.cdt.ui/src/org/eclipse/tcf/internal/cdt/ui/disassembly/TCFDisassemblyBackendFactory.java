/*******************************************************************************
 * Copyright (c) 2010, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.disassembly;

import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.IDisassemblyBackend;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;

@SuppressWarnings({"restriction", "rawtypes"})
public class TCFDisassemblyBackendFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = { IDisassemblyBackend.class };

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adaptableObject instanceof TCFNode) {
            TCFDisassemblyBackend backend = new TCFDisassemblyBackend();
            if (backend.supportsDebugContext((TCFNode)adaptableObject)) {
                return backend;
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return CLASSES;
    }
}
