/*******************************************************************************
 * Copyright (c) 2007-2019 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.services;

import java.util.Map;

import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;

/**
 * IPathMap service manages file path translation across systems.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IPathMap extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "PathMap";

    /**
     * Path mapping rule property names.
     */
    static final String
        /** String, rule ID */
        PROP_ID = "ID",

        /** String, source, or compile-time file path */
        PROP_SOURCE = "Source",

        /** String, destination, or run-time file path */
        PROP_DESTINATION = "Destination",

        /** String, symbols context group ID or name, deprecated - use ContextQuery */
        PROP_CONTEXT = "Context",

        /** String, contexts query, see IContextQuery */
        PROP_CONTEXT_QUERY = "ContextQuery",

        /** String, */
        PROP_HOST = "Host",

        /** String, file access protocol, see PROTOCOL_*, default is regular file */
        PROP_PROTOCOL = "Protocol";

    /**
     * PROP_PROTOCOL values.
     */
    static final String
        /** Regular file access using system calls */
        PROTOCOL_FILE = "file",

        /** File should be accessed using File System service on host */
        PROTOCOL_HOST = "host",

        /** File should be accessed using File System service on target */
        PROTOCOL_TARGET = "target";

    /**
     * PathMapRule interface represents a single file path mapping rule.
     */
    interface PathMapRule {

        /**
         * Get rule properties. See PROP_* definitions for property names.
         * Properties are read only, clients should not try to modify them.
         * @return Map of rule properties.
         */
        Map<String,Object> getProperties();

        /**
         * Get rule unique ID.
         * Same as getProperties().get(PROP_ID)
         * @return rule ID.
         */
        String getID();

        /**
         * Get compile-time file path.
         * Same as getProperties().get(PROP_SOURCE)
         * @return compile-time file path.
         */
        String getSource();

        /**
         * Get run-time file path.
         * Same as getProperties().get(PROP_DESTINATION)
         * @return run-time file path.
         */
        String getDestination();

        /**
         * Get host name of this rule.
         * Same as getProperties().get(PROP_HOST)
         * @return host name.
         */
        String getHost();

        /**
         * Get file access protocol name.
         * Same as getProperties().get(PROP_PROTOCOL)
         * @return protocol name.
         */
        String getProtocol();

        /**
         * Get context query that defines scope of the mapping rule, see also IContextQuery.
         * Same as getProperties().get(PROP_CONTEXT_QUERY)
         * @return context query expression, or null.
         */
        String getContextQuery();
    }

    /**
     * Retrieve file path mapping rules.
     *
     * @param done - call back interface called when operation is completed.
     * @return - pending command handle.
     */
    IToken get(DoneGet done);

    /**
     * Client call back interface for get().
     */
    interface DoneGet {
        /**
         * Called when file path mapping retrieval is done.
         * @param error - error description if operation failed, null if succeeded.
         * @param map - file path mapping data.
         */
        void doneGet(IToken token, Exception error, PathMapRule[] map);
    }

    /**
     * Set file path mapping rules.
     *
     * @param map - file path mapping rules.
     * @param done - call back interface called when operation is completed.
     * @return - pending command handle.
     */
    IToken set(PathMapRule[] map, DoneSet done);

    /**
     * Client call back interface for set().
     */
    interface DoneSet {
        /**
         * Called when file path mapping transmission is done.
         * @param error - error description if operation failed, null if succeeded.
         * @param map - memory map data.
         */
        void doneSet(IToken token, Exception error);
    }

    /**
     * Add path map event listener.
     * @param listener - path map event listener to add.
     */
    void addListener(PathMapListener listener);

    /**
     * Remove path map event listener.
     * @param listener - path map event listener to remove.
     */
    void removeListener(PathMapListener listener);

    /**
     * Service events listener interface.
     */
    interface PathMapListener {

        /**
         * Called when path map changes.
         */
        void changed();
    }
}
