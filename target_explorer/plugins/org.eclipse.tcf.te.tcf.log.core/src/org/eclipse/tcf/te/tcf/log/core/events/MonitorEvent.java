/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.log.core.events;

import java.util.EventObject;

/**
 * Monitor event implementation.
 */
public class MonitorEvent extends EventObject {
    private static final long serialVersionUID = 2503050052196826683L;

	/**
	 * Monitor event types.
	 */
	public static enum Type { OPEN, ACTIVITY, CLOSE }

	/**
	 * Immutable monitor event message.
	 */
	public static final class Message {
		/** The message type */
		public final char type;
		/** The message text */
		public final String text;

		/**
         * Constructor.
         */
        public Message(char type, String text) {
        	this.type = type;
        	this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
        	StringBuilder buffer = new StringBuilder(getClass().getName());
        	buffer.append(": type = '"); //$NON-NLS-1$
        	buffer.append(type);
        	buffer.append("', text = '"); //$NON-NLS-1$
        	buffer.append(text);
        	buffer.append("'"); //$NON-NLS-1$

            return buffer.toString();
        }
	}


	private Type type;
	private Message message;

	/**
	 * Constructor.
	 *
	 * @param source The source object. Must not be <code>null</code>.
	 * @param type The event type. Must not be <code>null</code>.
	 * @param message The event message or <code>null</code>.
	 *
	 * @exception IllegalArgumentException if type == null.
	 */
	public MonitorEvent(Object source, Type type, Message message) {
		super(source);

		if (type == null) throw new IllegalArgumentException("null type"); //$NON-NLS-1$
		this.type = type;

		this.message = message;
	}

	/**
	 * Returns the event type.
	 *
	 * @return The event type.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the event message.
	 *
	 * @return The event message or <code>null</code>.
	 */
	public Message getMessage() {
		return message;
	}
}
