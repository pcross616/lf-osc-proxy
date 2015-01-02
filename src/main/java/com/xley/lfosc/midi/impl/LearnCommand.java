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

package com.xley.lfosc.midi.impl;

import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolManager;
import com.xley.lfosc.midi.IMidiMessageHandler;
import com.xley.lfosc.midi.receiver.MidiServer;
import com.xley.lfosc.util.LogUtil;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;


public class LearnCommand {

    /**
     * The constant resources.
     */
    private static final ResourceBundle resources = ResourceBundle.getBundle(LearnCommand.class.getSimpleName(),
            Locale.getDefault());

    /**
     * Learn Midi Notes
     *
     * @param args the command args
     * @return
     */
    public int learn(String[] args) {
        if (args.length == 0) {
            LogUtil.console(resources.getString("learn.help"));
            return -1;
        }
        String device = args[0];
        LogUtil.debug("Using midi device or index: " + device);
        int deviceIdx = -1;
        try {
            deviceIdx = Integer.parseInt(device);
        } catch (NumberFormatException nfe) {
            //do nothing
        }

        String operation = args.length == 2 ? args[1] : null;
        while (true) {
            if (operation == null || operation.isEmpty()) {
                try {
                    //  open up standard input
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
                    System.out.print(resources.getString("learn.prompt"));
                    operation = br.readLine();
                    if (operation == null || operation.isEmpty()) {
                        return 0;
                    }

                } catch (IOException ioe) {
                    LogUtil.error(resources.getString("learn.error.io"), ioe);
                    return -1;
                }
            }
            IProtocolData data = ProtocolManager.resolve(operation);
            if (data == null) {
                LogUtil.console(MessageFormat.format(resources.getString("learn.operation.invalid"), operation));
                operation = null;
                continue;
            }

            LogUtil.console(MessageFormat.format(resources.getString("learn.operation.listening"), operation));
            MidiServer server = new MidiServer(new MidiMapperHandler(operation), device, deviceIdx);
            //clear the existing binding
            server.getOperatorBinding(operation).clear();

            Thread midiThread = new Thread(server);
            midiThread.start();
            try {
                //  open up standard input
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                LogUtil.console(resources.getString("learn.operation.listening.stop"));
                br.readLine();

                server.shutdown();
                midiThread.interrupt();

                Thread.sleep(1000);
                //reset for new operation
                operation = null;

            } catch (Exception ioe) {
                LogUtil.error(getClass(), ioe);
                return -1;
            }
        }
    }

    private static class MidiMapperHandler extends DefaultMidiMessageHandler implements IMidiMessageHandler {
        private final String operation;

        public MidiMapperHandler(String operation) {
            this.operation = operation;
        }

        @Override
        public void note(MidiMessage message, long timeStamp, String note) {
            // do any default note handling
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                if (shortMessage.getCommand() == 0x80 || shortMessage.getCommand() == 0x90) {
                    midiServer.addNoteBinding(this.operation, ((ShortMessage) message).getData1(), ((ShortMessage) message).getCommand());
                }
            }
            super.note(message, timeStamp, note);
        }
    }
}
