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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MockLightFactoryThread extends Thread {
    protected volatile String lastValue;
    private Socket socket;
    public MockLightFactoryThread(Socket socket) {
        super();
        this.socket=socket;
    }

    @Override
    public void run() {


        try (
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));
        ) {
            String inputLine, outputLine;
            outToClient.writeBytes("LF Mock Server: CONNECT\n");
            while (socket.isConnected() && (inputLine = in.readLine()) != null) {
                lastValue = inputLine.trim();
                outToClient.writeBytes("OK I got - " + lastValue + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                //
            }
        }
    }
}
