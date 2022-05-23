/*******************************************************************************
 * Copyright (c) 2008, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.adapters;

import java.util.Collection;
import java.util.Map;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.tcf.debug.ui.ITCFDebugUIConstants;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.internal.debug.ui.ColorCache;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildrenContextQuery;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelManager;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;

class TCFLaunchLabelProvider implements IElementLabelProvider {

    public void update(ILabelUpdate[] updates) {
        for (int i = 0; i < updates.length; i++) {
            String view_id = updates[i].getPresentationContext().getId();
            if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
                updateContextQueryViewLabel(updates[i]);
            }
            else {
                updateDebugViewLabel(updates[i]);
            }
        }
    }

    private void updateDebugViewLabel(ILabelUpdate result) {
        final TCFLaunch launch = (TCFLaunch)result.getElement();
        ImageDescriptor image = DebugUITools.getDefaultImageDescriptor(launch);
        if (image == null) image = ImageCache.getImageDescriptor(ImageCache.IMG_TCF);
        result.setImageDescriptor(image, 0);
        String status = "";
        if (launch.isConnecting()) {
            status = "Connecting";
        }
        else if (launch.isDisconnected()) {
            status = "Disconnected";
        }
        String peer_name = launch.getPeerName();
        if (peer_name != null) {
            if (status.length() == 0) status = peer_name;
            else status = peer_name + ": " + status;
        }
        if (status.length() > 0) status = " (" + status + ")";
        Throwable error = launch.getError();
        if (error != null) {
            status += ": " + TCFModel.getErrorMessage(error, false);
            result.setForeground(ColorCache.rgb_error, 0);
        }
        else if (launch.isExited()) {
            status += ": All exited or detached";
            int code = launch.getExitCode();
            if (code > 0) status += ", exit code " + code;
            if (code < 0) {
                status += ", signal " + (-code);
                Collection<Map<String,Object>> sigs = launch.getSignalList();
                if (sigs != null) {
                    for (Map<String,Object> m : sigs) {
                        Number num = (Number)m.get(IProcesses.SIG_CODE);
                        if (num == null) continue;
                        if (num.intValue() != -code) continue;
                        String s = (String)m.get(IProcesses.SIG_NAME);
                        if (s == null) continue;
                        status += " (" + s + ")";
                        break;
                    }
                }
            }
        }
        String name = "?";
        ILaunchConfiguration cfg = launch.getLaunchConfiguration();
        if (cfg != null) name = cfg.getName();
        result.setLabel(name + status, 0);
        result.done();
    }

    private void updateContextQueryViewLabel(final ILabelUpdate result) {
        Protocol.invokeLater(new Runnable() {
            public void run() {
                TCFLaunch launch = (TCFLaunch)result.getElement();
                ImageDescriptor image = DebugUITools.getDefaultImageDescriptor(launch);
                if (image == null) image = ImageCache.getImageDescriptor(ImageCache.IMG_TCF);
                result.setImageDescriptor(image, 0);

                StringBuffer label = new StringBuffer();
                TCFModel model = TCFModelManager.getModelManager().getModel(launch);

                if (model != null && model.getRootNode() != null) {
                    TCFChildrenContextQuery.Descendants des = TCFChildrenContextQuery.getDescendants(
                            model.getRootNode(), result, this);
                    if (des == null) return;
                    if (des.map != null && des.map.size() > 0) {
                        label.append("(");
                        label.append(des.map.size());
                        label.append(") ");
                    }
                }

                ILaunchConfiguration cfg = launch.getLaunchConfiguration();
                if (cfg != null) {
                    label.append( cfg.getName() );
                }
                else {
                    label.append("?");
                }

                String peer_name = launch.getPeerName();
                if (peer_name != null) {
                    label.append(" (");
                    label.append(peer_name);
                    label.append(")");
                }

                result.setLabel(label.toString(), 0);
                result.done();
            }
        });

    }
}
