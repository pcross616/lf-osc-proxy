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

package com.xley.lfosc.impl;

import com.illposed.osc.OSCMessage;
import com.xley.lfosc.util.LogUtil;
import sun.rmi.runtime.Log;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The type OSC bridge listener.
 */
public class OSCProxyListener implements com.illposed.osc.OSCListener {

    /**
     * Executor for LightFactory outbound connections.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);


    @Override
    public final void acceptMessage(final Date time, final OSCMessage message) {
        executorService.submit(new OSCProtocol(message));
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LogUtil.trace(getClass(), e);
        }
    }

}
