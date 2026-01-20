/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.cache.wan.internal;

import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.client.internal.PoolImpl;
import org.apache.geode.cache.wan.GatewayReceiver;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.PoolFactoryImpl;
import org.apache.geode.internal.cache.wan.AbstractGatewaySender;
import org.apache.geode.internal.cache.wan.GatewaySenderAttributes;
import org.apache.geode.internal.statistics.StatisticsClock;
import org.apache.geode.logging.internal.log4j.api.LogService;

/**
 * Abstract implementation of a GatewaySender that connects to a fixed hostname:port
 * instead of discovering receivers through locators.
 *
 * @since Geode 1.16
 */
public abstract class AbstractFixedAddressGatewaySender extends AbstractGatewaySender {
  private static final Logger logger = LogService.getLogger();

  protected final String remoteReceiverHostname;
  protected final int remoteReceiverPort;

  public AbstractFixedAddressGatewaySender(InternalCache cache, StatisticsClock statisticsClock,
      GatewaySenderAttributes attrs) {
    super(cache, statisticsClock, attrs);
    this.remoteReceiverHostname = attrs.getRemoteReceiverHostname();
    this.remoteReceiverPort = attrs.getRemoteReceiverPort();
  }

  /**
   * Returns the hostname of the remote receiver.
   *
   * @return the remote receiver hostname
   */
  public String getRemoteReceiverHostname() {
    return remoteReceiverHostname;
  }

  /**
   * Returns the port of the remote receiver.
   *
   * @return the remote receiver port
   */
  public int getRemoteReceiverPort() {
    return remoteReceiverPort;
  }

  @Override
  public synchronized void initProxy() {
    // return if proxy is already created
    if (proxy != null && !proxy.isDestroyed()) {
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Gateway Sender {} is configuring pool to connect to fixed address {}:{}",
          getId(), remoteReceiverHostname, remoteReceiverPort);
    }

    PoolFactoryImpl pf = (PoolFactoryImpl) PoolManager.createFactory();
    pf.setPRSingleHopEnabled(false);
    if (locatorDiscoveryCallback != null) {
      pf.setLocatorDiscoveryCallback(locatorDiscoveryCallback);
    }
    pf.setReadTimeout(socketReadTimeout);
    pf.setIdleTimeout(connectionIdleTimeOut);
    pf.setSocketBufferSize(socketBufferSize);
    pf.setServerGroup(GatewayReceiver.RECEIVER_GROUP);

    // Add the fixed server address instead of using locator discovery
    pf.addServer(remoteReceiverHostname, remoteReceiverPort);

    pf.init(this);
    proxy = ((PoolImpl) pf.create(getId()));

    logger.info(
        "GatewaySender {} created pool to connect to fixed address {}:{}",
        getId(), remoteReceiverHostname, remoteReceiverPort);
  }
}
