/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package com.example.sample.drools;

import static org.junit.Assert.assertEquals;
import static org.opennms.core.utils.InetAddressUtils.addr;

import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.netmgt.correlation.CorrelationEngine;
import org.opennms.netmgt.events.api.EventConstants;

import org.opennms.netmgt.correlation.CorrelationEngineRegistrar;
import org.opennms.netmgt.correlation.drools.DroolsCorrelationEngine;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.netmgt.dao.mock.EventAnticipator;
import org.opennms.netmgt.dao.mock.MockEventIpcManager;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.core.test.ConfigurationTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

/**
 * The Class CorrelationRulesTestCase.
 *
 * @author <a href="mailto:jeffg@opennms.org">Jeff Gehlbach</a>
 */
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {
      //"classpath:META-INF/opennms/applicationContext-daemon.xml",
      "classpath:META-INF/opennms/applicationContext-soa.xml",
      "classpath:META-INF/opennms/mockEventIpcManager.xml",
      "classpath:META-INF/opennms/correlation-engine.xml",
      "classpath:test-context.xml"
    })
@JUnitConfigurationEnvironment
public abstract class CorrelationRulesTestCase {

    /** The event IPC manager. */
    @Autowired
    @Qualifier("eventIpcManager")
    private MockEventIpcManager m_eventIpcMgr;

    /** The anticipated memory size. */
    protected Integer m_anticipatedMemorySize = 0;

    /** The correlator. */
    @Autowired
    @Qualifier("correlator")
    private CorrelationEngineRegistrar m_correlator;

    /**
     * Instantiates a new correlation rules test case.
     */
    protected CorrelationRulesTestCase() {
        ConfigurationTestUtils.setRelativeHomeDirectory("src/test/opennms-home");
    }

    /**
     * Sets the correlation engine registrar.
     *
     * @param correlator the new correlation engine registrar
     */
    public void setCorrelationEngineRegistrar(CorrelationEngineRegistrar correlator) {
        m_correlator = correlator;
    }

    /**
     * Verify.
     *
     * @param engine the engine
     */
    protected void verify(DroolsCorrelationEngine engine) {
        getAnticipator().verifyAnticipated(0, 0, 0, 0, 0);
        if (m_anticipatedMemorySize != null) {
            engine.getKieSessionObjects();
            assertEquals("Unexpected number of objects in working memory: "+engine.getKieSessionObjects(),
                m_anticipatedMemorySize.intValue(), engine.getKieSessionObjects().size());
        }
    }

    /**
     * Find engine by name.
     *
     * @param engineName the engine name
     * @return the drools correlation engine
     */
  protected DroolsCorrelationEngine findEngineByName(String engineName) {
    final DroolsCorrelationEngine droolsEngine = (DroolsCorrelationEngine) m_correlator.findEngineByName(engineName);
    if (droolsEngine == null) {
      StringBuilder sb = new StringBuilder();
      for (CorrelationEngine engine : m_correlator.getEngines()) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append(engine.getName());
      }
      throw new IllegalArgumentException(String.format("No engine by name '%s' found. Available engines are: ",
          engineName, sb.toString()));
    }
    return droolsEngine;
  }

    /**
     * Anticipate.
     *
     * @param event the event
     */
    protected void anticipate(Event event) {
        getAnticipator().anticipateEvent(event);
    }

    /**
     * Creates the event.
     *
     * @param uei the uei
     * @param nodeId the node id
     * @param ipAddr the ip addr
     * @param svcName the svc name
     * @param locationMonitor the location monitor
     * @return the event
     */
    protected Event createEvent(String uei, int nodeId, String ipAddr, String svcName, int locationMonitor) {
        return new EventBuilder(uei, "test")
        .setNodeid(nodeId).setInterface(addr(ipAddr))
        .setService(svcName)
        .addParam(EventConstants.PARM_LOCATION_MONITOR_ID, locationMonitor)
        .getEvent();
    }

    /**
     * Creates the service event.
     *
     * @param uei the uei
     * @param nodeId the node id
     * @param ipAddr the ip addr
     * @param svcName the svc name
     * @return the event
     */
    protected Event createServiceEvent(String uei, int nodeId, String ipAddr, String svcName) {
        return new EventBuilder(uei, "test")
        .setNodeid(nodeId).setInterface(addr("192.168.1.1"))
        .setService("HTTP")
        .getEvent();
    }

    /**
     * Gets the anticipator.
     *
     * @return the anticipator
     */
    protected EventAnticipator getAnticipator() {
        return m_eventIpcMgr.getEventAnticipator();
    }

}
