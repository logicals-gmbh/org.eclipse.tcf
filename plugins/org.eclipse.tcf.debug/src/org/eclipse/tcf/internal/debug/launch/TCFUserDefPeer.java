/*******************************************************************************
 * Copyright (c) 2007, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.launch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.tcf.internal.debug.Activator;
import org.eclipse.tcf.core.UserDefPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;

/**
 * The class represents manually configured (user defined) TCF peers (targets).
 * Unlike auto-discovered peers, manually configured ones are persistent -
 * they exist until explicitly deleted by user.
 * Eclipse plug-in state storage is used to keep the configuration data.
 */
public class TCFUserDefPeer extends UserDefPeer {

    public TCFUserDefPeer(Map<String, String> attrs) {
        super(attrs);
    }

    /**
     * Load manually configured peers from persistent storage.
     */
    public static void loadPeers() {
        try {
            assert Protocol.isDispatchThread();
            IPath path = Activator.getDefault().getStateLocation();
            File f = path.append("peers.ini").toFile();
            if (!f.exists()) return;
            HashMap<String,String> attrs = new HashMap<String,String>();
            BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            for (;;) {
                String s = rd.readLine();
                if (s == null) break;
                if (s.length() == 0) {
                    new TCFUserDefPeer(attrs);
                    attrs = new HashMap<String,String>();
                }
                else {
                    int i = s.indexOf('=');
                    if (i > 0) attrs.put(s.substring(0, i), s.substring(i + 1));
                }
            }
            rd.close();
        }
        catch (Exception x) {
            Activator.log("Cannot read peer list", x);
        }
    }

    /**
     * Save manually configured peers to persistent storage.
     */
    public static void savePeers() {
        try {
            assert Protocol.isDispatchThread();
            IPath path = Activator.getDefault().getStateLocation();
            File f = path.append("peers.ini").toFile();
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
            for (IPeer peer : Protocol.getLocator().getPeers().values()) {
                if (peer instanceof TCFUserDefPeer) {
                    Map<String,String> attrs = peer.getAttributes();
                    for (String nm : attrs.keySet()) {
                        wr.write(nm);
                        wr.write('=');
                        wr.write(attrs.get(nm));
                        wr.newLine();
                    }
                    wr.newLine();
                }
            }
            wr.close();
        }
        catch (Exception x) {
            Activator.log("Cannot save peer list", x);
        }
    }
}
