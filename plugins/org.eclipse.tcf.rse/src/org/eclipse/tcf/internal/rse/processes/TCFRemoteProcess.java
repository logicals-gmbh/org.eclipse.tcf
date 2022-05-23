/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Uwe Stieber (Wind River) - [271227] Fix compiler warnings in org.eclipse.tcf.rse
 *******************************************************************************/
package org.eclipse.tcf.internal.rse.processes;

import java.util.Map;

import org.eclipse.rse.services.clientserver.processes.IHostProcess;
import org.eclipse.rse.subsystems.processes.core.subsystem.IRemoteProcessContext;
import org.eclipse.rse.subsystems.processes.core.subsystem.impl.RemoteProcessImpl;

public class TCFRemoteProcess extends RemoteProcessImpl {

    public TCFRemoteProcess(IRemoteProcessContext context, IHostProcess process) {
        super(context, process);
        assert process != null;
    }

    @Override
    public Object getObject() {
        return _underlyingProcess;
    }

    public Map<String,Object> getProperties() {
        return ((TCFProcessResource)_underlyingProcess).getProperties();
    }
}
