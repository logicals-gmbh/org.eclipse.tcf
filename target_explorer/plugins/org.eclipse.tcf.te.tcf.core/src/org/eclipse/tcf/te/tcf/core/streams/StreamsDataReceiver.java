/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.streams;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;

/**
 * Remote context streams data receiver implementation.
 */
public class StreamsDataReceiver extends PlatformObject {
	// The associated writer instance
	private final Writer writer;
	// The list of applicable stream type id's
	private final List<String> streamTypeIds;
	// The list of registered listener
	private final ListenerList listeners;

	/**
	 * An interface to be implemented by listeners who want to listen
	 * to the streams data without interfering with the original data receiver.
	 * <p>
	 * Listeners are asynchronously invoked in the TCF dispatch thread.
	 */
	public static interface Listener {

		/**
		 * Signals that some data has been received by this streams data
		 * receiver.
		 *
		 * @param data The data received. Must not be <code>null</code>.
		 */
		public void dataReceived(String data);
	}

	/**
	 * Constructor.
	 *
	 * @param writer The writer instance. Must not be <code>null</code>.
	 * @param streamTypeIds The list of applicable stream type id's or <code>null</code>.
	 *
	 * @see IProcesses
	 */
	public StreamsDataReceiver(Writer writer, String[] streamTypeIds) {
		Assert.isNotNull(writer);
		this.writer = writer;
		this.streamTypeIds = streamTypeIds != null ? Arrays.asList(streamTypeIds) : null;
		this.listeners = new ListenerList();
	}

	/**
	 * Register a streams data receiver listener.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public final void addListener(Listener listener) {
		Assert.isNotNull(listener);
		listeners.add(listener);
	}

	/**
	 * Unregister a streams data receiver listener.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public final void removeListener(Listener listener) {
		Assert.isNotNull(listener);
		listeners.remove(listener);
	}

	/**
	 * Notify registered streams data receiver listener.
	 *
	 * @param data The data received. Must not be <code>null</code>.
	 */
	public final void notifyListener(final String data) {
		Assert.isNotNull(data);

		final Object[] listeners = this.listeners.getListeners();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				for (Object listener : listeners) {
					Assert.isTrue(listener instanceof Listener);
					((Listener)listener).dataReceived(data);
				}
			}
		});
	}

	/**
	 * Dispose the data receiver instance.
	 */
	public void dispose() {
		listeners.clear();

		try {
			writer.close();
		}
		catch (IOException e) {
			/* ignored on purpose */
		}
	}

	/**
	 * Returns the associated writer instance.
	 *
	 * @return The associated writer instance.
	 */
	public final Writer getWriter() {
		return writer;
	}

	/**
	 * Returns if or if not the given stream type id is applicable for this data receiver.
	 *
	 * @param streamTypeId The stream type id. Must not be <code>null</code>.
	 * @return <code>True</code> if the given stream type id is applicable for this data receiver, <code>false</code>
	 *         otherwise.
	 */
	public final boolean isApplicable(String streamTypeId) {
		Assert.isNotNull(streamTypeId);
		return streamTypeIds == null || streamTypeIds.contains(streamTypeId);
	}
}
