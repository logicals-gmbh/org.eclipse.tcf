/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River) - [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.url;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem.DoneWrite;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The TCF output stream returned by {@link TcfURLConnection#getOutputStream()}.
 */
public class TcfOutputStream extends OutputStream {
	// Default chunk size while pumping the data.
	private static final int DEFAULT_CHUNK_SIZE = 5 * 1024;

	// Current writing position
	long position;
	// The byte array used to buffer data.
	byte[] buffer;
	// The offset being written in the buffer.
	int offset;

	// If the stream has been closed.
	boolean closed;
	// The current error during writing.
	Exception ERROR;

	// The URL Connection
	TcfURLConnection connection;
	/**
	 * Create a TCF output stream connected the specified peer with specified
	 * path to the remote resource.
	 *
	 */
	public TcfOutputStream(TcfURLConnection connection) {
		this(connection, DEFAULT_CHUNK_SIZE);
	}

	/**
	 * Create a TCF output stream connected the specified peer with specified
	 * path to the remote resource using the specified buffer size.
	 *
	 * @param chunk_size
	 *            The buffer size.
	 */
	public TcfOutputStream(TcfURLConnection connection, int chunk_size) {
		this.connection = connection;
		buffer = new byte[chunk_size];
		offset = 0;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		if (closed)
			throw new IOException(Messages.TcfOutputStream_StreamClosed);
		if (ERROR != null) {
			IOException exception = new IOException(ERROR.toString());
			exception.initCause(ERROR);
			throw exception;
		}
		if (offset < buffer.length) {
			buffer[offset++] = (byte) b;
		}
		if (offset == buffer.length)
			flush();
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		if (offset > 0) {
			connection.service.write(connection.handle, position, buffer, 0, offset, new DoneWrite() {
				@Override
				public void doneWrite(IToken token, FileSystemException error) {
					if (error != null) {
						ERROR = error;
					}
					position += offset;
					offset = 0;
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		if (!closed) {
			connection.closeStream(this);
			closed = true;
		}
	}
}
