/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.interfaces.properties;

import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A generic properties container.
 */
public interface IPropertiesContainer extends IAdaptable {

	/**
	 * If a property key ends with ".silent", no change events will be fired.
	 */
	public static final String SILENT_PROPERTY = ".silent"; //$NON-NLS-1$

	/**
	 * If a property key ends with ".persistent", it should be persisted when the properties container is saved.
	 * This string should always be used at the end of all other property key extensions!
	 */
	public static final String PERSISTENT_PROPERTY = ""; //$NON-NLS-1$

	/**
	 * Returns the unique identified of the properties container.
	 *
	 * @return The unique identifier.
	 */
	public UUID getUUID();

	/**
	 * Set if or if not firing change events is enabled.
	 *
	 * @param param enabled <code>True</code> to enable the change events, <code>false</code> to disable the change events.
	 * @return <code>True</code> if the enablement has changed, <code>false</code> if not.
	 */
	public boolean setChangeEventsEnabled(boolean enabled);

	/**
	 * Returns if or if not firing change events is enabled.
	 *
	 * @return <code>True</code> if change events are enabled, <code>false</code> if disabled.
	 */
	public boolean changeEventsEnabled();

	/**
	 * Fires a change event with the given parameter.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param oldValue The old properties value, or <code>null</code>.
	 * @param newValue The new properties value, or <code>null</code>.
	 */
	public void fireChangeEvent(String key, Object oldValue, Object newValue);

	/**
	 * Set the properties from the given map. Calling this method
	 * will overwrite all previous set properties.
	 * <p>
	 * <b>Note:</b> The method will have no effect if the given properties are the
	 * same as the already set properties.
	 *
	 * @param properties The map of properties to set. Must not be <code>null</code>.
	 */
	public void setProperties(Map<String, Object> properties);

	/**
	 * Adds all properties from the given map. If a property already exist
	 * in the properties container, than the value of the property is overwritten.
	 *
	 * @param properties The map of properties to add. Must not be <code>null</code>.
	 */
	public void addProperties(Map<String, ?> properties);

	/**
	 * Stores the property under the given property key using the given property value.
	 * If the current property value is equal to the given property value, no store
	 * operation will be executed. If the property value is not <code>null</code> and
	 * is different from the current property value, the new value will be written to
	 * the property store and a property change event is fired. If the property value
	 * is <code>null</code>, the property key and the currently stored value are removed
	 * from the property store.
	 *
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @param value The property value.
	 * @return <code>true</code> if the property value had been applied to the property store, <code>false</code> otherwise.
	 */
	public boolean setProperty(String key, Object value);

	/**
	 * Stores the property under the given property key using the given long
	 * property value. The given long value is transformed to an <code>Long</code>
	 * object and stored to the properties store via <code>setProperty(java.lang.String, java.lang.Object)</code>.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @param value The property value.
	 * @return <code>true</code> if the property value had been applied to the property store, <code>false</code> otherwise.
	 *
	 * @see <code>setProperty(java.lang.String, java.lang.Object)</code>
	 */
	public boolean setProperty(String key, long value);

	/**
	 * Stores the property under the given property key using the given integer
	 * property value. The given integer value is transformed to an <code>Integer</code>
	 * object and stored to the properties store via <code>setProperty(java.lang.String, java.lang.Object)</code>.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @param value The property value.
	 * @return <code>true</code> if the property value had been applied to the property store, <code>false</code> otherwise.
	 *
	 * @see <code>setProperty(java.lang.String, java.lang.Object)</code>
	 */
	public boolean setProperty(String key, int value);

	/**
	 * Stores the property under the given property key using the given boolean
	 * property value. The given boolean value is transformed to an <code>Boolean</code>
	 * object and stored to the properties store via <code>setProperty(java.lang.String, java.lang.Object)</code>.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @param value The property value.
	 * @return <code>true</code> if the property value had been applied to the property store, <code>false</code> otherwise.
	 *
	 * @see <code>setProperty(java.lang.String, java.lang.Object)</code>
	 */
	public boolean setProperty(String key, boolean value);

	/**
	 * Stores the property under the given property key using the given float
	 * property value. The given float value is transformed to an <code>Float</code>
	 * object and stored to the properties store via <code>setProperty(java.lang.String, java.lang.Object)</code>.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @param value The property value.
	 * @return <code>true</code> if the property value had been applied to the property store, <code>false</code> otherwise.
	 *
	 * @see <code>setProperty(java.lang.String, java.lang.Object)</code>
	 */
	public boolean setProperty(String key, float value);

	/**
	 * Stores the property under the given property key using the given double
	 * property value. The given double value is transformed to an <code>Double</code>
	 * object and stored to the properties store via <code>setProperty(java.lang.String, java.lang.Object)</code>.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @param value The property value.
	 * @return <code>true</code> if the property value had been applied to the property store, <code>false</code> otherwise.
	 *
	 * @see <code>setProperty(java.lang.String, java.lang.Object)</code>
	 */
	public boolean setProperty(String key, double value);

	/**
	 * Return all properties. The result map is read-only.
	 *
	 * @return A map containing all properties.
	 */
	public Map<String, Object> getProperties();

	/**
	 * Queries the property value stored under the given property key. If the property
	 * does not exist, <code>null</code> is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value or <code>null</code>.
	 */
	public Object getProperty(String key);

	/**
	 * Queries the property value stored under the given property key. If the property
	 * exist and is of type <code>java.lang.String</code>, the property value casted to
	 * <code>java.lang.String</code> is returned. In all other cases, <code>null</code>
	 * is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value casted <code>java.lang.String</code> or <code>null</code>.
	 */
	public String getStringProperty(String key);

	/**
	 * Queries the property value stored under the given property key. If the property
	 * exist and is of type <code>java.lang.Long</code>, the property value converted
	 * to an long value is returned. In all other cases, <code>-1</code> is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value converted to a long value or <code>-1</code>.
	 */
	public long getLongProperty(String key);

	/**
	 * Queries the property value stored under the given property key. If the property
	 * exist and is of type <code>java.lang.Integer</code>, the property value converted
	 * to an integer value is returned. In all other cases, <code>-1</code> is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value converted to an integer value or <code>-1</code>.
	 */
	public int getIntProperty(String key);

	/**
	 * Queries the property value stored under the given property key. If the property
	 * exist and is of type <code>java.lang.Boolean</code>, the property value converted
	 * to an boolean value is returned. In all other cases, <code>false</code> is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value converted to an boolean value or <code>false</code>.
	 */
	public boolean getBooleanProperty(String key);

	/**
	 * Queries the property value stored under the given property key. If the property
	 * exist and is of type <code>java.lang.Float</code>, the property value converted
	 * to an float value is returned. In all other cases, <code>Float.NaN</code> is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value converted to a float value or <code>Float.NaN</code>.
	 */
	public float getFloatProperty(String key);

	/**
	 * Queries the property value stored under the given property key. If the property
	 * exist and is of type <code>java.lang.Double</code>, the property value converted
	 * to an double value is returned. In all other cases, <code>Double.NaN</code> is returned.
	 *
	 * @param key The property key. Must not be <code>null</code>!
	 * @return The stored property value converted to a double value or <code>Double.NaN</code>.
	 */
	public double getDoubleProperty(String key);

	/**
	 * Remove all properties from the properties store. The method does not fire any
	 * properties changed event.
	 */
	public void clearProperties();

	/**
	 * Returns whether this properties container is empty or not.
	 *
	 * @return <code>True</code> if the properties container is empty, <code>false</code> if not.
	 */
	public boolean isEmpty();

	/**
	 * Returns whether this properties container contains the given key.
	 *
	 * @param key The key. Must not be <code>null</code>.
	 * @return <code>True</code> if the properties container contains the key, <code>false</code> if not.
	 */
	public boolean containsKey(String key);

	/**
	 * Test if the property value stored under the given property is equal ignoring the case to the given
	 * expected string value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property string value.
	 * @return <code>true</code> if the expected string value is equal ignoring the case to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isPropertyIgnoreCase(String key, String value);

	/**
	 * Test if the property value stored under the given property is equal to the given
	 * expected value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property value.
	 * @return <code>true</code> if the expected value is equal to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isProperty(String key, Object value);

	/**
	 * Test if the property value stored under the given property is equal to the given
	 * expected value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property value.
	 * @return <code>true</code> if the expected value is equal to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isProperty(String key, long value);

	/**
	 * Test if the property value stored under the given property is equal to the given
	 * expected value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property value.
	 * @return <code>true</code> if the expected value is equal to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isProperty(String key, int value);

	/**
	 * Test if the property value stored under the given property is equal to the given
	 * expected value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property value.
	 * @return <code>true</code> if the expected value is equal to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isProperty(String key, boolean value);

	/**
	 * Test if the property value stored under the given property is equal to the given
	 * expected value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property value.
	 * @return <code>true</code> if the expected value is equal to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isProperty(String key, float value);

	/**
	 * Test if the property value stored under the given property is equal to the given
	 * expected value.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The expected property value.
	 * @return <code>true</code> if the expected value is equal to the stored property value, <code>false</code> otherwise.
	 */
	public boolean isProperty(String key, double value);
}
