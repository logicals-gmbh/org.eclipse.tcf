 /*******************************************************************************
 * Copyright (c) 2006, 2013 Wind River Systems, Inc., Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/


 * Contributors:
 *    Liping Ke        (Intel Corp.) - initial API and implementation
 *    Sheldon D'souza  (Celunite)    - LoginThread and readUntil implementation
 *    Liping Ke        (Intel Corp.) - For non-login mode, we don't need detect command prompt
 ******************************************************************************/
package org.eclipse.tcf.internal.rse.shells;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.rse.core.model.IPropertySet;
import org.eclipse.rse.services.clientserver.PathUtility;
import org.eclipse.rse.services.clientserver.messages.CommonMessages;
import org.eclipse.rse.services.clientserver.messages.ICommonMessageIds;
import org.eclipse.rse.services.clientserver.messages.SimpleSystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.terminals.AbstractTerminalShell;
import org.eclipse.rse.services.terminals.ITerminalService;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.tcf.internal.rse.ITCFSessionProvider;
import org.eclipse.tcf.internal.rse.Messages;
import org.eclipse.tcf.internal.rse.TCFConnectorService;
import org.eclipse.tcf.internal.rse.TCFRSETask;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.ITerminals;
import org.eclipse.tcf.util.TCFVirtualInputStream;
import org.eclipse.tcf.util.TCFVirtualOutputStream;


public class TCFTerminalShell extends AbstractTerminalShell {
    private final ITCFSessionProvider fSessionProvider;
    private final IChannel fChannel;
    private String fPtyType;
    private ITerminals.TerminalContext terminalContext;
    private String fEncoding;
    private InputStream fInputStream;
    private OutputStream fOutputStream;
    private Writer fOutputStreamWriter;
    private int fWidth = 0;
    private int fHeight = 0;
    private String fContextID;
    private String inp_id;
    private String out_id;
    private boolean connected;
    private boolean exited;
    private ITerminals terminals;
    private int status;

    private IPropertySet tcfPropertySet = null;

    private static String defaultEncoding = new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();
    private ITerminals.TerminalsListener listeners = new ITerminals.TerminalsListener(){

        public void exited(String terminalId, int exitCode) {
            if(!terminalContext.getID().equals(terminalId)) return;
            terminals.removeListener(listeners);
            connected = false;
            exited = true;
        }


        public void winSizeChanged(String terminalId, int newWidth, int newHeight) {
        }
    };

    /* LoginThread and readUntil functionality are cloned from TelnetConnectorService
     * and then modified for our own needs
     * */
    private class LoginThread extends Thread {

        private String username;
        private String password;
        private int status = ITCFSessionProvider.SUCCESS_CODE;
        public LoginThread(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public void run() {
            tcfPropertySet = ((TCFConnectorService)fSessionProvider).getTCFPropertySet();
            /* By default, we only support non-login mode. If user open the
             * switch on the agent side, he can also set the properties in
             * TCF connection property*/
            String login_required = tcfPropertySet.getPropertyValue(TCFConnectorService.PROPERTY_LOGIN_REQUIRED);

            if (Boolean.valueOf(login_required).booleanValue()) {
                String login_prompt = tcfPropertySet.getPropertyValue(TCFConnectorService.PROPERTY_LOGIN_PROMPT);
                String password_prompt =tcfPropertySet.getPropertyValue(TCFConnectorService.PROPERTY_PASSWORD_PROMPT);
                String command_prompt = tcfPropertySet.getPropertyValue(TCFConnectorService.PROPERTY_COMMAND_PROMPT);
                String pwd_required = tcfPropertySet.getPropertyValue(TCFConnectorService.PROPERTY_PWD_REQUIRED);
                status = ITCFSessionProvider.SUCCESS_CODE;
                if (login_prompt != null && login_prompt.length() > 0) {
                    status = readUntil(login_prompt,fInputStream);
                    write(username + "\n"); //$NON-NLS-1$
                }
                if (Boolean.valueOf(pwd_required).booleanValue()) {
                    if (status == ITCFSessionProvider.SUCCESS_CODE && password_prompt != null && password_prompt.length() > 0) {
                        status = readUntil(password_prompt,fInputStream);
                        write(password + "\n"); //$NON-NLS-1$
                    }
                }
                if (status == ITCFSessionProvider.SUCCESS_CODE && command_prompt != null && command_prompt.length() > 0) {
                    status = readUntil(command_prompt,fInputStream);
                    write("\n"); //$NON-NLS-1$
                }
            }
            else {
                status = ITCFSessionProvider.SUCCESS_CODE;
            }
        }

        public int readUntil(String pattern,InputStream in) {
            try {
                char lastChar = pattern.charAt(pattern.length() - 1);
                StringBuffer sb = new StringBuffer();
                int ch = in.read();
                while (ch >= 0) {
                    char tch = (char) ch;
                    sb.append(tch);
                    if (tch=='t' && sb.indexOf("incorrect") >= 0) { //$NON-NLS-1$
                        return ITCFSessionProvider.ERROR_CODE;
                    }
                    if (tch=='d' && sb.indexOf("closed") >= 0) { //$NON-NLS-1$
                        return ITCFSessionProvider.CONNECT_CLOSED;
                    }
                    if (tch == lastChar) {
                        if (sb.toString().endsWith(pattern)) {
                            return ITCFSessionProvider.SUCCESS_CODE;
                        }
                    }
                    ch = in.read();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                SystemBasePlugin.logError(e.getMessage() == null ?      e.getClass().getName() : e.getMessage(), e);
            }
            return ITCFSessionProvider.CONNECT_CLOSED;
        }

        public int getLoginStatus() {
            return this.status;
        }
    }

    public void write(String value) {
        try {
            fOutputStream.write(value.getBytes());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int login(String username, String password) throws InterruptedException {
        long millisToEnd = System.currentTimeMillis() + ITCFSessionProvider.TCP_CONNECT_TIMEOUT * 1000;
        LoginThread checkLogin = new LoginThread(username, password);
        status = ITCFSessionProvider.ERROR_CODE;
        checkLogin.start();
        while (checkLogin.isAlive() && System.currentTimeMillis()<millisToEnd)
            checkLogin.join(500);
        status = checkLogin.getLoginStatus();
        checkLogin.join();
        return status;
    }
    /**
     * Construct a new TCF connection.
     *
     * The TCF channel is immediately connected in the Constructor.
     *
     * @param sessionProvider TCF session provider
     * @param ptyType Terminal type to set, or <code>null</code> if not
     *            relevant
     * @param encoding The default encoding to use for initial command.
     * @param environment Environment array to set, or <code>null</code> if
     *            not relevant.
     * @param initialWorkingDirectory initial directory to open the Terminal in.
     *            Use <code>null</code> or empty String ("") to start in a
     *            default directory. Empty String will typically start in the
     *            home directory.
     * @param commandToRun initial command to send.
     * @throws SystemMessageException in case anything goes wrong. Channels and
     *             Streams are all cleaned up again in this case.
     * @see ITerminalService
     */
    public TCFTerminalShell(final ITCFSessionProvider sessionProvider, final String ptyType,
            final String encoding, final String[] environment,
            String initialWorkingDirectory, String commandToRun)
    throws SystemMessageException {
        fSessionProvider = sessionProvider;
        fEncoding = encoding;
        fPtyType = ptyType;
        fChannel = fSessionProvider.getChannel();
        Exception nestedException = null;
        try {

            if (fChannel == null || fChannel.getState() != IChannel.STATE_OPEN)
                throw new Exception("TCP channel is not connected!");//$NON-NLS-1$

            new TCFRSETask<ITerminals.TerminalContext>() {
                public void run() {
                    terminals = ((TCFConnectorService)sessionProvider).getService(ITerminals.class);
                    fSessionProvider.onStreamsConnecting();
                    terminals.launch(ptyType, encoding, environment, new ITerminals.DoneLaunch() {
                        @Override
                        public void doneLaunch(IToken token, Exception error, ITerminals.TerminalContext ctx) {
                            if (ctx != null) {
                                terminalContext = ctx;
                                terminals.addListener(listeners);
                                inp_id = ctx.getStdOutID();
                                out_id = ctx.getStdInID();
                                fSessionProvider.onStreamsID(inp_id);
                                fSessionProvider.onStreamsID(out_id);
                                try {
                                    fInputStream = new TCFVirtualInputStream(fChannel, inp_id, new Runnable() {
                                        @Override
                                        public void run() {
                                            onInputStreamClosed();
                                        }
                                    });
                                    fOutputStream = new TCFVirtualOutputStream(fChannel, out_id, true, new Runnable() {
                                        @Override
                                        public void run() {
                                            onOutputStreamClosed();
                                        }
                                    });
                                }
                                catch (Exception x) {
                                    error = x;
                                }
                            }
                            fSessionProvider.onStreamsConnected();
                            if (error != null) error(error);
                            else done(ctx);
                        }
                    });
                }

            }.getS(null, Messages.TCFTerminalService_Name);

            fPtyType = terminalContext.getPtyType();
            fEncoding = terminalContext.getEncoding();
            fContextID = terminalContext.getID();
            fWidth = terminalContext.getWidth();
            fHeight = terminalContext.getHeight();

            String user = fSessionProvider.getSessionUserId();
            String password = fSessionProvider.getSessionPassword();
            status = ITCFSessionProvider.ERROR_CODE;

            if (fEncoding != null) {
                fOutputStreamWriter = new BufferedWriter(new OutputStreamWriter(fOutputStream, encoding));
            }
            else {
                // default encoding == System.getProperty("file.encoding")
                // TODO should try to determine remote encoding if possible
                fOutputStreamWriter = new BufferedWriter(new OutputStreamWriter(fOutputStream));
            }

            try {
                status = login(user, password);
            }
            finally {
                if (status == ITCFSessionProvider.CONNECT_CLOSED) {
                    // Give one time chance of retrying....
                }
            }

            // give another chance of retrying
            if (status == ITCFSessionProvider.CONNECT_CLOSED) {
                status = login(user, password);
            }

            if (status == ITCFSessionProvider.SUCCESS_CODE) {
                connected = true;
                if (initialWorkingDirectory != null && initialWorkingDirectory.length() > 0
                        && !initialWorkingDirectory.equals(".") //$NON-NLS-1$
                        && !initialWorkingDirectory.equals("Command Shell")) //$NON-NLS-1$ //FIXME workaround for bug 153047
                {
                    writeToShell("cd " + PathUtility.enQuoteUnix(initialWorkingDirectory)); //$NON-NLS-1$
                }

                if (commandToRun != null && commandToRun.length() > 0) {
                    writeToShell(commandToRun);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            nestedException = e;
        }
        finally {
            if (status != ITCFSessionProvider.SUCCESS_CODE) {
                SystemMessage msg;

                if (nestedException != null) {
                    msg = new SimpleSystemMessage(org.eclipse.tcf.internal.rse.Activator.PLUGIN_ID,
                            ICommonMessageIds.MSG_EXCEPTION_OCCURRED,
                            IStatus.ERROR,
                            CommonMessages.MSG_EXCEPTION_OCCURRED, nestedException);
                }
                else {
                    String strErr;
                    if (status == ITCFSessionProvider.CONNECT_CLOSED)
                        strErr = "Connection closed!";//$NON-NLS-1$
                    else if (status == ITCFSessionProvider.ERROR_CODE)
                        strErr = "Login Incorrect or meet other unknown error!";//$NON-NLS-1$
                    else
                        strErr = "Not identified Errors";//$NON-NLS-1$

                    msg = new SimpleSystemMessage(org.eclipse.tcf.internal.rse.Activator.PLUGIN_ID,
                            ICommonMessageIds.MSG_COMM_AUTH_FAILED,
                            IStatus.ERROR,
                            strErr,
                            "Meet error when trying to login in!");//$NON-NLS-1$
                    msg.makeSubstitution(((TCFConnectorService)fSessionProvider).getHost().getAliasName());
                }
                throw new SystemMessageException(msg);//$NON-NLS-1$
            }
        }

    }

    public void writeToShell(String command) throws IOException {
        if (isActive()) {
            if ("#break".equals(command)) { //$NON-NLS-1$
                command = "\u0003"; // Unicode 3 == Ctrl+C //$NON-NLS-1$
            }
            else {
                command += "\r\n"; //$NON-NLS-1$
            }
            fOutputStreamWriter.write(command);
        }
    }

    public void exit() {

        if (fChannel == null || (fChannel.getState() == IChannel.STATE_CLOSED) || !connected) {
            return;
        }
        try {
            getOutputStream().close();
            getInputStream().close();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        } //$NON-NLS-1$

        try {
            new TCFRSETask<Object>() {
                public void run() {
                    terminalContext.exit(new ITerminals.DoneCommand(){
                        public void doneCommand(IToken token, Exception error) {
                            if (error != null) error(error);
                            else done(this);
                        }
                    });
                }
            }.getS(null, Messages.TCFShellService_Name); //seems no need block here. need further modification.
        }
        catch (SystemMessageException e) {
            e.printStackTrace();
        } //$NON-NLS-1$;
    }

    public InputStream getInputStream() {
        if (!connected) throw new Error("Not connected");
        return fInputStream;
    }

    public OutputStream getOutputStream() {
        if (!connected) throw new Error("Not connected");
        return fOutputStream;
    }

    public boolean isActive() {
        if (fChannel != null && fChannel.getState() != IChannel.STATE_CLOSED && connected) {
            return true;
        }
        exit();
        // shell is not active: check for session lost
        return false;
    }

    public String getPtyType() {
        return fPtyType;
    }

    public void setTerminalSize(int newWidth, int newHeight) {
        if (fWidth == newWidth && fHeight == newHeight) return;
        if (fChannel == null || (fChannel.getState() == IChannel.STATE_CLOSED) || !connected) {
            // do nothing
            return;
        }
        fWidth = newWidth;
        fHeight = newHeight;
        try {
            new TCFRSETask<Object>() {
                public void run() {
                    if (fChannel != null && connected) {
                        terminals.setWinSize(fContextID, fWidth, fHeight, new ITerminals.DoneCommand(){
                            public void doneCommand(IToken token, Exception error) {
                                if (error != null) error(error);
                                else done(this);
                            }
                        });
                    }
                    else {
                        done(this);
                    }
                }
            }.getS(null, Messages.TCFShellService_Name);
        }
        catch (SystemMessageException e) {
            e.printStackTrace();
        }
    }

    public String getDefaultEncoding() {
        if (fEncoding != null) return fEncoding;
        return defaultEncoding;
    }

    private void onInputStreamClosed() {
        inp_id = null;
        if (out_id == null && !exited) {
            terminalContext.exit(new ITerminals.DoneCommand(){
                public void doneCommand(IToken token, Exception error) {
                }
            });
        }
    }

    private void onOutputStreamClosed() {
        out_id = null;
        if (inp_id == null && !exited) {
            terminalContext.exit(new ITerminals.DoneCommand(){
                public void doneCommand(IToken token, Exception error) {
                }
            });
        }
    }
}
