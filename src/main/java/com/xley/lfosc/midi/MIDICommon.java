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

package com.xley.lfosc.midi;

import com.xley.lfosc.util.LogUtil;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

public class MidiCommon {

    public static MidiDevice.Info[] listDevices(boolean bForInput,
                                                boolean bForOutput,
                                                boolean bVerbose) {
        if (bForInput && !bForOutput) {
            LogUtil.info(MidiProtocol.resources.getString("midi.common.available.in.devices"));
        } else if (!bForInput && bForOutput) {
            LogUtil.info(MidiProtocol.resources.getString("midi.common.available.out.devices"));
        } else {
            LogUtil.info(MidiProtocol.resources.getString("midi.common.available.devices"));
        }

        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                if ((bAllowsInput && bForInput) ||
                        (bAllowsOutput && bForOutput)) {
                    if (bVerbose) {
                        LogUtil.trace("" + i + "  "
                                + (bAllowsInput ? MidiProtocol.resources.getString("midi.common.in") + " " : "   ")
                                + (bAllowsOutput ? MidiProtocol.resources.getString("midi.common.out") + " " : "    ")
                                + aInfos[i].getName() + ", "
                                + aInfos[i].getVendor() + ", "
                                + aInfos[i].getVersion() + ", "
                                + aInfos[i].getDescription());
                    } else {
                        LogUtil.info("" + i + "  " + aInfos[i].getName());
                    }
                }
            } catch (MidiUnavailableException e) {
                LogUtil.trace(MidiCommon.class, e);
            }
        }
        if (aInfos.length == 0) {
            LogUtil.info(MidiProtocol.resources.getString("midi.common.no.devices"));
        }
        return aInfos;
    }


    /**
     * Retrieve a MidiDevice.Info for a given name.
     * <p/>
     * This method tries to return a MidiDevice.Info whose name
     * matches the passed name. If no matching MidiDevice.Info is
     * found, null is returned.  If bForOutput is true, then only
     * output devices are searched, otherwise only input devices.
     *
     * @param strDeviceName the name of the device for which an info
     *                      object should be retrieved.
     * @param bForOutput    If true, only output devices are
     *                      considered. If false, only input devices are considered.
     * @return A MidiDevice.Info object matching the passed device
     * name or null if none could be found.
     */
    public static MidiDevice.Info getMidiDeviceInfo(String strDeviceName, boolean bForOutput) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info aInfo : aInfos) {
            if (aInfo.getName().equals(strDeviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfo);
                    boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                    boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                    if ((bAllowsOutput && bForOutput) || (bAllowsInput && !bForOutput)) {
                        return aInfo;
                    }
                } catch (MidiUnavailableException e) {
                    LogUtil.trace(MidiCommon.class, e);
                }
            }
        }
        return null;
    }


    /**
     * Retrieve a MidiDevice.Info by index number.
     * This method returns a MidiDevice.Info whose index
     * is specified as parameter. This index matches the
     * number printed in the listDevices method.
     * If index is too small or too big, null is returned.
     *
     * @param index the index of the device to be retrieved
     * @return A MidiDevice.Info object of the specified index
     * or null if none could be found.
     */
    public static MidiDevice.Info getMidiDeviceInfo(int index) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        if ((index < 0) || (index >= aInfos.length)) {
            return null;
        }
        return aInfos[index];
    }
}
