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

import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortIn;
import junit.framework.TestCase;

import java.io.DataOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: crossleyp
 * Date: 4/24/14
 * Time: 11:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestOSCProcessing extends TestCase {
    private Thread server = null;
    private OSCPortIn receiver = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new Thread(new TestOSCProxyServer());
        server.start();
        receiver = new OSCPortIn(new DatagramSocket(
                                 new InetSocketAddress(InetAddress.getLoopbackAddress(),OSCPort.defaultSCOSCPort())));
    }

    @Override
    protected void tearDown() throws Exception {

        server.interrupt();
        receiver.close();
        super.tearDown();
    }

    public void testProxy() throws Exception {
        TestOSCListener listener = new TestOSCListener();
        receiver.addListener("/message/receiving", listener);
        receiver.startListening();

        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        try {
            clientSocket = new Socket(InetAddress.getLoopbackAddress(), 3100);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            String data = "osc@"+InetAddress.getLoopbackAddress().getHostAddress()+":" + OSCPort.defaultSCOSCPort() + " /message/receiving\n";
            outToServer.writeBytes(data);
            Thread.sleep(100); // wait a bit
        } finally {
            if (outToServer != null) {
                outToServer.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }

        receiver.stopListening();
        if (!listener.isMessageReceived()) {
            fail("Message was not received");
        }
    }
}
