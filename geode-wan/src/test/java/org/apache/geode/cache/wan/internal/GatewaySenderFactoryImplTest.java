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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.Statistics;
import org.apache.geode.StatisticsFactory;
import org.apache.geode.cache.wan.GatewaySender;
import org.apache.geode.cache.wan.internal.parallel.ParallelGatewaySenderImpl;
import org.apache.geode.cache.wan.internal.serial.SerialFixedAddressGatewaySenderImpl;
import org.apache.geode.cache.wan.internal.serial.SerialGatewaySenderImpl;
import org.apache.geode.distributed.DistributedLockService;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.InternalRegionFactory;
import org.apache.geode.internal.cache.LocalRegion;
import org.apache.geode.internal.statistics.StatisticsClock;
import org.apache.geode.test.fake.Fakes;
import org.apache.geode.test.junit.categories.WanTest;

@Category(WanTest.class)
public class GatewaySenderFactoryImplTest {

  private static final String TEST_HOSTNAME = "testhost.example.com";
  private static final int TEST_PORT = 5678;
  private static final String SENDER_ID = "testSender";
  private static final int REMOTE_DS_ID = 2;

  private InternalCache cache;
  private StatisticsClock statisticsClock;
  private GatewaySenderFactoryImpl factory;

  @Before
  public void setUp() throws Exception {
    cache = Fakes.cache();
    when(cache.getRegion(any())).thenReturn(null);

    InternalRegionFactory regionFactory = mock(InternalRegionFactory.class);
    when(regionFactory.create(any())).thenReturn(mock(LocalRegion.class));
    when(cache.createInternalRegionFactory(any())).thenReturn(regionFactory);

    StatisticsFactory statisticsFactory = mock(StatisticsFactory.class);
    when(statisticsFactory.createAtomicStatistics(any(), any())).thenReturn(mock(Statistics.class));

    statisticsClock = mock(StatisticsClock.class);

    DistributedLockService distributedLockService = mock(DistributedLockService.class);
    when(distributedLockService.lock(any(), anyLong(), anyLong())).thenReturn(true);
    when(cache.getGatewaySenderLockService()).thenReturn(distributedLockService);

    InternalDistributedSystem system = mock(InternalDistributedSystem.class);
    DistributionManager dm = mock(DistributionManager.class);
    when(dm.getDistributedSystemId()).thenReturn(1);
    when(system.getDistributionManager()).thenReturn(dm);
    DistributionConfig config = mock(DistributionConfig.class);
    when(config.getLocators()).thenReturn("localhost[10334]");
    when(system.getConfig()).thenReturn(config);
    when(cache.getInternalDistributedSystem()).thenReturn(system);

    factory = new GatewaySenderFactoryImpl(cache, statisticsClock);
  }

  @Test
  public void setReceiverHostnameAndPortConfiguresFixedAddressInFactory() {
    factory.setReceiverHostname(TEST_HOSTNAME);
    factory.setReceiverPort(TEST_PORT);

    // Use manual start to prevent actually starting the sender
    factory.setManualStart(true);

    GatewaySender sender = factory.create(SENDER_ID, REMOTE_DS_ID);

    assertThat(sender).isInstanceOf(SerialFixedAddressGatewaySenderImpl.class);
    SerialFixedAddressGatewaySenderImpl fixedAddressSender =
        (SerialFixedAddressGatewaySenderImpl) sender;
    assertThat(fixedAddressSender.getRemoteReceiverHostname()).isEqualTo(TEST_HOSTNAME);
    assertThat(fixedAddressSender.getRemoteReceiverPort()).isEqualTo(TEST_PORT);
  }

  @Test
  public void serialSenderWithoutFixedAddressCreatesSerialGatewaySender() {
    factory.setManualStart(true);

    GatewaySender sender = factory.create(SENDER_ID, REMOTE_DS_ID);

    assertThat(sender).isInstanceOf(SerialGatewaySenderImpl.class);
  }

  @Test
  public void parallelSenderWithoutFixedAddressCreatesParallelGatewaySender() {
    factory.setParallel(true);
    factory.setManualStart(true);

    GatewaySender sender = factory.create(SENDER_ID, REMOTE_DS_ID);

    assertThat(sender).isInstanceOf(ParallelGatewaySenderImpl.class);
  }

  @Test
  public void senderWithOnlyHostnameDoesNotCreateFixedAddressSender() {
    factory.setReceiverHostname(TEST_HOSTNAME);
    // No port set
    factory.setManualStart(true);

    GatewaySender sender = factory.create(SENDER_ID, REMOTE_DS_ID);

    assertThat(sender).isInstanceOf(SerialGatewaySenderImpl.class);
  }

  @Test
  public void senderWithOnlyPortDoesNotCreateFixedAddressSender() {
    // No hostname set
    factory.setReceiverPort(TEST_PORT);
    factory.setManualStart(true);

    GatewaySender sender = factory.create(SENDER_ID, REMOTE_DS_ID);

    assertThat(sender).isInstanceOf(SerialGatewaySenderImpl.class);
  }
}
