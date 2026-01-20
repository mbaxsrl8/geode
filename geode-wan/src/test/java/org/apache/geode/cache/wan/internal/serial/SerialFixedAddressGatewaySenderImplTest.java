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
package org.apache.geode.cache.wan.internal.serial;

import static org.apache.geode.cache.wan.GatewaySender.DEFAULT_DISTRIBUTED_SYSTEM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.Statistics;
import org.apache.geode.StatisticsFactory;
import org.apache.geode.distributed.DistributedLockService;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.InternalRegionFactory;
import org.apache.geode.internal.cache.LocalRegion;
import org.apache.geode.internal.cache.wan.AbstractGatewaySenderEventProcessor;
import org.apache.geode.internal.cache.wan.GatewaySenderAdvisor;
import org.apache.geode.internal.cache.wan.GatewaySenderAttributes;
import org.apache.geode.internal.statistics.StatisticsClock;
import org.apache.geode.test.fake.Fakes;
import org.apache.geode.test.junit.categories.WanTest;

@Category(WanTest.class)
public class SerialFixedAddressGatewaySenderImplTest {

  private static final String TEST_HOSTNAME = "testhost.example.com";
  private static final int TEST_PORT = 5678;

  private InternalCache cache;

  private SerialFixedAddressGatewaySenderImpl fixedAddressSender;
  private StatisticsFactory statisticsFactory;
  private GatewaySenderAttributes gatewaySenderAttributes;
  private StatisticsClock statisticsClock;
  private InternalRegionFactory regionFactory;

  AbstractGatewaySenderEventProcessor eventProcessor1;
  AbstractGatewaySenderEventProcessor eventProcessor2;


  @Before
  public void setUp() throws Exception {
    cache = Fakes.cache();
    when(cache.getRegion(any())).thenReturn(null);
    regionFactory = mock(InternalRegionFactory.class);
    when(regionFactory.create(any())).thenReturn(mock(LocalRegion.class));
    when(cache.createInternalRegionFactory(any())).thenReturn(regionFactory);

    statisticsFactory = mock(StatisticsFactory.class);
    when(statisticsFactory.createAtomicStatistics(any(), any())).thenReturn(mock(Statistics.class));

    gatewaySenderAttributes = mock(GatewaySenderAttributes.class);
    when(gatewaySenderAttributes.getId()).thenReturn("sender");
    when(gatewaySenderAttributes.getRemoteDSId()).thenReturn(DEFAULT_DISTRIBUTED_SYSTEM_ID);
    when(gatewaySenderAttributes.getMaximumQueueMemory()).thenReturn(10);
    when(gatewaySenderAttributes.getDispatcherThreads()).thenReturn(1);
    when(gatewaySenderAttributes.isForInternalUse()).thenReturn(false);
    when(gatewaySenderAttributes.getRemoteReceiverHostname()).thenReturn(TEST_HOSTNAME);
    when(gatewaySenderAttributes.getRemoteReceiverPort()).thenReturn(TEST_PORT);

    statisticsClock = mock(StatisticsClock.class);

    DistributedLockService distributedLockService = mock(DistributedLockService.class);
    when(distributedLockService.lock(any(), anyLong(), anyLong())).thenReturn(true);
    when(cache.getGatewaySenderLockService()).thenReturn(distributedLockService);
  }

  private SerialFixedAddressGatewaySenderImpl createSerialFixedAddressGatewaySenderImplSpy() {
    GatewaySenderAdvisor gatewaySenderAdvisor = mock(GatewaySenderAdvisor.class);
    when(gatewaySenderAdvisor.isPrimary()).thenReturn(true);

    eventProcessor1 = mock(AbstractGatewaySenderEventProcessor.class);
    eventProcessor2 = mock(AbstractGatewaySenderEventProcessor.class);

    when(eventProcessor1.isStopped()).thenReturn(false);
    when(eventProcessor1.getRunningStateLock()).thenReturn(mock(Object.class));

    when(eventProcessor2.isStopped()).thenReturn(false);
    when(eventProcessor2.getRunningStateLock()).thenReturn(mock(Object.class));

    SerialFixedAddressGatewaySenderImpl sender =
        new SerialFixedAddressGatewaySenderImpl(cache, statisticsClock, gatewaySenderAttributes);
    SerialFixedAddressGatewaySenderImpl spySender = spy(sender);
    doReturn(gatewaySenderAdvisor).when(spySender).getSenderAdvisor();
    doReturn(eventProcessor1).when(spySender).createEventProcessor(false);
    doReturn(eventProcessor2).when(spySender).createEventProcessor(true);

    doReturn(null).when(spySender).getQueues();

    return spySender;
  }

  @Test
  public void whenStartedShouldCreateEventProcessor() {
    fixedAddressSender = createSerialFixedAddressGatewaySenderImplSpy();

    fixedAddressSender.start();

    assertThat(fixedAddressSender.getEventProcessor()).isEqualTo(eventProcessor1);
  }

  @Test
  public void whenStartedWithCleanShouldCreateEventProcessor() {
    fixedAddressSender = createSerialFixedAddressGatewaySenderImplSpy();

    fixedAddressSender.startWithCleanQueue();

    assertThat(fixedAddressSender.getEventProcessor()).isEqualTo(eventProcessor2);
  }

  @Test
  public void whenStoppedShouldResetTheEventProcessor() {
    fixedAddressSender = createSerialFixedAddressGatewaySenderImplSpy();

    fixedAddressSender.stop();

    assertThat(fixedAddressSender.getEventProcessor()).isNull();
  }

  @Test
  public void getRemoteReceiverHostnameReturnsConfiguredHostname() {
    fixedAddressSender = createSerialFixedAddressGatewaySenderImplSpy();

    assertThat(fixedAddressSender.getRemoteReceiverHostname()).isEqualTo(TEST_HOSTNAME);
  }

  @Test
  public void getRemoteReceiverPortReturnsConfiguredPort() {
    fixedAddressSender = createSerialFixedAddressGatewaySenderImplSpy();

    assertThat(fixedAddressSender.getRemoteReceiverPort()).isEqualTo(TEST_PORT);
  }

  @Test
  public void toStringContainsHostnameAndPort() {
    fixedAddressSender = createSerialFixedAddressGatewaySenderImplSpy();

    String toStringResult = fixedAddressSender.toString();

    assertThat(toStringResult).contains("SerialFixedAddressGatewaySender");
    assertThat(toStringResult).contains("remoteReceiverHostname=" + TEST_HOSTNAME);
    assertThat(toStringResult).contains("remoteReceiverPort=" + TEST_PORT);
  }
}
