/*******************************************************************************
 * Copyright (c) 2008, 2011 Anyware Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Anyware Technologies  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.internal.services.local.DiagnosticsService;
import org.eclipse.tcf.internal.services.remote.GenericProxy;
import org.eclipse.tcf.internal.services.remote.LocatorProxy;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IServiceProvider;
import org.eclipse.tcf.protocol.Protocol;

/**
 * ServiceManager class provides static methods used to handle ServiceProviders.
 * These methods are used by the Protocol class and the LocatorService Class.
 */
public class ServiceManager {

    private static final Collection<IServiceProvider> providers = new ArrayList<IServiceProvider>();

    static {
        addServiceProvider(new IServiceProvider() {

            private final String package_name = LocatorProxy.class.getPackage().getName();

            public IService[] getLocalService(IChannel channel) {
                return new IService[]{ new DiagnosticsService(channel) };
            }

            public IService getServiceProxy(IChannel channel, String service_name) {
                IService service = null;
                try {
                    Class<?> cls = Class.forName(package_name + "." + service_name + "Proxy");
                    service = (IService)cls.getConstructor(IChannel.class).newInstance(channel);
                    assert service_name.equals(service.getName());
                }
                catch (Exception x) {
                }
                return service;
            }
        });
    }

    /**
     * Get the ServiceManager ID
     * @return ServiceManagerID
     */
    public static String getID() {
        // In current implementation ServiceManager is a singleton,
        // so its ID is same as agent ID.
        return Protocol.getAgentID();
    }

    public static synchronized void addServiceProvider(IServiceProvider provider) {
        providers.add(provider);
    }

    public static synchronized void removeServiceProvider(IServiceProvider provider) {
        providers.remove(provider);
    }

    public static synchronized void onChannelCreated(IChannel channel, Map<String,IService> services) {
        IService zero_copy = new IService() {
            public String getName() {
                return "ZeroCopy";
            }
        };
        services.put(zero_copy.getName(), zero_copy);
        for (IServiceProvider provider : providers) {
            try {
                IService[] arr = provider.getLocalService(channel);
                if (arr == null) continue;
                for (IService service : arr) {
                    if (services.containsKey(service.getName())) continue;
                    services.put(service.getName(), service);
                }
            }
            catch (Throwable x) {
                Protocol.log("Error calling TCF service provider", x);
            }
        }
    }

    public static synchronized void onChannelOpened(IChannel channel, Collection<String> service_names, Map<String,IService> services) {
        for (String name : service_names) {
            for (IServiceProvider provider : providers) {
                try {
                    IService service = provider.getServiceProxy(channel, name);
                    if (service == null) continue;
                    services.put(name, service);
                    break;
                }
                catch (Throwable x) {
                    Protocol.log("Error calling TCF service provider", x);
                }
            }
            if (services.containsKey(name)) continue;
            services.put(name, new GenericProxy(channel, name));
        }
    }
}
