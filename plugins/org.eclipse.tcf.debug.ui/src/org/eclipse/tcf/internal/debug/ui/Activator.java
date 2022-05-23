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
package org.eclipse.tcf.internal.debug.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.internal.debug.ui.model.TCFAnnotationManager;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelManager;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.eclipse.tcf.debug.ui";

    private static final Object lock = new Object();

    private static Activator plugin;
    private static TCFModelManager model_manager;
    private static TCFAnnotationManager annotation_manager;

    public void start(BundleContext context) throws Exception {
        synchronized (lock) {
            super.start(context);
            plugin = this;
            Protocol.invokeLater(new Runnable() {
                public void run() {
                    if (model_manager == null) {
                        model_manager = new TCFModelManager();
                    }
                }
            });
        }
    }

    public void stop(BundleContext context) throws Exception {
        Protocol.invokeAndWait(new Runnable() {
            public void run() {
                if (model_manager != null) {
                    model_manager.dispose();
                    model_manager = null;
                }
                if (annotation_manager != null) {
                    annotation_manager.dispose();
                    annotation_manager = null;
                }
            }
        });
        synchronized (lock) {
            plugin = null;
            super.stop(context);
        }
    }

    /**
     * Returns the shared instance
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns the shared TCFModelManager instance
     * @return the shared TCFModelManager instance
     */
    public static TCFModelManager getModelManager() {
        assert Protocol.isDispatchThread();
        if (model_manager == null) model_manager = new TCFModelManager();
        return model_manager;
    }

    /**
     * Returns the shared TCFAnnotationManager instance
     * @return the shared TCFAnnotationManager instance
     */
    public static TCFAnnotationManager getAnnotationManager() {
        assert Protocol.isDispatchThread();
        if (annotation_manager == null) annotation_manager = new TCFAnnotationManager();
        return annotation_manager;
    }

    /**
     * Send error message into Eclipse log.
     * @param msg - error message test
     * @param err - exception
     */
    public static void log(String msg, Throwable err) {
        synchronized (lock) {
            if (plugin == null || plugin.getLog() == null) {
                err.printStackTrace();
            }
            else {
                plugin.getLog().log(new Status(IStatus.ERROR,
                        plugin.getBundle().getSymbolicName(), IStatus.OK, msg, err));
            }
        }
    }

    /**
     * Send error message into Eclipse log.
     * @param err - exception
     */
    public static void log(Throwable err) {
        synchronized (lock) {
            if (plugin == null || plugin.getLog() == null) {
                err.printStackTrace();
            }
            else {
                plugin.getLog().log(new Status(IStatus.ERROR,
                        plugin.getBundle().getSymbolicName(), IStatus.OK, "Unhandled exception in TCF UI", err));
            }
        }
    }
}
