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
package org.eclipse.tcf;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.core.ChannelTCP;
import org.eclipse.tcf.internal.nls.TcfPluginMessages;
import org.eclipse.tcf.protocol.ILogger;
import org.eclipse.tcf.protocol.IServiceProvider;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.ssl.TCFSecurityManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

    public static final String PLUGIN_ID = "org.eclipse.tcf"; //$NON-NLS-1$

    private static Activator plugin;
    private static boolean debug;
    private static final EventQueue queue = new EventQueue();
    private static final BundleListener bundle_listener = new BundleListener() {
        private boolean started = false;
        public void bundleChanged(BundleEvent event) {
            if (plugin != null && !started &&
                    event.getBundle() == plugin.getBundle() &&
                    plugin.getBundle().getState() == Bundle.ACTIVE) {
                queue.start();
                started = true;
            }
        }
    };

    /** Eclipse tracing option, plug-in wide */
    private static boolean TRACE;

    /**
     * Constructor.
     */
    public Activator() {
        plugin = this;
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        debug = Platform.inDebugMode();

        TRACE = "true".equals(Platform.getDebugOption("org.eclipse.tcf/debug")); //$NON-NLS-1$
        if (TRACE && "true".equals(Platform.getDebugOption("org.eclipse.tcf/debug/discovery"))) {
            System.setProperty("org.eclipse.tcf.core.tracing.discovery", "true");
        }
        if (TRACE && "true".equals(Platform.getDebugOption("org.eclipse.tcf/debug/channel"))) {
            System.setProperty("org.eclipse.tcf.core.tracing.channel", "true");
        }

        ChannelTCP.setSSLContext(TCFSecurityManager.createSSLContext());
        Protocol.setLogger(new ILogger() {

            public void log(final String msg, final Throwable x) {
                // Normally, we hook the TCF logging service (ILogger) to the
                // Plug-in logger. Trace hooks in the code use the TCF logger.
                // The Plug-in logger isn't really designed for large amounts of
                // trace data, though, so redirect to stdout when tracing is
                // enabled.
                if (TRACE) {
                    System.out.println(msg);
                    if (x != null) x.printStackTrace();
                }
                else {
                    if (debug) {
                        System.err.println(msg);
                        if (x != null) x.printStackTrace();
                    }
                    if (plugin != null) {
                        final ILog logger = getLog();
                        if (logger != null) {
                            // Do not call logger on TCF thread - it can cause deadlock,
                            // because Eclipse log listeners (e.g. IDEWorkbenchErrorHandler)
                            // can call Display.syncExec(), which is not allowed
                            // on the TCF dispatch thread.
                            Job job = new Job("TCF Log") {
                                @Override
                                protected IStatus run(IProgressMonitor monitor) {
                                    logger.log(new Status(IStatus.ERROR,
                                            getBundle().getSymbolicName(), IStatus.OK, msg, x));
                                    return Status.OK_STATUS;
                                }
                            };
                            job.setPriority(Job.SHORT);
                            job.setSystem(true);
                            job.schedule();
                        }
                    }
                }
            }
        });
        /*
         * Starts the timer_queue and sets the event_queue
         */
        Protocol.setEventQueue(queue);
        Protocol.invokeLater(new Runnable() {
            public void run() {
                runTCFStartup();
            }
        });
        context.addBundleListener(bundle_listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(bundle_listener);
        queue.shutdown();
        plugin = null;
        super.stop(context);
    }

    private void runTCFStartup() {
        try {
            IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, "startup"); //$NON-NLS-1$
            IExtension[] extensions = point.getExtensions();
            for (int i = 0; i < extensions.length; i++) {
                try {
                    Bundle bundle = Platform.getBundle(extensions[i].getNamespaceIdentifier());
                    bundle.start(Bundle.START_TRANSIENT);
                    IConfigurationElement[] e = extensions[i].getConfigurationElements();
                    for (int j = 0; j < e.length; j++) {
                        String nm = e[j].getName();
                        if (nm.equals("class")) { //$NON-NLS-1$
                            Class<?> c = bundle.loadClass(e[j].getAttribute("name")); //$NON-NLS-1$
                            Class.forName(c.getName(), true, c.getClassLoader());
                        }
                    }
                }
                catch (Throwable x) {
                    Protocol.log("TCF startup error", x); //$NON-NLS-1$
                }
            }
        }
        catch (Exception x) {
            Protocol.log("TCF startup error", x); //$NON-NLS-1$
        }

        // Register service providers contributed via Eclipse extension point
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.eclipse.tcf.serviceProviders"); //$NON-NLS-1$
        if (point != null) {
            IExtension[] extensions = point.getExtensions();
            for (IExtension extension : extensions) {
                IConfigurationElement[] elements = extension.getConfigurationElements();
                for (IConfigurationElement element : elements) {
                    if ("serviceProvider".equals(element.getName())) { //$NON-NLS-1$
                        try {
                            // Create the service provider instance
                            IServiceProvider provider = (IServiceProvider)element.createExecutableExtension("class"); //$NON-NLS-1$
                            if (provider != null) Protocol.addServiceProvider(provider);
                        } catch (CoreException e) {
                            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                                        NLS.bind(TcfPluginMessages.Extension_error_invalidExtensionPoint, element.getDeclaringExtension().getUniqueIdentifier()),
                                                        e);
                            Activator.getDefault().getLog().log(status);
                        }
                    }
                }
            }
        }
    }
}
