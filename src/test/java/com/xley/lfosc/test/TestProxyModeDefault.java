/*
 * Copyright (c) 2014. Peter Crossley (xley.com)
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.xley.lfosc.test;

import com.xley.lfosc.OSCProxy;
import com.xley.lfosc.lightfactory.client.LightFactoryClient;
import com.xley.lfosc.test.support.MockLightFactoryServer;
import com.xley.lfosc.test.support.ProxyServerRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;


public class TestProxyModeDefault {
    private Thread server = null;
    private ServerSocket receiver = null;
    private MockLightFactoryServer mockServer = null;
    private Thread mockThread = null;

    @Before
    public void setUp() throws Exception {
        server = new Thread(new ProxyServerRunner(), "TestProxyMode - Default");
        server.start();

        mockServer = new MockLightFactoryServer(3300);
        mockThread = new Thread(mockServer, "MockLightFactoryServer");
        mockThread.start();


        System.out.println("Waiting for servers to start... (1 sec)");
        Thread.sleep(1000);
    }

    @After
    public void tearDown() throws Exception {

        mockThread.interrupt();
        mockServer.shutdown();

        server.interrupt();
        if (receiver != null) {
            receiver.close();
        }

        mockServer = null;
        mockThread = null;
        server = null;
        receiver = null;

        System.gc();
    }

    @Test
    public void testLoopback() throws Exception {
        String data = "osc@" + InetAddress.getLoopbackAddress().getHostAddress() +
                ":3200 /lf/" + InetAddress.getLoopbackAddress().getHostAddress() +
                ":3300/loopback test\n";

        LightFactoryClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3100), data);
        assertEquals("loopback test", mockServer.getLastValue());
    }

/*    @Test
    public void testOSCtoMIDICommand() throws Exception {
        OSCMessage msg = new OSCMessage("/midi/loopMIDI%20Port/1:1:1");
        Object response = OSCClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3200), msg);
        Thread.sleep(3000);
        assertNull(response);
    }*/

    @Test
    public void testAlreadyRunning() throws Exception {
        assertEquals(2, new OSCProxy().execute(new String[]{"-v", "TRACE"}));
    }

    @Test
    public void testHelpOptions() throws Exception {
        assertEquals(0, new OSCProxy().execute(new String[]{"-?", "-v", "TRACE"}));
    }

    @Test
    public void testInvalidMode() throws Exception {
        assertEquals(1, new OSCProxy().execute(new String[]{"-m", "foobar", "-v", "TRACE"}));
    }
}
