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
import com.xley.lfosc.ClientProtocolException;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.http.client.HttpClient;
import com.xley.lfosc.midi.MidiProtocol;
import com.xley.lfosc.osc.client.OSCClient;
import com.xley.lfosc.test.support.MockLightFactoryServer;
import com.xley.lfosc.test.support.MockOSCListener;
import com.xley.lfosc.test.support.ProxyServerRunner;
import com.xley.lfosc.util.LogUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.text.MessageFormat;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestProxyModeHttp {
    private Thread server = null;
    private ServerSocket receiver = null;
    private MockLightFactoryServer mockServer = null;
    private Thread mockThread = null;
    private OSCPortIn oscReceiver = null;
    private MockOSCListener listener = null;

    @Before
    public void setUp() throws Exception {
        server = new Thread(new ProxyServerRunner("http"), "TestProxyMode - HTTP");
        server.start();

        mockServer = new MockLightFactoryServer(3300);
        mockThread = new Thread(mockServer, "MockLightFactoryServer");
        mockThread.start();

        oscReceiver = new OSCPortIn(new DatagramSocket(
                new InetSocketAddress(InetAddress.getLoopbackAddress(),
                        OSCPort.defaultSCOSCPort())));

        listener = new MockOSCListener();
        oscReceiver.addListener("/message/receiving", listener);
        oscReceiver.startListening();


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

        if (oscReceiver != null) {
            oscReceiver.stopListening();
            oscReceiver.close();
        }


        mockServer = null;
        mockThread = null;
        server = null;
        receiver = null;
        oscReceiver = null;
        listener = null;

        System.gc();
    }

    @Test
    public void testHTTPtoLFCommand() throws Exception {
        String url = "http://localhost:8080/lf/localhost:3300/foo%20bar%201234";
        Object response = HttpClient.send(url);
        assertNotNull(response);
        assertEquals("foo bar 1234", mockServer.getLastValue());
    }

    @Test
    public void testHTTPtoOSC() throws Exception {
        String url = "http://localhost:8080/osc/localhost:57110/message/receiving%20testoscproxy";
        Object response = HttpClient.send(url);
        assertEquals("", response);
        assertEquals(1, listener.getMessages().size());
        assertEquals("testoscproxy", ((OSCMessage) listener.getMessages().toArray()[0])
                .getArguments()
                .get(0));

    }

    @Test
    public void testHTTPtoMIDI() throws Exception {
        String url = "http://localhost:8080/midi/loopMIDI%20Port/1:1:1";
        Object response = HttpClient.send(url);
        assertEquals(MessageFormat.format(MidiProtocol.resources.getString("midi.note.sent"), "loopMIDI Port", 1,1,1), response);
    }

    @Test
    public void testHTTPInvalid() throws Exception {
        String url = "http://localhost:8080/foo/default/bar";
        try {
            Object response = HttpClient.send(url);
            assert true; //should never hit this
        } catch (ClientProtocolException e) {
            LogUtil.error(e);
            assertEquals(HttpResponseStatus.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void testHTTPBadProtocol() throws Exception {
        String url = "ftp://localhost:8080/foo/default/bar";
        try {
            Object response = HttpClient.send(url);
            assert true; //should never hit this
        } catch (ClientProtocolException e) {
            LogUtil.error(e);
            assertNull(e.getStatus());
        }
    }
}
