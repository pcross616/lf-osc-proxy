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
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortIn;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
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
public class TestOSCEvents extends TestCase {
    private Thread server = null;
    private OSCPortIn receiver = null;
    private TestOSCListener listener = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new Thread(new TestOSCProxyServer("bridge"), "Test Process - Server Thread");
        server.start();
        receiver = new OSCPortIn(new DatagramSocket(
                                 new InetSocketAddress(InetAddress.getLoopbackAddress(),OSCPort.defaultSCOSCPort())));

        listener = new TestOSCListener();
        receiver.addListener("/message/receiving", listener);
        receiver.startListening();

        System.out.println("Waiting for servers to start... (5 sec)");
        Thread.sleep(5000);
    }

    @Override
    protected void tearDown() throws Exception {

        server.interrupt();
        if (receiver != null) {
            receiver.stopListening();
            receiver.close();
        }

        //clean up for gc
        server = null;
        receiver = null;
        listener = null;
        System.gc();

        Thread.sleep(3000);
        super.tearDown();
    }

    public void testOSCProxy() throws Exception {
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        try {
            clientSocket = new Socket(InetAddress.getLoopbackAddress(), 3100);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            // *** TODO: Once https://github.com/hoijui/JavaOSC/pull/14 is resolved this above test OSC message will work
            //String data = "osc@"+InetAddress.getLoopbackAddress().getHostAddress()+":" + OSCPort.defaultSCOSCPort() + " /message/receiving testoscproxy 123 0.222\n";
            String data = "osc@"+InetAddress.getLoopbackAddress().getHostAddress()+":" + OSCPort.defaultSCOSCPort() + " /message/receiving testoscproxy 123 0.222 bar\n";
            outToServer.writeBytes(data);
        } finally {
            if (outToServer != null) {
                outToServer.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
        Thread.sleep(2000); // wait a bit
        assertEquals(listener.getMessages().size(), 1);
        assertEquals(((OSCMessage)listener.getMessages().toArray()[0]).getArguments().get(0), "testoscproxy");
    }

    public void testInvalidOSCFormat() throws Exception {
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        BufferedReader inFromServer = null;
        try {
            clientSocket = new Socket(InetAddress.getLoopbackAddress(), 3100);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String data = "osc@"+InetAddress.getLoopbackAddress().getHostAddress()+":" + OSCPort.defaultSCOSCPort();
            outToServer.writeBytes(data);
            outToServer.flush();
            clientSocket.shutdownOutput();

            //wait
            Thread.sleep(1000);

            //did we get the error?
            assertEquals(inFromServer.readLine(), "OSC Event Invalid! Syntax 'osc@address:port /container data'");

        } finally {
            if (inFromServer != null) {
                inFromServer.close();
            }
            if (outToServer != null) {
                outToServer.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }

        Thread.sleep(2000); // wait a bit
        assertEquals(listener.getMessages().size(), 0);
    }

    public void testOSCBadEndpoint() throws Exception {
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        try {
            clientSocket = new Socket(InetAddress.getLoopbackAddress(), 3100);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            String data = "osc@"+InetAddress.getLoopbackAddress().getHostAddress()+":9999" + " /message/receiving abc 123 0.222\n";
            outToServer.writeBytes(data);
        } finally {
            if (outToServer != null) {
                outToServer.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }

        Thread.sleep(2000); // wait a bit
        assertEquals(listener.getMessages().size(), 0);
    }
}
