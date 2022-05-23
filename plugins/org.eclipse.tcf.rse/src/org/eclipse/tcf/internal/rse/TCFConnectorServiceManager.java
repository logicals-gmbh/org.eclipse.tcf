/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Uwe Stieber (Wind River) - [271227] Fix compiler warnings in org.eclipse.tcf.rse
 *******************************************************************************/
package org.eclipse.tcf.internal.rse;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.AbstractConnectorServiceManager;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;

public class TCFConnectorServiceManager extends AbstractConnectorServiceManager {

    public static final int TCF_PORT = 1534;

    private static final TCFConnectorServiceManager manager =
        new TCFConnectorServiceManager();

    @Override
    public IConnectorService createConnectorService(IHost host) {
        return new TCFConnectorService(host, TCF_PORT);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getSubSystemCommonInterface(ISubSystem subsystem) {
        return ITCFSubSystem.class;
    }

    @Override
    public boolean sharesSystem(ISubSystem otherSubSystem) {
        return otherSubSystem instanceof ITCFSubSystem;
    }

    public static TCFConnectorServiceManager getInstance() {
        return manager;
    }
}
