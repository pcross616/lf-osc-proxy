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

package com.xley.lfosc;

import com.illposed.osc.OSCMessage;

import java.util.Date;

public class OSCBridgeListener implements com.illposed.osc.OSCListener {

    private boolean messageReceived = false;
    private Date receivedTimestamp = null;

    public Date lastReceivedTimestamp() {
        return receivedTimestamp;
    }

    public boolean isMessageReceived() {
        return messageReceived;
    }

    @Override
    public void acceptMessage(Date time, OSCMessage message) {
        messageReceived = true;
        receivedTimestamp = time;
        OSCEventProtocol eventProtocol = new OSCEventProtocol();
        eventProtocol.processOSCEvent(message);
    }
}
