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

import com.illposed.osc.OSCMessage;
import com.xley.lfosc.osc.client.OSCClient;
import com.xley.lfosc.test.support.MockLightFactoryServer;
import com.xley.lfosc.test.support.ProxyServerRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestProxyModeOSC {
    private Thread server = null;
    private ServerSocket receiver = null;
    private MockLightFactoryServer mockServer = null;
    private Thread mockThread = null;

    @Before
    public void setUp() throws Exception {
        server = new Thread(new ProxyServerRunner("osc"), "TestProxyMode - OSC");
        server.start();

        mockServer = new MockLightFactoryServer(3300);
        mockThread = new Thread(mockServer, "MockLightFactoryServer");
        mockThread.start();


        System.out.println("Waiting for servers to start... (5 sec)");
        Thread.sleep(5000);
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
    public void testOSCtoLFCommand() throws Exception {
        String[] args = {"bar", "1234"};
        OSCMessage msg = new OSCMessage("/lf/localhost:3300/foo", Arrays.asList((Object[]) args));
        Object response = OSCClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3200), msg);
        assertNull(response);
        Thread.sleep(5000);
        assertEquals("foo bar 1234", mockServer.getLastValue());
    }
}
