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
import com.xley.lfosc.test.support.MockLightFactoryServer;
import com.xley.lfosc.test.support.ProxyServerRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertEquals;


public class TestProxyModeBoth {
    private Thread server = null;
    private ServerSocket receiver = null;
    private MockLightFactoryServer mockServer = null;
    private Thread mockThread = null;

    @Before
    public void setUp() throws Exception {
        server = new Thread(new ProxyServerRunner("both"), "TestProxyMode - Both");
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
    public void testLoopback() throws Exception {
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        try {
            clientSocket = new Socket(InetAddress.getLoopbackAddress(), 3100);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            String data = "osc@" + InetAddress.getLoopbackAddress().getHostAddress() + ":3200 /lf/" + InetAddress.getLoopbackAddress().getHostAddress() + ":3300/loopback test\n";
            outToServer.writeBytes(data);
            Thread.sleep(2000); // wait a bit
        } finally {
            if (outToServer != null) {
                outToServer.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
        assertEquals("loopback test", mockServer.getLastValue());
    }

    @Test
    public void testAlreadyRunning() throws Exception {
        assertEquals(new OSCProxy().execute(new String[]{"-d", "TRACE"}), 2);
    }

    @Test
    public void testHelpOptions() throws Exception {
        assertEquals(new OSCProxy().execute(new String[]{"-?", "-d", "TRACE"}), 0);
    }

    @Test
    public void testInvalidMode() throws Exception {
        assertEquals(new OSCProxy().execute(new String[]{"-m", "foobar", "-d", "TRACE"}), 1);
    }
}
