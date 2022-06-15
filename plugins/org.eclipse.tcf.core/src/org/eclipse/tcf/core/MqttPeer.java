// Copyright (C) logi.cals GmbH. All rights reserved.
package org.eclipse.tcf.core;

import java.util.Map;

/**
 * MqttPeer extends TransientPeer
 *
 * @since 1.7
 */
public class MqttPeer extends TransientPeer
{
  public static final String ATTR_DEVICE_ID = "DeviceId";
  public static final String ATTR_BROKER_ID = "BrokerId";
  public static final String ATTR_PASSWORD = "Password";
  public static final String ATTR_SESSION_ID = "SessionId";
  public static final String ATTR_TCF_TOPIC = "tcfTopic";

  public MqttPeer(final Map<String, String> attrs)
  {
    super(attrs);
  }

}
