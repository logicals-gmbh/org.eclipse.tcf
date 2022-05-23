/*******************************************************************************
 * Copyright (c) 2009, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch.setup;

import java.io.IOException;

interface IRemoteShell {

    /**
     * String that is used as shell prompt.
     */
    static final String PROMPT = "***SHELL***>";

    /**
     * Send text to remote shell.
     * @param s - a string for shell input.
     * @throws IOException
     */
    void write(String s) throws IOException;

    /**
     * Read shell output until given string if found.
     * @param s - a string to search in shell output.
     * @throws IOException
     */
    void expect(String s) throws IOException;

    /**
     * Read and collect shell output until shell prompt is found.
     * @return shell output, not including the prompt.
     * @throws IOException
     */
    String waitPrompt() throws IOException;

    /**
     * Exit shell and close communication channel.
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Enable/disable debug output to System.out.
     */
    void setDebug(boolean debug);
}
