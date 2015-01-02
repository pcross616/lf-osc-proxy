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

import com.xley.lfosc.midi.MidiCommon;
import com.xley.lfosc.util.LogUtil;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import javax.sound.midi.MidiDevice;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestProxyModeMIDI {


    @Before
    public void setup() {
        LogUtil.setLevel(Level.ALL);
    }


    @Test
    public void testListMidiDevices() throws Exception {
        MidiDevice.Info[] infos = MidiCommon.listDevices(true, true, true);
        assertNotNull(infos);
        assertTrue(infos.length >= 1);
    }

}
