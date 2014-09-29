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
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class ProxyServerRunner implements Runnable {
    static {
        OSCProxy.logger.removeAllAppenders();
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("[%p] (%t) %c[%M] - %m%n");
        OSCProxy.logger.addAppender(new ConsoleAppender(layout));
    }

    private String mode = "both";

    public ProxyServerRunner(String mode) {
        this.mode = mode;
    }

    @Override
    public void run() {
        new OSCProxy().execute(new String[]{"-d", "TRACE", "-m", mode});
    }
}
