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

package com.xley.lfosc.test.support;

import java.io.IOException;
import java.net.ServerSocket;

public class MockLightFactoryServer implements Runnable {
    protected volatile String lastValue;
    private ServerSocket serverSocket;
    private int port;

    public MockLightFactoryServer(int port) {
        this.port = port;
    }


    public String getLastValue() {
        return lastValue;
    }

    public void shutdown() {
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (IOException e) {
            //do nothing
        }
    }

    @Override
    public void run() {

        try {
            serverSocket = new ServerSocket(port);
            while (!Thread.interrupted()) {
                new MockLightFactoryThread(serverSocket.accept(), this).start();
            }

        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                e.printStackTrace();
            }
        }
    }
}
