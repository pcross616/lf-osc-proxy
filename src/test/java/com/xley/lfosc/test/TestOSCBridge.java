/*
 * Copyright (c) 2014. Peter Crossley
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.xley.lfosc.test;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import junit.framework.TestCase;

import java.net.*;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: crossleyp
 * Date: 4/24/14
 * Time: 11:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestOSCBridge extends TestCase {
    private Thread server = null;
    private ServerSocket receiver = null;
    private MockLightFactoryServer mockServer = null;
    private Thread mockThread = null;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new Thread(new TestOSCProxyServer("osc"), "TestOSCProxyServer");
        server.start();

        mockServer = new MockLightFactoryServer();
        mockThread = new Thread(mockServer, "MockLightFactoryServer");
        mockThread.start();


        System.out.println("Waiting for servers to start... (5 sec)");
        Thread.sleep(5000);
    }

    @Override
    protected void tearDown() throws Exception {

        mockThread.interrupt();
        server.interrupt();
        if (receiver != null) {
            receiver.close();
        }
        Thread.sleep(3000);
        super.tearDown();
    }

    public void testBridge() throws Exception {
        OSCPortOut oscPortOut = new OSCPortOut(InetAddress.getLoopbackAddress(), 3200);
        String[] args = {"bar","1234"};
        OSCMessage msg = new OSCMessage("/lf/localhost:3300/foo", Arrays.asList((Object[])args));
        oscPortOut.send(msg);
        oscPortOut.close();
        Thread.sleep(5000);
        assertEquals("foo bar 1234", mockServer.getLastValue());
    }
}
