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

import com.xley.lfosc.IProtocol;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.impl.BaseProtocol;
import com.xley.lfosc.impl.SimpleProtocolData;
import com.xley.lfosc.midi.impl.LearnCommand;
import com.xley.lfosc.util.LogUtil;

import javax.sound.midi.*;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MidiProtocol extends BaseProtocol implements IProtocol {

    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(MidiProtocol.class.getSimpleName(),
            Locale.getDefault());

    /**
     * The constant midi address pattern.
     */
    protected static final Pattern midiAddress = Pattern.compile("^\\/(\\w*)\\/(\\w*)\\/(\\d:\\d:\\d)($|\\?.*$)",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);

    private static final int MIDI_EVENT_PROTOCOL = 1;
    private static final int MIDI_EVENT_DEVICE = 2;
    private static final int MIDI_EVENT_NOTE = 3;

    private static final String DEFAULT_DEVICE = "default";

    @Override
    public Object process(IProtocolData data) throws ProtocolException {
        String device = (String) data.getTarget();
        String[] note = ((String) data.getOperation()).split(":");
        return _process(Integer.parseInt(note[0]), Integer.parseInt(note[1]), Integer.parseInt(note[2]), device);
    }

    @Override
    public IProtocolData createProtocolData(Object value) {
        if (value instanceof CharSequence) {
            Matcher matches = midiAddress.matcher((CharSequence) value);
            //find the address and verify
            if (matches.find()) {
                LogUtil.debug(getClass(), resources.getString("midi.event.valid"));
                String protocol = matches.group(MIDI_EVENT_PROTOCOL);
                String device = matches.group(MIDI_EVENT_DEVICE);
                String note = matches.group(MIDI_EVENT_NOTE);

                return new SimpleProtocolData(protocol, device, note, null);
            }
        }
        return null;
    }

    @Override
    public IProtocolData configureProtocolData(IProtocolData data) {
        if (data.getOperation() instanceof String) {
            String operation = (String) data.getOperation();
            if (operation.startsWith("/")) {
                ((SimpleProtocolData) data).setOperation(((String) data.getOperation()).substring(1));
            }
        }
        return super.configureProtocolData(data);
    }

    private Object _process(int nKey, int nVelocity, int nDuration, String deviceName) throws ProtocolException {
        int nChannel = 0;

        nKey = Math.min(127, Math.max(0, nKey));
        nVelocity = Math.min(127, Math.max(0, nVelocity));
        nDuration = Math.max(0, nDuration);


        MidiDevice outputDevice = null;
        Receiver receiver = null;
        if (deviceName != null && !DEFAULT_DEVICE.equalsIgnoreCase(deviceName)) {
            MidiDevice.Info info = MidiCommon.getMidiDeviceInfo(deviceName, true);
            if (info == null) {
                throw new ProtocolException(MessageFormat.format(resources.getString("midi.device.not_found"),
                        deviceName));
            }
            try {
                outputDevice = MidiSystem.getMidiDevice(info);
                LogUtil.trace(getClass(), MessageFormat.format(resources.getString("midi.device.output_name"),
                        outputDevice));
                if (!outputDevice.isOpen()) {
                    outputDevice.open();
                }
            } catch (MidiUnavailableException e) {
                LogUtil.error(MidiProtocol.class, e);
            }

            if (outputDevice == null) {
                throw new ProtocolException(MessageFormat.format(resources.getString("midi.device.output_unavailable"),
                        deviceName));
            }
            try {
                receiver = outputDevice.getReceiver();
            } catch (MidiUnavailableException e) {
                LogUtil.error(MidiProtocol.class, e);
            }
        } else {
            try {
                receiver = MidiSystem.getReceiver();
            } catch (MidiUnavailableException e) {
                LogUtil.error(MidiProtocol.class, e);
            }
        }
        if (receiver == null) {
            throw new ProtocolException(MessageFormat.format(resources.getString("midi.device.input_unavailable"),
                    deviceName));
        }

        LogUtil.debug(getClass(), MessageFormat.format(resources.getString("midi.device.input_name"), deviceName));

		/*	Here, we prepare the MIDI messages to send.
            Obviously, one is for turning the key on and
			one for turning it off.
		*/
        ShortMessage onMessage = null;
        ShortMessage offMessage = null;
        try {
            onMessage = new ShortMessage();
            offMessage = new ShortMessage();
            onMessage.setMessage(ShortMessage.NOTE_ON, nChannel, nKey, nVelocity);
            offMessage.setMessage(ShortMessage.NOTE_OFF, nChannel, nKey, 0);

        } catch (InvalidMidiDataException e) {
            LogUtil.error(MidiProtocol.class, e);
        }

		/*
         *	Turn the note on
		 */
        LogUtil.trace(getClass(), MessageFormat.format(resources.getString("midi.note.on"),
                onMessage.getStatus(),
                onMessage.getData1(),
                onMessage.getData2()));

        receiver.send(onMessage, -1);

		/*
         *	Wait for the specified amount of time
		 *	(the duration of the note).
		 */
        try {
            Thread.sleep(nDuration);
        } catch (InterruptedException e) {
            LogUtil.error(MidiProtocol.class, e);
        }

		/*
		 *	Turn the note off.
		 */
        LogUtil.trace(getClass(), MessageFormat.format(resources.getString("midi.note.off"),
                onMessage.getStatus(),
                onMessage.getData1(),
                onMessage.getData2()));

        receiver.send(offMessage, -1);

		/*
		 *	Clean up.
		 */
        receiver.close();
        if (outputDevice != null && outputDevice.getReceivers().size() == 0) {
            outputDevice.close();
        }
        return MessageFormat.format(resources.getString("midi.note.sent"), deviceName, nKey, nVelocity, nDuration);
    }

    @Override
    public int command(String commandName, String[] args) throws ProtocolException {
        switch (commandName) {
            case "list":
                MidiCommon.listDevices(true, true, true);
                return 0;
            case "learn":
                return new LearnCommand().learn(args);
            default:
                return super.command(commandName, args);
        }
    }
}
