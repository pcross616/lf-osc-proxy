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


import com.xley.lfosc.OSCProxy;

import java.util.ArrayList;
import java.util.List;

public class ProxyServerRunner implements Runnable {
    private final String[] args;

    public ProxyServerRunner(String[] modes) {
        List<String> argList = new ArrayList<String>();
        argList.add("-v");
        argList.add("ALL");
        for (String mode : modes) {
            argList.add("-m");
            argList.add(mode);
        }
        this.args = argList.toArray(new String[argList.size()]);
    }

    public ProxyServerRunner(String mode) {
        this(new String[]{mode});
    }

    public ProxyServerRunner() {
        this(new String[]{});
    }

    @Override
    public void run() {
        new OSCProxy().execute(args);
    }
}