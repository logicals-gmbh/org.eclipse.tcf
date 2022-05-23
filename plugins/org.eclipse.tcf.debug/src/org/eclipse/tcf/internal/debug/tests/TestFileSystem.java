/*******************************************************************************
 * Copyright (c) 2008, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.tests;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DirEntry;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.util.TCFFileInputStream;
import org.eclipse.tcf.util.TCFFileOutputStream;

class TestFileSystem implements ITCFTest, IFileSystem.DoneStat,
        IFileSystem.DoneOpen, IFileSystem.DoneClose,
        IFileSystem.DoneWrite, IFileSystem.DoneRead,
        IFileSystem.DoneRename, IFileSystem.DoneRealPath,
        IFileSystem.DoneRemove, IFileSystem.DoneRoots,
        IFileSystem.DoneReadDir {

    private final TCFTestSuite test_suite;
    private final int channel_id;

    private static final String client_id = UUID.randomUUID().toString();

    private static final int
        STATE_PRE = 0,
        STATE_RD_DIR = 1,
        STATE_WRITE = 2,
        STATE_READ = 3,
        STATE_OUT = 4,
        STATE_INP = 5,
        STATE_EXIT = 6;

    private final IFileSystem files;
    private final Random rnd = new Random();
    private final LinkedList<String> tmp_files = new LinkedList<String>();
    private final HashMap<IToken,Object> cmds = new HashMap<IToken,Object>();
    private byte[] data;
    private String root;
    private String tmp_path;
    private String file_name;
    private IFileHandle handle;
    private int state = STATE_PRE;
    private boolean async_close;

    private static class ReadCmd {
        int offs;
        int size;
    }

    TestFileSystem(TCFTestSuite test_suite, IChannel channel, int channel_id) {
        this.test_suite = test_suite;
        this.channel_id = channel_id;
        files = channel.getRemoteService(IFileSystem.class);
    }

    public void start() {
        if (files == null) {
            test_suite.done(this, null);
        }
        else {
            files.roots(this);
        }
    }

    private void testClosedHandle(IFileHandle handle) {
        int n = rnd.nextInt(3) + 1;
        for (int i = 0; i < n; i++) {
            switch (rnd.nextInt(7)) {
            case 0:
                files.close(handle, new IFileSystem.DoneClose() {
                    @Override
                    public void doneClose(IToken token, FileSystemException error) {
                        if (error == null) exit(new Error("Error expected"));
                    }
                });
                break;
            case 1:
                files.fsetstat(handle, null, new IFileSystem.DoneSetStat() {
                    @Override
                    public void doneSetStat(IToken token, FileSystemException error) {
                        if (error == null) exit(new Error("Error expected"));
                    }
                });
                break;
            case 2:
                files.fstat(handle, new IFileSystem.DoneStat() {
                    @Override
                    public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
                        if (error == null) exit(new Error("Error expected"));
                    }
                });
                break;
            case 3:
                files.read(handle, rnd.nextBoolean() ? 0 : -1, 1, new IFileSystem.DoneRead() {
                    @Override
                    public void doneRead(IToken token, FileSystemException error, byte[] data, boolean eof) {
                        if (error == null) exit(new Error("Error expected"));
                    }
                });
                break;
            case 4:
                files.readdir(handle, new IFileSystem.DoneReadDir() {
                    @Override
                    public void doneReadDir(IToken token, FileSystemException error, DirEntry[] entries, boolean eof) {
                        if (error == null) exit(new Error("Error expected"));
                    }
                });
                break;
            case 5:
                files.write(handle, rnd.nextBoolean() ? 0 : -1, new byte[1], 0, 1, new IFileSystem.DoneWrite() {
                    @Override
                    public void doneWrite(IToken token, FileSystemException error) {
                        if (error == null) exit(new Error("Error expected"));
                    }
                });
                break;
            }
        }
    }

    public void doneRoots(IToken token, FileSystemException error, DirEntry[] entries) {
        assert state == STATE_PRE;
        if (error != null) {
            exit(error);
        }
        else if (entries == null || entries.length == 0) {
            exit(new Exception("Invalid FileSysrem.roots responce: empty roots array"));
        }
        else {
            for (DirEntry d : entries) {
                if (d.filename.startsWith("A:")) continue;
                if (d.filename.startsWith("B:")) continue;
                if (d.filename.startsWith("/romfs")) continue;
                root = d.filename;
                break;
            }
            if (root == null) exit(new Exception("Invalid FileSystem.roots responce: no suitable root"));
            else files.opendir(root, this);
        }
    }

    public void doneReadDir(IToken token, FileSystemException error,
            DirEntry[] entries, boolean eof) {
        if (error != null) {
            exit(error);
        }
        else if (state == STATE_PRE) {
            if (entries != null && tmp_path == null) {
                for (DirEntry e : entries) {
                    if (e.filename.equals("tmp") || e.filename.equalsIgnoreCase("temp")) {
                        tmp_path = root;
                        if (!tmp_path.endsWith("/")) tmp_path += "/";
                        tmp_path += e.filename;
                        break;
                    }
                }
            }
            if (eof) {
                if (tmp_path == null) {
                    exit(new Exception("File system test failed: cannot find temporary directory"));
                    return;
                }
                files.close(handle, this);
                testClosedHandle(handle);
            }
            else {
                files.readdir(handle, this);
            }
        }
        else if (state == STATE_RD_DIR) {
            if (entries != null) {
                for (DirEntry e : entries) {
                    if (e.filename.startsWith("tcf-test-" + client_id + "-" + channel_id + "-")) {
                        tmp_files.add(e.filename);
                    }
                }
            }
            if (eof) {
                files.close(handle, this);
                testClosedHandle(handle);
            }
            else {
                files.readdir(handle, this);
            }
        }
        else {
            assert false;
        }
    }

    public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
        if (error != null) {
            exit(error);
        }
        else if (state == STATE_READ) {
            if (attrs.size != data.length) {
                exit(new Exception("Invalid FileSysrem.fstat responce: wrong file size"));
            }
            else {
                files.close(handle, this);
                testClosedHandle(handle);
            }
        }
        else if (state == STATE_WRITE) {
            char[] bf = new char[64];
            for (int i = 0; i < bf.length; i++) {
                char ch = (char)(rnd.nextInt(0x4000) + 0x20);
                switch (ch) {
                case '<':
                case '>':
                case ':':
                case '"':
                case '/':
                case '\\':
                case '|':
                case '?':
                case '*':
                case '~':
                    ch = '-';
                    break;
                }
                bf[i] = ch;
            }
            file_name = tmp_path + "/tcf-test-" + client_id + "-" + channel_id + "-" + new String(bf) + ".tmp";
            files.open(file_name, IFileSystem.TCF_O_CREAT | IFileSystem.TCF_O_TRUNC | IFileSystem.TCF_O_WRITE, null, this);
        }
        else {
            assert false;
        }
    }

    public void doneOpen(IToken token, FileSystemException error, final IFileHandle handle) {
        if (error != null) {
            exit(error);
        }
        else {
            this.handle = handle;
            if (state == STATE_READ) {
                for (int i = 0; i < rnd.nextInt(8); i++) {
                    ReadCmd cmd = new ReadCmd();
                    cmd.offs = rnd.nextInt(data.length);
                    cmd.size = rnd.nextInt(data.length - cmd.offs) + 2;
                    cmds.put(files.read(handle, cmd.offs, cmd.size, this), cmd);
                }
                if (rnd.nextBoolean()) {
                    ReadCmd cmd = new ReadCmd();
                    cmd.offs = 0;
                    cmd.size = data.length + 1;
                    cmds.put(files.read(handle, cmd.offs, cmd.size, this), cmd);
                }
                else {
                    int pos = 0;
                    while (pos < data.length) {
                        int size = rnd.nextInt(data.length - pos) + 1;
                        ReadCmd cmd = new ReadCmd();
                        cmd.offs = pos;
                        cmd.size = size + 1;
                        cmds.put(files.read(handle, cmd.offs, cmd.size, this), cmd);
                        pos += size;
                    }
                }
                async_close = rnd.nextBoolean();
                if (async_close) {
                    files.close(handle, this);
                    testClosedHandle(handle);
                }
            }
            else if (state == STATE_WRITE) {
                data = new byte[rnd.nextInt(0x1000) + 1];
                rnd.nextBytes(data);
                if (rnd.nextBoolean()) {
                    cmds.put(files.write(handle, 0, data, 0, data.length, this), null);
                }
                else {
                    int pos = 0;
                    while (pos < data.length) {
                        int size = rnd.nextInt(data.length - pos) + 1;
                        cmds.put(files.write(handle, pos, data, pos, size, this), null);
                        pos += size;
                    }
                }
                async_close = rnd.nextBoolean();
                if (async_close) {
                    files.close(handle, this);
                    testClosedHandle(handle);
                }
            }
            else if (state == STATE_INP) {
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            int pos = 0;
                            int len = data.length * 16;
                            byte[] buf = new byte[333];
                            int buf_pos = 0;
                            int buf_len = 0;
                            boolean mark = true;
                            boolean reset = true;
                            int mark_pos = rnd.nextInt(len - 1);
                            int reset_pos = mark_pos + rnd.nextInt(len - mark_pos);
                            assert reset_pos >= mark_pos;
                            InputStream inp = new TCFFileInputStream(handle, 133);
                            for (;;) {
                                if (mark && pos == mark_pos) {
                                    inp.mark(len);
                                    mark = false;
                                }
                                if (reset && pos == reset_pos) {
                                    inp.reset();
                                    reset = false;
                                    pos = mark_pos;
                                    buf_pos = buf_len = 0;
                                }
                                int ch = 0;
                                if (buf_pos >= buf_len && (pos >= mark_pos || pos + buf.length <= mark_pos)) {
                                    buf_len = inp.read(buf, buf_pos = 0, buf.length);
                                    if (buf_len < 0) break;
                                }
                                if (buf_pos < buf_len) {
                                    ch = buf[buf_pos++] & 0xff;
                                }
                                else {
                                    ch = inp.read();
                                    if (ch < 0) break;
                                }
                                int dt = data[pos % data.length] & 0xff;
                                if (ch != dt) {
                                    error(new Exception("Invalid TCFFileInputStream.read responce: wrong data at offset " + pos +
                                            ", expected " + dt + ", actual " + ch));
                                }
                                pos++;
                            }
                            if (pos != data.length * 16) {
                                error(new Exception("Invalid TCFFileInputStream.read responce: wrong file length: " +
                                        "expected " + data.length + ", actual " + pos));
                            }
                            inp.close();
                            Protocol.invokeLater(new Runnable() {
                                public void run() {
                                    state = STATE_EXIT;
                                    files.rename(file_name, file_name + ".rnm", TestFileSystem.this);
                                }
                            });
                        }
                        catch (Throwable x) {
                            error(x);
                        }
                    }
                    private void error(final Throwable x) {
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                exit(x);
                            }
                        });
                    }
                };
                thread.setName("TCF FileSystem Test");
                thread.start();
            }
            else if (state == STATE_OUT) {
                rnd.nextBytes(data);
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            int pos = 0;
                            int len = data.length * 16;
                            OutputStream out = new TCFFileOutputStream(handle, 121);
                            while (pos < len) {
                                int m = pos % data.length;
                                int n = rnd.nextInt(1021);
                                if (n > data.length - m) n = data.length - m;
                                out.write(data, m, n);
                                pos += n;
                                if (pos == len) break;
                                out.write(data[pos % data.length] & 0xff);
                                pos++;
                            }
                            out.close();
                            Protocol.invokeLater(new Runnable() {
                                public void run() {
                                    state = STATE_INP;
                                    files.open(file_name, IFileSystem.TCF_O_READ, null, TestFileSystem.this);
                                }
                            });
                        }
                        catch (Throwable x) {
                            error(x);
                        }
                    }
                    private void error(final Throwable x) {
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                exit(x);
                            }
                        });
                    }
                };
                thread.setName("TCF FileSystem Test");
                thread.start();
            }
            else {
                assert state == STATE_PRE || state == STATE_RD_DIR;
                files.readdir(handle, this);
            }
        }
    }

    public void doneWrite(IToken token, FileSystemException error) {
        assert cmds.containsKey(token);
        cmds.remove(token);
        if (error != null) {
            exit(error);
        }
        else if (cmds.size() == 0 && !async_close) {
            files.close(handle, this);
            testClosedHandle(handle);
        }
    }

    public void doneRead(IToken token, FileSystemException error, byte[] data, boolean eof) {
        assert cmds.containsKey(token);
        ReadCmd cmd = (ReadCmd)cmds.remove(token);
        if (error != null) {
            exit(error);
        }
        else {
            if (cmd.offs + cmd.size > this.data.length && !eof) {
                exit(new Exception("Invalid FileSysrem.read responce: EOF expected"));
            }
            else if (data.length != (eof ? cmd.size - 1 : cmd.size)) {
                exit(new Exception("Invalid FileSysrem.read responce: wrong data array size"));
            }
            else {
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != this.data[i + cmd.offs]) {
                        exit(new Exception("Invalid FileSysrem.read responce: wrong data at offset " + i +
                                ", expected " + this.data[i + cmd.offs] + ", actual " + data[i]));
                        return;
                    }
                }
                if (cmds.size() == 0 && !async_close) files.fstat(handle, this);
            }
        }
    }

    public void doneClose(IToken token, FileSystemException error) {
        if (error != null) {
            exit(error);
        }
        else {
            handle = null;
            if (state == STATE_PRE) {
                state = STATE_RD_DIR;
                files.opendir(tmp_path, this);
            }
            else if (state == STATE_RD_DIR) {
                state = STATE_WRITE;
                files.realpath(tmp_path, this);
            }
            else if (state == STATE_WRITE) {
                testSetStat(new Runnable() {
                    @Override
                    public void run() {
                        state = STATE_READ;
                        files.open(file_name, IFileSystem.TCF_O_READ, null, TestFileSystem.this);
                    }
                });
            }
            else if (state == STATE_READ) {
                state = STATE_OUT;
                files.open(file_name, IFileSystem.TCF_O_WRITE, null, this);
            }
            else {
                assert false;
            }
        }
    }

    public void doneRename(IToken token, FileSystemException error) {
        assert state == STATE_EXIT;
        if (error != null) {
            exit(error);
        }
        else {
            files.realpath(file_name + ".rnm", this);
        }
    }

    public void doneRealPath(IToken token, FileSystemException error, String path) {
        if (error != null) {
            exit(error);
        }
        else if (state == STATE_WRITE) {
            tmp_path = path;
            if (tmp_files.size() > 0) {
                files.remove(tmp_path + "/" + tmp_files.removeFirst(), this);
            }
            else {
                files.stat(tmp_path, this);
            }
        }
        else if (!path.equals(file_name + ".rnm")) {
            exit(new Exception("Invalid FileSysrem.realpath responce: " + path));
        }
        else {
            files.remove(file_name + ".rnm", this);
        }
    }

    public void doneRemove(IToken token, FileSystemException error) {
        if (error != null) {
            exit(error);
        }
        else if (state == STATE_WRITE) {
            if (tmp_files.size() > 0) {
                files.remove(tmp_path + "/" + tmp_files.removeFirst(), this);
            }
            else {
                files.stat(tmp_path, this);
            }
        }
        else if (state == STATE_EXIT) {
            exit(null);
        }
        else {
            assert false;
        }
    }

    private void testSetStat(final Runnable done) {
        files.stat(file_name, new IFileSystem.DoneStat() {
            @Override
            public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
                if (error != null) {
                    exit(error);
                    return;
                }
                FileAttrs new_attrs = new FileAttrs(
                        IFileSystem.ATTR_SIZE | IFileSystem.ATTR_ACMODTIME,
                        attrs.size, 0, 0, 0, attrs.atime, attrs.mtime, null);
                files.setstat(file_name, new_attrs, new IFileSystem.DoneSetStat() {
                    @Override
                    public void doneSetStat(IToken token, FileSystemException error) {
                        if (error != null) {
                            exit(error);
                            return;
                        }
                        testFSetStat(done);
                    }
                });
            }
        });
    }

    private void testFSetStat(final Runnable done) {
        files.open(file_name, IFileSystem.TCF_O_WRITE, null, new IFileSystem.DoneOpen() {
            @Override
            public void doneOpen(IToken token, FileSystemException error, final IFileHandle handle) {
                if (error != null) {
                    exit(error);
                    return;
                }
                files.fstat(handle, new IFileSystem.DoneStat() {
                    @Override
                    public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
                        if (error != null) {
                            exit(error);
                            return;
                        }
                        FileAttrs new_attrs = new FileAttrs(
                                IFileSystem.ATTR_SIZE | IFileSystem.ATTR_ACMODTIME,
                                attrs.size, 0, 0, 0, attrs.atime, attrs.mtime, null);
                        files.fsetstat(handle, new_attrs, new IFileSystem.DoneSetStat() {
                            @Override
                            public void doneSetStat(IToken token, FileSystemException error) {
                                if (error != null) {
                                    exit(error);
                                    return;
                                }
                                files.close(handle, new IFileSystem.DoneClose() {
                                    @Override
                                    public void doneClose(IToken token, FileSystemException error) {
                                        if (error != null) {
                                            exit(error);
                                            return;
                                        }
                                        done.run();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void exit(Throwable x) {
        if (!test_suite.isActive(this)) return;
        test_suite.done(this, x);
    }

    public boolean canResume(String id) {
        return true;
    }
}
