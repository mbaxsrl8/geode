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

import static org.apache.geode.cache.wan.GatewaySender.DEFAULT_DISTRIBUTED_SYSTEM_ID;
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
import org.apache.geode.distributed.DistributedLockService;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.InternalRegionFactory;
import org.apache.geode.internal.cache.LocalRegion;
import org.apache.geode.internal.cache.wan.GatewaySenderAttributes;
import org.apache.geode.internal.statistics.StatisticsClock;
import org.apache.geode.test.fake.Fakes;
import org.apache.geode.test.junit.categories.WanTest;

@Category(WanTest.class)
public class AbstractFixedAddressGatewaySenderTest {

  private static final String TEST_HOSTNAME = "testhost.example.com";
  private static final int TEST_PORT = 5678;
  private static final String SENDER_ID = "testSender";

  private InternalCache cache;
  private StatisticsClock statisticsClock;
  private GatewaySenderAttributes gatewaySenderAttributes;

  @Before
  public void setUp() throws Exception {
    cache = Fakes.cache();
    when(cache.getRegion(any())).thenReturn(null);

    InternalRegionFactory regionFactory = mock(InternalRegionFactory.class);
    when(regionFactory.create(any())).thenReturn(mock(LocalRegion.class));
    when(cache.createInternalRegionFactory(any())).thenReturn(regionFactory);

    StatisticsFactory statisticsFactory = mock(StatisticsFactory.class);
    when(statisticsFactory.createAtomicStatistics(any(), any())).thenReturn(mock(Statistics.class));

    gatewaySenderAttributes = mock(GatewaySenderAttributes.class);
    when(gatewaySenderAttributes.getId()).thenReturn(SENDER_ID);
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

  @Test
  public void constructorStoresRemoteReceiverHostname() {
    TestFixedAddressGatewaySender sender =
        new TestFixedAddressGatewaySender(cache, statisticsClock, gatewaySenderAttributes);

    assertThat(sender.getRemoteReceiverHostname()).isEqualTo(TEST_HOSTNAME);
  }

  @Test
  public void constructorStoresRemoteReceiverPort() {
    TestFixedAddressGatewaySender sender =
        new TestFixedAddressGatewaySender(cache, statisticsClock, gatewaySenderAttributes);

    assertThat(sender.getRemoteReceiverPort()).isEqualTo(TEST_PORT);
  }

  /**
   * Concrete test implementation of AbstractFixedAddressGatewaySender for testing.
   */
  private static class TestFixedAddressGatewaySender extends AbstractFixedAddressGatewaySender {

    TestFixedAddressGatewaySender(InternalCache cache, StatisticsClock statisticsClock,
        GatewaySenderAttributes attrs) {
      super(cache, statisticsClock, attrs);
    }

    @Override
    public void start() {
      // no-op
    }

    @Override
    public void startWithCleanQueue() {
      // no-op
    }

    @Override
    public void prepareForStop() {
      // no-op
    }

    @Override
    public void stop() {
      // no-op
    }

    @Override
    public void setModifiedEventId(
        org.apache.geode.internal.cache.EntryEventImpl clonedEvent) {
      // no-op
    }

    @Override
    public void fillInProfile(
        org.apache.geode.distributed.internal.DistributionAdvisor.Profile profile) {
      // no-op
    }
  }
}
