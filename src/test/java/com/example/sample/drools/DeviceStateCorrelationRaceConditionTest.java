package com.example.sample.drools;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import org.opennms.netmgt.correlation.drools.DroolsCorrelationEngine;
import org.opennms.netmgt.correlation.drools.DeviceOfflineAffliction;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.AlarmData;
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
        "./target/DeviceStateCorrelationRaceConditionTest", 1000);
    hwAddrPrefix = "B5-60-4D-1D-BF-";
  }

  @After
  public void tearDown() {
    logger.close();
    m_engine.getKieSession().getAgendaEventListeners()
        .forEach(ael -> m_engine.getKieSession().removeEventListener(ael));
    m_engine.getKieSession().getRuleRuntimeEventListeners()
        .forEach(rel -> m_engine.getKieSession().removeEventListener(rel));
    m_engine.getKieSession().getFactHandles().forEach(fh -> m_engine.getKieSession().delete(fh));
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

  /**
   * Insert 14 deviceOffline events, followed by 14 deviceAdopted events.
   * Do not sleep between event insertions.
   * The deviceOffline event set skips device number 14.
   * The deviceAdopted event set skips device number 8.
   * Attempting to remove a device from the DeviceOfflineAffliction that is not
   * in its offlineDevices list should cause a failure.
   *
   * At the end of the test, only device number 8 should remain in the
   * DeviceOfflineAffliction's offlineDevices list.
   * 
   * @throws InterruptedException
   */
  @Test
  public void testMultipleOfflineAndAdoptedEventsNoSleepBetweenEvents() throws InterruptedException {
    // insert a set of offline events
    correlateOfflineEvents(15, 14, false);
    // insert a set of adopted events
    correlateAdoptedEvents(15, 8, false);

    // if we don't sleep here, Drools won't have correlated all of the events in time for
    // the following assertions.
    Thread.sleep(500);
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    ArrayList<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Incorrect number of remaining offline devices", 1, offlineDevices.size());
    assertEquals("Wrong offline device remains in tracker", "device08", offlineDevices.get(0));
  }

  /**
   * Insert 14 deviceOffline events, followed by 14 deviceAdopted events.
   * Sleep for 500ms between each event insertion.
   * The deviceOffline event set skips device number 14.
   * The deviceAdopted event set skips device number 8.
   * Attempting to remove a device from the DeviceOfflineAffliction that is not
   * in its offlineDevices list should cause a failure.
   *
   * At the end of the test, only device number 8 should remain in the
   * DeviceOfflineAffliction's offlineDevices list.
   *
   * @throws InterruptedException
   */
  @Test
  public void testMultipleOfflineAndAdoptedEventsWithSleepBetweenEachEvent() throws InterruptedException {
    // insert a set of offline events
    correlateOfflineEvents(15, 14, true);
    // insert a set of adopted events
    correlateAdoptedEvents(15, 8, true);

    // if we don't sleep here, Drools won't have correlated all of the events in time for
    // the following assertions.
    Thread.sleep(500);
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    ArrayList<String> offlineDevices = new ArrayList<>(aff.getOfflineDevices());
    assertEquals("Incorrect number of remaining offline devices", 1, offlineDevices.size());
    assertEquals("Wrong offline device remains in tracker", "device08", offlineDevices.get(0));
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
    final Event templateOfflineEvent = new EventBuilder("uei.opennms.org/tests/trapDeviceOffline", "tests")
        .addParam("location", "loc001")
        .addParam("parseHwAddrFailed","false")
        .addParam("reductionKey","uei.opennms.org/tests/trapDeviceOffline:loc001:Warning")
        .getEvent();
    for (int i = 1; i <= count; i++) {
      if (i == skip) {
        continue;
      }
      Event offlineEvent =
          new EventBuilder(EventTranslatorConfigFactory.cloneEvent(templateOfflineEvent))
              .setTime(new Date())
              .setNodeid(i)
              .setSeverity("Warning")
              .addParam("opennms_traceId",UUID.randomUUID().toString())
              .addParam("trapMsg", "not an empty string")
              .addParam("deviceName", "device" + StringUtils.leftPad(String.valueOf(i), 2, "0"))
              .getEvent();
      offlineEvent.setDbid(0);
      m_engine.correlate(offlineEvent);
      if (threadSleep) {
        Thread.sleep(500);
      }
    }
  }

  private void correlateAdoptedEvents(int count, int skip, boolean threadSleep) throws InterruptedException {
    final Event templateAdoptedEvent = new EventBuilder("uei.opennms.org/tests/trapDeviceAdopted", "tests")
        .addParam("location", "loc001")
        .addParam("parseHwAddrFailed","false")
        .addParam("reductionKey", "uei.opennms.org/tests/trapDeviceAdopted:loc001:Normal")
        .addParam("clearKey", "uei.opennms.org/tests/trapDeviceAdopted:loc001")
        .getEvent();

    for (int i = 1; i <= count; i++) {
      if (i == skip) {
        continue;
      }
      AlarmData alarmData = new AlarmData();
      alarmData.setAlarmType(2);
      alarmData.setClearKey(templateAdoptedEvent.getParm("clearKey").getValue().getContent());
      alarmData.setReductionKey(templateAdoptedEvent.getParm("reductionKey").getValue().getContent());
      Event adoptedEvent =
          new EventBuilder(EventTranslatorConfigFactory.cloneEvent(templateAdoptedEvent))
              .setTime(new Date())
              .setNodeid(i)
              .setSeverity("Normal")
              .addParam("opennms_traceId",UUID.randomUUID().toString())
              .addParam("trapMsg", "not an empty string")
              .addParam("deviceName", "device" + StringUtils.leftPad(String.valueOf(i), 2, "0"))
              .setAlarmData(alarmData)
              .getEvent();
      adoptedEvent.setDbid(0);
      m_engine.correlate(adoptedEvent);
      if (threadSleep) {
        Thread.sleep(500);
      }
    }
  }
}