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
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.services.IProcesses;

/**
 * Remote context streams data provider implementation.
 */
public class StreamsDataProvider extends PlatformObject {
	// The associated reader instance
	private final Reader reader;
	// The list of applicable stream type id's
	private final List<String> streamTypeIds;

	/**
	 * Constructor.
	 *
	 * @param reader The reader instance. Must not be <code>null</code>.
	 * @param streamTypeIds The list of applicable stream type id's or <code>null</code>.
	 *
	 * @see IProcesses
	 */
	public StreamsDataProvider(Reader reader, String[] streamTypeIds) {
		Assert.isNotNull(reader);
		this.reader = reader;
		this.streamTypeIds = streamTypeIds != null ? Arrays.asList(streamTypeIds) : null;
	}

	/**
	 * Dispose the data provider instance.
	 */
	public void dispose() {
		try {
			reader.close();
		}
		catch (IOException e) {
			/* ignored on purpose */
		}
	}

	/**
	 * Returns the associated reader instance.
	 *
	 * @return The associated reader instance.
	 */
	public final Reader getReader() {
		return reader;
	}

	/**
	 * Returns if or if not the given stream type id is applicable for this data receiver.
	 *
	 * @param streamTypeId The stream type id. Must not be <code>null</code>.
	 * @return <code>True</code> if the given stream type id is applicable for this data receiver,
	 *         <code>false</code> otherwise.
	 */
	public final boolean isApplicable(String streamTypeId) {
		Assert.isNotNull(streamTypeId);
		return streamTypeIds == null || streamTypeIds.contains(streamTypeId);
	}
}
