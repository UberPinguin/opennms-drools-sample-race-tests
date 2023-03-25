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

import static org.opennms.core.utils.InetAddressUtils.addr;

import org.junit.Test;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.correlation.drools.DroolsCorrelationEngine;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;

/**
 * The Class HoldDownRulesTest.
 * 
 * @author <a href="mailto:jeffg@opennms.org">Jeff Gehlbach</a>
 */
public class HoldDownRulesTest extends CorrelationRulesTestCase {

    /** The hold down time. */
    private static Integer HOLDDOWN_TIME = 30000;

    /** The node down past hold down UEI. */
    private static String NODE_DOWN_PAST_HOLDDOWN_UEI = "uei.opennms.org/nodes/correlation/nodeDownPastHoldDownTime";

    /** The interface down past hold down UEI. */
    private static String IFACE_DOWN_PAST_HOLDDOWN_UEI = "uei.opennms.org/nodes/correlation/interfaceDownPastHoldDownTime";

    /**
     * Test hold down rules.
     *
     * @throws Exception the exception
     */
    @Test
    public void testHoldDownRules() throws Exception {
        testHoldDownRules("nodeDownHolddownRules");
    }

    /**
     * Test hold down rules.
     *
     * @param engineName the engine name
     * @throws InterruptedException the interrupted exception
     */
    private void testHoldDownRules(String engineName) throws InterruptedException {

        getAnticipator().reset();

        // In the end, we expect to see a down-past-holddown-time event only for node one
        EventBuilder bldr = new EventBuilder( NODE_DOWN_PAST_HOLDDOWN_UEI, "Drools" );
        bldr.setNodeid( 1 );
        bldr.addParam( "holdDownTime", "30" );

        anticipate( bldr.getEvent() );

        DroolsCorrelationEngine engine = findEngineByName(engineName);

        // Node one goes down at zero seconds
        Event event = createNodeDownEvent( 1 );
        System.err.println("SENDING nodeDown EVENT FOR NODE ONE!!");
        System.err.println("MAKE IT SO, NUMBER ONE!!!");
        engine.correlate( event );

        // Node two goes down at substantially zero seconds
        event = createNodeDownEvent( 2 );
        System.err.println("SENDING nodeDown EVENT FOR NODE TWO!!");
        engine.correlate( event );

        // Wait for half the hold-down time length
        System.err.println("SLEEPING FOR " + (HOLDDOWN_TIME / 2) + " ms");
        Thread.sleep(HOLDDOWN_TIME / 2);

        // Node two comes back up at halftime
        event = createNodeUpEvent( 2 );
        System.err.println("SENDING nodeUp EVENT FOR NODE TWO!!");
        engine.correlate( event );

        // Now wait for the hold-down timer to run out on node one
        System.err.println("SLEEPING FOR " + (HOLDDOWN_TIME / 2 + 1000) + " ms");        
        Thread.sleep( HOLDDOWN_TIME / 2 + 1000);

        // Node one comes back up, but it's too late for him
        event = createNodeUpEvent( 1 );
        System.err.println("SENDING nodeUp EVENT FOR NODE ONE!!");
        engine.correlate( event );

        getAnticipator().verifyAnticipated();
    }

    /**
     * Test iface down holddown rules.
     *
     * @throws Exception the exception
     */
    @Test
    public void testIfaceDownHolddownRules() throws Exception {
        testIfaceDownHolddownRules("interfaceDownHolddownRules");
    }

    /**
     * Test iface down holddown rules.
     *
     * @param engineName the engine name
     * @throws InterruptedException the interrupted exception
     */
    private void testIfaceDownHolddownRules(String engineName) throws InterruptedException {

        getAnticipator().reset();

        // In the end, we expect to see a down-past-holddown-time event only for interface 1.1.1.1
        EventBuilder bldr = new EventBuilder( IFACE_DOWN_PAST_HOLDDOWN_UEI, "Drools" );
        bldr.setNodeid( 1 );
        bldr.setInterface(InetAddressUtils.addr("1.1.1.1"));
        bldr.addParam( "holdDownTime", "30" );

        anticipate( bldr.getEvent() );

        DroolsCorrelationEngine engine = findEngineByName(engineName);

        // Interface one goes down at zero seconds
        Event event = createIfEvent(EventConstants.INTERFACE_DOWN_EVENT_UEI, 1, "1.1.1.1");
        System.err.println("SENDING interfaceDown EVENT FOR IFACE ONE!!");
        System.err.println("MAKE IT SO, NUMBER ONE!!!");
        engine.correlate( event );

        // Interface two goes down at substantially zero seconds
        event = createIfEvent(EventConstants.INTERFACE_DOWN_EVENT_UEI, 1, "2.2.2.2");
        System.err.println("SENDING interfaceDown EVENT FOR IFACE TWO!!");
        engine.correlate( event );

        // Wait for half the hold-down time length
        System.err.println("SLEEPING FOR " + (HOLDDOWN_TIME / 2) + " ms");
        Thread.sleep(HOLDDOWN_TIME / 2);

        // Interface two comes back up at halftime
        event = createIfEvent(EventConstants.INTERFACE_UP_EVENT_UEI, 1, "2.2.2.2");
        System.err.println("SENDING interfaceUp EVENT FOR IFACE TWO!!");
        engine.correlate( event );

        // Now wait for the hold-down timer to run out on node one
        System.err.println("SLEEPING FOR " + (HOLDDOWN_TIME / 2 + 1000) + " ms");        
        Thread.sleep( HOLDDOWN_TIME / 2 + 1000);

        // Node one comes back up, but it's too late for him
        event = createIfEvent(EventConstants.INTERFACE_UP_EVENT_UEI, 1, "1.1.1.1");
        System.err.println("SENDING interfaceUp EVENT FOR IFACE ONE!!");
        engine.correlate( event );

        getAnticipator().verifyAnticipated();
    }

    /**
     * Creates the node down event.
     *
     * @param nodeid the Node ID
     * @return the event
     */
    public Event createNodeDownEvent(int nodeid) {
        return createNodeEvent(EventConstants.NODE_DOWN_EVENT_UEI, nodeid);
    }

    /**
     * Creates the node up event.
     *
     * @param nodeid the Node ID
     * @return the event
     */
    public Event createNodeUpEvent(int nodeid) {
        return createNodeEvent(EventConstants.NODE_UP_EVENT_UEI, nodeid);
    }

    /**
     * Creates the node lost service event.
     *
     * @param nodeid the Node ID
     * @param ipAddr the IP address
     * @param svcName the service name
     * @return the event
     */
    public Event createNodeLostServiceEvent(int nodeid, String ipAddr, String svcName) {
        return createSvcEvent("uei.opennms.org/nodes/nodeLostService", nodeid, ipAddr, svcName);
    }

    /**
     * Creates the node regained service event.
     *
     * @param nodeid the Node ID
     * @param ipAddr the IP address
     * @param svcName the service name
     * @return the event
     */
    public Event createNodeRegainedServiceEvent(int nodeid, String ipAddr, String svcName) {
        return createSvcEvent("uei.opennms.org/nodes/nodeRegainedService", nodeid, ipAddr, svcName);
    }

    /**
     * Creates the service event.
     *
     * @param uei the event UEI
     * @param nodeid the Node ID
     * @param ipaddr the IP Address
     * @param svcName the service name
     * @return the event
     */
    private Event createSvcEvent(String uei, int nodeid, String ipaddr, String svcName) {
        return new EventBuilder(uei, "Drools")
        .setNodeid(nodeid)
        .setInterface( addr( ipaddr ) )
        .setService( svcName )
        .getEvent();

    }

    /**
     * Creates the if event.
     *
     * @param uei the event UEI
     * @param nodeid the Node ID
     * @param ipaddr the IP Address
     * @return the event
     */
    private Event createIfEvent(String uei, int nodeid, String ipaddr) {
        return new EventBuilder(uei, "Drools")
        .setNodeid(nodeid)
        .setInterface( addr( ipaddr ) )
        .getEvent();
    }

    /**
     * Creates the one parameter if event.
     *
     * @param uei the event UEI
     * @param nodeid the Node ID
     * @param ipaddr the IP Address
     * @param parmName the parameter name
     * @param parmValue the parameter value
     * @return the event
     */
    private Event createOneParmIfEvent(String uei, int nodeid, String ipaddr, String parmName, String parmValue) {
        return new EventBuilder(uei, "Drools")
        .setNodeid(nodeid)
        .setInterface( addr( ipaddr ) )
        .addParam(parmName, parmValue)
        .getEvent();
    }


    /**
     * Creates the node event.
     *
     * @param uei the event UEI
     * @param nodeid the Node ID
     * @return the event
     */
    private Event createNodeEvent(String uei, int nodeid) {
        return new EventBuilder(uei, "test")
        .setNodeid(nodeid)
        .getEvent();
    }

}
