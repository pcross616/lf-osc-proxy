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

package com.xley.lfosc.midi.receiver;

import com.xley.lfosc.impl.ProxyDaemon;
import com.xley.lfosc.midi.IMidiMessageHandler;
import com.xley.lfosc.midi.MidiCommon;
import com.xley.lfosc.midi.MidiProtocol;
import com.xley.lfosc.midi.impl.objects.MidiKey;
import com.xley.lfosc.util.LogUtil;
import org.codehaus.jackson.map.ObjectMapper;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MidiServer implements Runnable {
    private final ProxyDaemon daemon;
    private final String deviceName;
    private final int deviceNameIdx;
    private final IMidiMessageHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object monitor = new Object();
    private Map<String, List<MidiKey>> noteMap = null;

    public MidiServer(ProxyDaemon proxyDaemon, String deviceName, int deviceNameIdx) {
        this(deviceName, deviceNameIdx, proxyDaemon, null);
    }

    public MidiServer(IMidiMessageHandler midiMapperHandler, String deviceName, int deviceNameIdx) {
        this(deviceName, deviceNameIdx, null, midiMapperHandler);
        midiMapperHandler.setMidiServer(this);
    }

    private MidiServer(String deviceName, int deviceNameIdx, ProxyDaemon proxyDaemon, IMidiMessageHandler handler) {
        this.daemon = proxyDaemon;
        this.deviceName = deviceName;
        this.deviceNameIdx = deviceNameIdx;
        this.handler = handler;

        FileInputStream fis = null;
        try {
            File midiMap = new File(MidiProtocol.resources.getString("midi.server.map.json"));
            if (midiMap.exists()) {
                fis = new FileInputStream(midiMap);
                noteMap = objectMapper.readValue(fis, Map.class);
            } else {
                noteMap = new HashMap<>();
            }

        } catch (IOException e) {
            LogUtil.error(getClass(), MidiProtocol.resources.getString("midi.server.error.load.props"), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    LogUtil.trace(getClass(), e);
                }
            }
        }
    }


    public void run() {
        if (deviceName == null && deviceNameIdx < 0) {
            LogUtil.error(getClass(), MidiProtocol.resources.getString("midi.server.error.no_device"));
            shutdown(2);
            return;
        }

        MidiDevice.Info info;
        if (deviceName != null) {
            info = MidiCommon.getMidiDeviceInfo(deviceName, false);
        } else {
            info = MidiCommon.getMidiDeviceInfo(deviceNameIdx);
        }
        if (info == null) {
            if (deviceName != null) {
                LogUtil.error(getClass(), MessageFormat.format(
                        MidiProtocol.resources.getString("midi.server.error.no_device_name"), deviceName));
            } else {
                LogUtil.error(getClass(), MessageFormat.format(
                        MidiProtocol.resources.getString("midi.server.error.no_device_index"), deviceNameIdx));
            }
            //no device found
            shutdown(2);
            return;
        }
        MidiDevice inputDevice = null;
        try {
            inputDevice = MidiSystem.getMidiDevice(info);
            if (!inputDevice.isOpen()) {
                inputDevice.open();
            }
        } catch (MidiUnavailableException e) {
            LogUtil.error(getClass(), e);
        }
        if (inputDevice == null) {
            LogUtil.error(getClass(), MessageFormat.format(
                    MidiProtocol.resources.getString("midi.server.error.unable.retrieve"), info.getName()));
            shutdown(2);
            return;
        }
        MidiReceiver r = new MidiReceiver(handler);

        try {
            Transmitter t = inputDevice.getTransmitter();
            t.setReceiver(r);
        } catch (MidiUnavailableException e) {
            LogUtil.error(getClass(), MidiProtocol.resources.getString("midi.server.error.unable.connect"), e);

            r.close();
            if (inputDevice.getReceivers().size() == 0) {
                inputDevice.close();
            }

            shutdown(2);
            return;
        }


        try {
            synchronized (monitor) {
                while ((daemon != null && !daemon.isShutdown()) || !Thread.currentThread().isInterrupted()) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        if (daemon != null && daemon.isShutdown()) {
                            LogUtil.trace(getClass(), "Caught shutdown signal, isShutdown: " + daemon.isShutdown());
                        }
                        break;
                    }
                }
            }
        } finally {
            r.close();
            if (inputDevice.getReceivers().size() == 0) {
                inputDevice.close();
            }

            FileOutputStream fos = null;
            try {
                File midiMap = new File(MidiProtocol.resources.getString("midi.server.map.json"));
                fos = new FileOutputStream(midiMap);
                objectMapper.defaultPrettyPrintingWriter().writeValue(fos, noteMap);
            } catch (IOException e) {
                LogUtil.error(getClass(), MidiProtocol.resources.getString("midi.server.error.save.props"), e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e1) {
                        LogUtil.trace(getClass(), e1);
                    }
                }
            }
            LogUtil.trace(getClass(), "Received " + r.seCount + " sysex messages with a total of " + r.seByteCount + " bytes");
            LogUtil.trace(getClass(), "Received " + r.smCount + " short messages with a total of " + r.smByteCount + " bytes");
            LogUtil.trace(getClass(), "Received a total of " + (r.smByteCount + r.seByteCount) + " bytes");
        }
    }

    public void addNoteBinding(String operation, int status, int key) {
        getOperatorBinding(operation).add(new MidiKey(key, status));
    }

    public synchronized List<MidiKey> getOperatorBinding(String operation) {
        if (!noteMap.containsKey(operation)) {
            noteMap.put(operation, new ArrayList<MidiKey>());
        }
        return noteMap.get(operation);
    }

    private void shutdown(int errorCode) {
        if (daemon != null) {
            daemon.shutdown(errorCode);
        }
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public void shutdown() {
        shutdown(0);
    }
}
