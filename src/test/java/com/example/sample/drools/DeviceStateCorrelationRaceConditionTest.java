package com.example.sample.drools;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;
import org.drools.core.common.DefaultFactHandle;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.event.DebugAgendaEventListener;
import org.drools.core.event.DebugRuleRuntimeEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieSession;
import org.opennms.netmgt.config.EventTranslatorConfigFactory;
import org.opennms.netmgt.correlation.drools.DeviceOfflineAffliction;
import org.opennms.netmgt.correlation.drools.DroolsCorrelationEngine;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;

public class DeviceStateCorrelationRaceConditionTest extends CorrelationRulesTestCase {
  private DroolsCorrelationEngine m_engine;
  private KieRuntimeLogger logger;
  private String hwAddrPrefix;

  @Before
  public void setUp() {
    getAnticipator().reset();
    m_engine = findEngineByName("deviceStateTrackingRaceConditionRules");
    m_engine.getKieSession().addEventListener(new DebugAgendaEventListener());
    m_engine.getKieSession().addEventListener(new DebugRuleRuntimeEventListener());
    logger = KieServices.Factory.get().getLoggers().newThreadedFileLogger(m_engine.getKieSession(),
        "./target/DeviceStateCorrelationRaceConditionTest.log", 1000);
    hwAddrPrefix = "B5-60-4D-1D-BF-";
  }

  @After
  public void tearDown() {
    m_engine.getKieSession().getFactHandles().forEach(fh -> m_engine.getKieSession().delete(fh));
    logger.close();
  }

  @Test
  public void testSingleOfflineEvent() throws InterruptedException {
    m_anticipatedMemorySize = 1;
    correlateOfflineEvents(1, 0, false);
    // if we don't sleep here, Drools won't create the affliction in time for
    // the following assertions.
    Thread.sleep(500);
    verify(m_engine);
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    ArrayList<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Wrong offline device remains in tracker", "device01", offlineDevices.get(0));
  }

  @Test
  public void testSingleUnenrichedOfflineEvent() throws InterruptedException {
    m_anticipatedMemorySize = 1;
    correlateUnenrichedOfflineEvents(1, 0, false);
    // if we don't sleep here, Drools won't create the affliction in time for
    // the following assertions.
    Thread.sleep(500);
    verify(m_engine);
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    ArrayList<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Wrong offline device remains in tracker", "ab1234cd01", offlineDevices.get(0));
  }

  @Test
  public void testMultipleOfflineAndAdoptedEvents() throws InterruptedException {
    m_anticipatedMemorySize = 1;
    // insert a set of offline events
    correlateOfflineEvents(15, 14, false);
    // insert a set of adopted events
    correlateAdoptedEvents(15, 8, false);

    // if we don't sleep here, Drools won't have correlated all of the events in time for
    // the following assertions.
    Thread.sleep(500);
    verify(m_engine);
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertEquals("The number of offline devices is wrong \n" + aff.getOfflineDevices(), 1,
        aff.getOfflineDevices().size());
    ArrayList<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Incorrect number of remaining offline devices", 1, offlineDevices.size());
    assertEquals("Wrong offline device remains in tracker", "device08", offlineDevices.get(0));
  }

  @Test
  public void testMultipleUnenrichedOfflineAndAdoptedEvents() throws InterruptedException {
    m_anticipatedMemorySize = 1;
    // insert a set of offline events
    correlateUnenrichedOfflineEvents(15, 14, false);
    // insert a set of adopted events
    correlateUnenrichedAdoptedEvents(15, 8, false);

    // if we don't sleep here, Drools won't have correlated all of the events in time for
    // the following assertions.
    Thread.sleep(500);
    verify(m_engine);
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertEquals("The number of offline devices is wrong \n" + aff.getOfflineDevices(), 1,
        aff.getOfflineDevices().size());
    ArrayList<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Incorrect number of remaining offline devices", 1, offlineDevices.size());
    assertEquals("Wrong offline device remains in tracker", "ab1234cd08", offlineDevices.get(0));
  }

  private DeviceOfflineAffliction getDeviceOfflineAffliction() {
    KieSession kieSession = m_engine.getKieSession();
    return kieSession.getFactHandles().stream()
        .filter(fh -> fh.getClass().getSimpleName().equals(DefaultFactHandle.class.getSimpleName()))
        .map(InternalFactHandle.class::cast)
        .map(InternalFactHandle::getObject)
        .map(DeviceOfflineAffliction.class::cast)
        .collect(Collectors.toList()).get(0);
  }

  private void correlateOfflineEvents(int count, int skip, boolean threadSleep) throws InterruptedException {
    final Event templateOfflineEvent = new EventBuilder("DeviceOffline", "tests")
        .addParam("location", "loc001").getEvent();
    for (int i = 1; i <= count; i++) {
      if (i == skip) {
        continue;
      }
      Event offlineEvent =
          new EventBuilder(EventTranslatorConfigFactory.cloneEvent(templateOfflineEvent))
              .setTime(new Date())
              .setNodeid(i)
              .addParam("deviceName", "device" + StringUtils.leftPad(String.valueOf(i), 2, "0"))
              .getEvent();
      m_engine.correlate(offlineEvent);
      if (threadSleep) {
        Thread.sleep(500);
      }
    }
  }

  private void correlateAdoptedEvents(int count, int skip, boolean threadSleep) throws InterruptedException {
    final Event templateAdoptedEvent = new EventBuilder("DeviceAdopted", "tests")
        .addParam("location", "loc001").getEvent();

    for (int i = 1; i <= count; i++) {
      if (i == skip) {
        continue;
      }
      Event adoptedEvent =
          new EventBuilder(EventTranslatorConfigFactory.cloneEvent(templateAdoptedEvent))
              .setTime(new Date())
              .setNodeid(i)
              .addParam("deviceName", "device" + StringUtils.leftPad(String.valueOf(i), 2, "0"))
              .getEvent();
      m_engine.correlate(adoptedEvent);
      if (threadSleep) {
        Thread.sleep(500);
      }
    }
  }

  private void correlateUnenrichedOfflineEvents(int count, int skip, boolean threadSleep) throws InterruptedException {
    final Event templateOfflineEvent = new EventBuilder("DeviceOffline", "tests").getEvent();

    for (int i = 1; i <= count; i++) {
      if (i == skip) {
        continue;
      }
      String paddedCounter = StringUtils.leftPad(String.valueOf(i), 2, "0");
      String counterAsHex = StringUtils.leftPad(Integer.toHexString(i), 2, "0");
      String eventDescription = new StringBuilder("Device ").append(hwAddrPrefix).append(counterAsHex)
          .append("(loc002ab1234cd").append(paddedCounter).append(")")
          .append(" is offline, last seen:5 minutes ago")
          .toString();
      Event offlineEvent =
          new EventBuilder(EventTranslatorConfigFactory.cloneEvent(templateOfflineEvent))
              .setTime(new Date())
              .setNodeid(i)
              .addParam("eventDescription", eventDescription)
              .getEvent();
      m_engine.correlate(offlineEvent);
      if (threadSleep) {
        Thread.sleep(500);
      }
    }
  }

  private void correlateUnenrichedAdoptedEvents(int count, int skip, boolean threadSleep) throws InterruptedException {
    final Event templateAdoptedEvent = new EventBuilder("DeviceAdopted", "tests").getEvent();

    for (int i = 1; i <= count; i++) {
      if (i == skip) {
        continue;
      }
      String paddedCounter = StringUtils.leftPad(String.valueOf(i), 2, "0");
      String counterAsHex = StringUtils.leftPad(Integer.toHexString(i), 2, "0");
      String eventDescription = new StringBuilder("Device('loc002ab1234cd").append(paddedCounter)
          .append("'/'ab1234'/").append(hwAddrPrefix).append(counterAsHex).append(")")
          .append(" at rf-domain:'loc002' adopted and configured").toString();
      Event adoptedEvent =
          new EventBuilder(EventTranslatorConfigFactory.cloneEvent(templateAdoptedEvent))
              .setTime(new Date())
              .setNodeid(i)
              .addParam("eventDescription", eventDescription)
              .getEvent();
      m_engine.correlate(adoptedEvent);
      if (threadSleep) {
        Thread.sleep(500);
      }
    }
  }
}