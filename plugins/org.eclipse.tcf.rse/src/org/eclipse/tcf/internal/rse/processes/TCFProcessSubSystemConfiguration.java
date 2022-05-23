/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems       - initial API and implementation
 *     Uwe Stieber (Wind River) - [271227] Fix compiler warnings in org.eclipse.tcf.rse
 *     Intel Corporation        - [329654] Make all sub services operate against TCF connector service
 *******************************************************************************/
package org.eclipse.tcf.internal.rse.processes;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.services.processes.IProcessService;
import org.eclipse.rse.subsystems.processes.core.subsystem.IHostProcessToRemoteProcessAdapter;
import org.eclipse.rse.subsystems.processes.servicesubsystem.ProcessServiceSubSystem;
import org.eclipse.rse.subsystems.processes.servicesubsystem.ProcessServiceSubSystemConfiguration;
import org.eclipse.tcf.internal.rse.ITCFService;
import org.eclipse.tcf.internal.rse.TCFConnectorService;
import org.eclipse.tcf.internal.rse.TCFConnectorServiceManager;

public class TCFProcessSubSystemConfiguration extends ProcessServiceSubSystemConfiguration {

    private final TCFProcessAdapter process_adapter = new TCFProcessAdapter();

    @Override
    public Class<ITCFService> getServiceImplType() {
        return ITCFService.class;
    }

    @Override
    public ISubSystem createSubSystemInternal(IHost host) {
        TCFConnectorService connectorService = (TCFConnectorService)getConnectorService(host);
        return new ProcessServiceSubSystem(host, connectorService,
                getProcessService(host), getHostProcessAdapter());
    }

    public IProcessService createProcessService(IHost host) {
        return new TCFProcessService(host);
    }

    public IHostProcessToRemoteProcessAdapter getHostProcessAdapter() {
        return process_adapter;
    }

    @Override
    public IConnectorService getConnectorService(IHost host) {
        return TCFConnectorServiceManager.getInstance()
            .getConnectorService(host, getServiceImplType());
    }

    @Override
    public void setConnectorService(IHost host, IConnectorService connectorService) {
        TCFConnectorServiceManager.getInstance().setConnectorService(host, getServiceImplType(), connectorService);
    }
}
