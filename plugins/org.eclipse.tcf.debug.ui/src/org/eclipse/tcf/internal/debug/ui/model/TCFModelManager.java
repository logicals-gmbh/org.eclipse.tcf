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
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;

/**
 * TCFModelManager listens debug launch manager and creates TCF debug models when necessary.
 */
public class TCFModelManager {

    private static final Map<TCFLaunch,TCFModel> sync_model_map =
            Collections.synchronizedMap(new HashMap<TCFLaunch,TCFModel>());

    public interface ModelManagerListener {
        public void onConnected(TCFLaunch launch, TCFModel model);
        public void onDisconnected(TCFLaunch launch, TCFModel model);
    }

    private final Map<TCFLaunch,TCFModel> models = new HashMap<TCFLaunch,TCFModel>();
    private final List<ModelManagerListener> listeners = new ArrayList<ModelManagerListener>();

    private final TCFLaunch.LaunchListener tcf_launch_listener = new TCFLaunch.LaunchListener() {

        public void onCreated(TCFLaunch launch) {
            assert Protocol.isDispatchThread();
            assert models.get(launch) == null;
            TCFModel model = new TCFModel(launch);
            models.put(launch, model);
            sync_model_map.put(launch, model);
        }

        public void onConnected(TCFLaunch launch) {
            assert Protocol.isDispatchThread();
            TCFModel model = models.get(launch);
            if (model != null) model.onConnected();
            for (ModelManagerListener l : listeners) {
                try {
                    l.onConnected(launch, model);
                }
                catch (Throwable x) {
                    Activator.log(x);
                }
            }
        }

        public void onDisconnected(TCFLaunch launch) {
            assert Protocol.isDispatchThread();
            TCFModel model = models.get(launch);
            if (model != null) model.onDisconnected();
            for (ModelManagerListener l : listeners) {
                try {
                    l.onDisconnected(launch, model);
                }
                catch (Throwable x) {
                    Activator.log(x);
                }
            }
        }

        public void onProcessOutput(TCFLaunch launch, String process_id, int stream_id, byte[] data) {
            assert Protocol.isDispatchThread();
            TCFModel model = models.get(launch);
            if (model != null) model.onProcessOutput(process_id, stream_id, data);
        }

        public void onProcessStreamError(TCFLaunch launch, String process_id,
                int stream_id, Exception error, int lost_size) {
            assert Protocol.isDispatchThread();
            TCFModel model = models.get(launch);
            if (model != null) model.onProcessStreamError(process_id, stream_id, error, lost_size);
        }
    };

    private final ILaunchesListener debug_launch_listener = new ILaunchesListener() {

        public void launchesAdded(final ILaunch[] launches) {
        }

        public void launchesChanged(final ILaunch[] launches) {
            Protocol.invokeAndWait(new Runnable() {
                public void run() {
                    for (ILaunch launch : launches) {
                        TCFModel model = models.get(launch);
                        if (model != null) model.launchChanged();
                    }
                }
            });
        }

        public void launchesRemoved(final ILaunch[] launches) {
            Protocol.invokeAndWait(new Runnable() {
                public void run() {
                    for (ILaunch launch : launches) {
                        TCFModel model = models.remove(launch);
                        if (model != null) {
                            sync_model_map.remove(launch);
                            model.dispose();
                        }
                    }
                }
            });
        }
    };

    private final IWorkbenchListener workbench_listener = new IWorkbenchListener() {

        @Override
        public boolean preShutdown(IWorkbench workbench, boolean forced) {
            for (ILaunch launch : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
                if (launch instanceof TCFLaunch) {
                    try {
                        ((TCFLaunch)launch).disconnect();
                        DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
                    }
                    catch (Exception x) {
                        Activator.log("Cannot disconnect TCF launch", x);
                    }
                }
            }
            TCFMemoryBlock.onWorkbenchShutdown();
            return true;
        }

        @Override
        public void postShutdown(IWorkbench workbench) {
        }
    };

    public TCFModelManager() {
        assert Protocol.isDispatchThread();
        try {
            PlatformUI.getWorkbench().addWorkbenchListener(workbench_listener);
        }
        catch (IllegalStateException e) {
            // In headless environments the plug-in load can be still triggered.
            // Should not trigger an "Unhandled exception in TCF event dispatch thread"
        }
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(debug_launch_listener);
        TCFLaunch.addListener(tcf_launch_listener);
    }

    public void dispose() {
        assert Protocol.isDispatchThread();
        try {
            PlatformUI.getWorkbench().removeWorkbenchListener(workbench_listener);
        }
        catch (IllegalStateException e) {
            // In headless environments the plug-in load can be still triggered.
            // Should not trigger an "Unhandled exception in TCF event dispatch thread"
        }
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(debug_launch_listener);
        TCFLaunch.removeListener(tcf_launch_listener);
        for (Iterator<TCFModel> i = models.values().iterator(); i.hasNext();) {
            TCFModel model = i.next();
            sync_model_map.remove(model.getLaunch());
            model.dispose();
            i.remove();
        }
        assert models.isEmpty();
    }

    public void addListener(ModelManagerListener l) {
        listeners.add(l);
    }

    public void removeListener(ModelManagerListener l) {
        listeners.remove(l);
    }

    public Collection<TCFModel> getModels() {
        assert Protocol.isDispatchThread();
        return models.values();
    }

    public TCFModel getModel(TCFLaunch launch) {
        assert Protocol.isDispatchThread();
        return models.get(launch);
    }

    public TCFNodeLaunch getRootNode(TCFLaunch launch) {
        TCFModel model = getModel(launch);
        if (model == null) return null;
        return model.getRootNode();
    }

    public static TCFModelManager getModelManager() {
        return Activator.getModelManager();
    }

    /**
     * Synchronized and thread-safe method to map a launch to TCFModel.
     */
    public static TCFModel getModelSync(Launch launch) {
        if (launch instanceof TCFLaunch) return sync_model_map.get((TCFLaunch)launch);
        return null;
    }

    /**
     * Synchronized and thread-safe method to map a launch to TCFNodeLaunch.
     */
    public static TCFNodeLaunch getRootNodeSync(Launch launch) {
        if (launch instanceof TCFLaunch) {
            TCFModel model = sync_model_map.get((TCFLaunch)launch);
            if (model != null) return model.getRootNode();
        }
        return null;
    }
}
