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
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortIn;
import com.xley.lfosc.lightfactory.LightFactoryProtocol;
import com.xley.lfosc.lightfactory.client.LightFactoryClient;
import com.xley.lfosc.test.support.MockOSCListener;
import com.xley.lfosc.test.support.ProxyServerRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.MessageFormat;

import static org.junit.Assert.assertEquals;

public class TestProxyModeLightFactory {
    private Thread server = null;
    private OSCPortIn receiver = null;
    private MockOSCListener listener = null;

    @Before
    public void setUp() throws Exception {
        server = new Thread(new ProxyServerRunner("lf"),
                "TestProxyMode - LightFactory");
        server.start();
        receiver = new OSCPortIn(new DatagramSocket(
                new InetSocketAddress(InetAddress.getLoopbackAddress(),
                        OSCPort.defaultSCOSCPort())));

        listener = new MockOSCListener();
        receiver.addListener("/message/receiving", listener);
        receiver.startListening();

        System.out.println("Waiting for servers to start... (5 sec)");
        Thread.sleep(5000);
    }

    @After
    public void tearDown() throws Exception {

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
    }


    @Test
    public void testLFtoOSC() throws Exception {

        // *** TODO: Once https://github.com/hoijui/JavaOSC/pull/14 is resolved this above test OSC message will work
        //String data = "osc@"+InetAddress.getLoopbackAddress().getHostAddress()+":" + OSCPort.defaultSCOSCPort() + " /message/receiving testoscproxy 123 0.222\n";
        String data = "osc@" + InetAddress.getLoopbackAddress().getHostAddress()
                + ":" + OSCPort.defaultSCOSCPort()
                + " /message/receiving testoscproxy 123 0.222 bar\n";
        Object response = LightFactoryClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3100), data.toUpperCase());

        //Thread.sleep(3000); // wait a bit

        assertEquals(">", response);
        assertEquals(1, listener.getMessages().size());
        assertEquals("testoscproxy", ((OSCMessage) listener.getMessages().toArray()[0])
                .getArguments()
                .get(0));
    }

    @Test
    public void testInvalidFormatLFtoOSC() throws Exception {

        String data = "osc@" + InetAddress.getLoopbackAddress().getHostAddress()
                + ":" + OSCPort.defaultSCOSCPort();
        Object response = LightFactoryClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3100), data);

        //wait
        //Thread.sleep(1000);

        //did we get the error?
        assertEquals(">" + MessageFormat.format(LightFactoryProtocol.resources.getString("lf.command.failed"),
                LightFactoryProtocol.resources.getString("lf.error.invalid")), response + "\r\n>");
        //Thread.sleep(2000); // wait a bit
        assertEquals(listener.getMessages().size(), 0);
    }

    @Test
    public void testLFtoOSCBadEndpoint() throws Exception {
        String data = "osc@" + InetAddress.getLoopbackAddress().getHostAddress()
                + ":9999" + " /message/receiving abc 123 0.222\n";
        Object response = LightFactoryClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3100), data);

        //Thread.sleep(2000); // wait a bit
        assertEquals(">", response);
        assertEquals(listener.getMessages().size(), 0);
    }

    @Test
    public void testLFtoMIDI() throws Exception {
        String data = "midi@default /1:1:1\n";
        Object response = LightFactoryClient.send(new InetSocketAddress(InetAddress.getLoopbackAddress(), 3100), data);
    }
}
