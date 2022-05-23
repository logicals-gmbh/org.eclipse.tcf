/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)- [345387]Open the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.url;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem.DoneRead;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The TCF input stream returned by {@link TcfURLConnection#getInputStream()}.
 */
public class TcfInputStream extends InputStream {
	// Default chunk size while pumping the data.
	private static final int DEFAULT_CHUNK_SIZE = 5 * 1024;

	// Current reading position
	long position;
	// The byte array used to buffer data.
	byte[] buffer;
	// The offset being read in the buffer.
	int offset;

	// If the reading has reached the end of the file.
	boolean EOF;
	// If the stream has been closed.
	boolean closed;
	// The current error during reading.
	Exception ERROR;

	// The chunk size of the reading buffer.
	int chunk_size = 0;

	// The URL Connection
	TcfURLConnection connection;

	/**
	 * Create a TCF input stream connected the specified peer with specified
	 * path to the remote resource.
	 *
	 */
	public TcfInputStream(TcfURLConnection connection) {
		this(connection, DEFAULT_CHUNK_SIZE);
	}

	/**
	 * Create a TCF input stream connected the specified peer with specified
	 * path to the remote resource using the specified buffer size.
	 *
	 * @param chunk_size
	 *            The buffer size.
	 */
	public TcfInputStream(TcfURLConnection connection, int chunk_size) {
		this.connection = connection;
		this.chunk_size = chunk_size;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		if (closed)
			throw new IOException(Messages.TcfInputStream_StreamClosed);
		if (ERROR != null) {
			IOException exception = new IOException(ERROR.toString());
			exception.initCause(ERROR);
			throw exception;
		}
		if (buffer == null) {
			if (EOF) {
				return -1;
			}
			readBlock();
			return read();
		}
		if (EOF) {
			if (offset == buffer.length) {
				return -1;
			}
			// Note that convert the byte to an integer correctly
			return 0xff & buffer[offset++];
		}
		if (offset == buffer.length) {
			readBlock();
			return read();
		}
		// Note that convert the byte to an integer correctly
		return 0xff & buffer[offset++];
	}

	/**
	 * Read a block of data into the buffer. Reset the offset, increase the
	 * current position and remember the EOF status. If there's an error,
	 * remember it for read() to check.
	 */
	private void readBlock() {
		connection.service.read(connection.handle, position, chunk_size, new DoneRead() {
			@Override
			public void doneRead(IToken token, FileSystemException error, byte[] data, boolean eof) {
				if (error != null) {
					ERROR = error;
				}
				if (data == null) {
					ERROR = new IOException(Messages.TcfInputStream_NoDataAvailable);
				}
				EOF = eof;
				buffer = data;
				if (buffer != null)
					position += buffer.length;
				offset = 0;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		if (!closed) {
			connection.closeStream(this);
			closed = true;
		}
	}
}
