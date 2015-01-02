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

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * Copyright (c) 2003 by Florian Bomers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.xley.lfosc.midi.receiver;

import com.xley.lfosc.midi.IMidiMessageHandler;
import com.xley.lfosc.util.LogUtil;

import javax.sound.midi.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import static com.xley.lfosc.midi.MidiProtocol.resources;


/**
 * Displays the file format information of a MIDI file.
 */
public class MidiReceiver implements Receiver {

    private static final String[] sm_astrKeyNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] sm_astrKeySignatures = {"Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#"};
    private static final String[] SYSTEM_MESSAGE_TEXT =
            {
                    resources.getString("midi.recv.sys.0"),
                    resources.getString("midi.recv.sys.1"),
                    resources.getString("midi.recv.sys.2"),
                    resources.getString("midi.recv.sys.3"),
                    resources.getString("midi.recv.sys.4"),
                    resources.getString("midi.recv.sys.5"),
                    resources.getString("midi.recv.sys.6"),
                    resources.getString("midi.recv.sys.7"),
                    resources.getString("midi.recv.sys.8"),
                    resources.getString("midi.recv.sys.9"),
                    resources.getString("midi.recv.sys.10"),
                    resources.getString("midi.recv.sys.11"),
                    resources.getString("midi.recv.sys.12"),
                    resources.getString("midi.recv.sys.13"),
                    resources.getString("midi.recv.sys.14"),
                    resources.getString("midi.recv.sys.15")
            };
    private static final String[] QUARTER_FRAME_MESSAGE_TEXT =
            {
                    resources.getString("midi.recv.qf.0"),
                    resources.getString("midi.recv.qf.1"),
                    resources.getString("midi.recv.qf.2"),
                    resources.getString("midi.recv.qf.3"),
                    resources.getString("midi.recv.qf.4"),
                    resources.getString("midi.recv.qf.5"),
                    resources.getString("midi.recv.qf.6"),
                    resources.getString("midi.recv.qf.7"),
            };
    private static final String[] FRAME_TYPE_TEXT =
            {
                    resources.getString("midi.recv.ft.0"),
                    resources.getString("midi.recv.ft.1"),
                    resources.getString("midi.recv.ft.2"),
                    resources.getString("midi.recv.ft.3"),
            };
    protected static long seByteCount = 0;
    protected static long smByteCount = 0;
    protected static long seCount = 0;
    protected static long smCount = 0;
    private static char hexDigits[] =
            {'0', '1', '2', '3',
                    '4', '5', '6', '7',
                    '8', '9', 'A', 'B',
                    'C', 'D', 'E', 'F'};


    private IMidiMessageHandler noteHandler;


    public MidiReceiver(IMidiMessageHandler noteHandler) {
        this.noteHandler = noteHandler;
    }

    public static String getKeyName(int nKeyNumber) {
        if (nKeyNumber > 127) {
            return resources.getString("midi.recv.key.illegal_value");
        } else {
            int nNote = nKeyNumber % 12;
            int nOctave = nKeyNumber / 12;
            return sm_astrKeyNames[nNote] + (nOctave - 1);
        }
    }

    public static int get14bitValue(int nLowerPart, int nHigherPart) {
        return (nLowerPart & 0x7F) | ((nHigherPart & 0x7F) << 7);
    }

    private static int signedByteToUnsigned(byte b) {
        return b & 0xFF;
    }

    // convert from microseconds per quarter note to beats per minute and vice versa
    private static float convertTempo(float value) {
        if (value <= 0) {
            value = 0.1f;
        }
        return 60000000.0f / value;
    }

    public static String getHexString(byte[] aByte) {
        StringBuffer sbuf = new StringBuffer(aByte.length * 3 + 2);
        for (byte anAByte : aByte) {
            sbuf.append(' ');
            sbuf.append(hexDigits[(anAByte & 0xF0) >> 4]);
            sbuf.append(hexDigits[anAByte & 0x0F]);
            /*byte	bhigh = (byte) ((aByte[i] &  0xf0) >> 4);
            sbuf.append((char) (bhigh > 9 ? bhigh + 'A' - 10: bhigh + '0'));
			byte	blow = (byte) (aByte[i] & 0x0f);
			sbuf.append((char) (blow > 9 ? blow + 'A' - 10: blow + '0'));*/
        }
        return new String(sbuf);
    }

    private static String intToHex(int i) {
        return "" + hexDigits[(i & 0xF0) >> 4]
                + hexDigits[i & 0x0F];
    }

    public static String getHexString(ShortMessage sm) {
        // bug in J2SDK 1.4.1
        // return getHexString(sm.getMessage());
        int status = sm.getStatus();
        String res = intToHex(sm.getStatus());
        // if one-byte message, return
        switch (status) {
            case 0xF6:            // Tune Request
            case 0xF7:            // EOX
                // System real-time messages
            case 0xF8:            // Timing Clock
            case 0xF9:            // Undefined
            case 0xFA:            // Start
            case 0xFB:            // Continue
            case 0xFC:            // Stop
            case 0xFD:            // Undefined
            case 0xFE:            // Active Sensing
            case 0xFF:
                return res;
        }
        res += ' ' + intToHex(sm.getData1());
        // if 2-byte message, return
        switch (status) {
            case 0xF1:            // MTC Quarter Frame
            case 0xF3:            // Song Select
                return res;
        }
        switch (sm.getCommand()) {
            case 0xC0:
            case 0xD0:
                return res;
        }
        // 3-byte messages left
        res += ' ' + intToHex(sm.getData2());
        return res;
    }

    public static String decodeMessage(ShortMessage message) {
        String strMessage;
        switch (message.getCommand()) {
            case 0x80:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.note_off"),
                        getKeyName(message.getData1()), message.getData2());
                break;

            case 0x90:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.note_on"),
                        getKeyName(message.getData1()), message.getData2());
                break;

            case 0xa0:

                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.poly_key_press"),
                        getKeyName(message.getData1()), message.getData2());

                break;

            case 0xb0:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.ctrl_chg"),
                        message.getData1(), message.getData2());
                break;

            case 0xc0:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.pgrm_chg"),
                        message.getData1());

                break;

            case 0xd0:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.key_press"),
                        getKeyName(message.getData1()), message.getData2());

                break;

            case 0xe0:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.pitch_wheel_chg"),
                        get14bitValue(message.getData1(), message.getData2()));

                break;

            case 0xF0:
                strMessage = resources.getString(SYSTEM_MESSAGE_TEXT[message.getChannel()]);
                switch (message.getChannel()) {
                    case 0x1:
                        int nQType = (message.getData1() & 0x70) >> 4;
                        int nQData = message.getData1() & 0x0F;
                        if (nQType == 7) {
                            nQData = nQData & 0x1;
                        }
                        strMessage += resources.getString(QUARTER_FRAME_MESSAGE_TEXT[nQType]) + nQData;
                        if (nQType == 7) {
                            int nFrameType = (message.getData1() & 0x06) >> 1;
                            strMessage += ", " + MessageFormat.format(resources.getString("midi.recv.msg.frame_type"),
                                    resources.getString(FRAME_TYPE_TEXT[nFrameType]));
                        }
                        break;

                    case 0x2:
                        strMessage += get14bitValue(message.getData1(), message.getData2());
                        break;

                    case 0x3:
                        strMessage += message.getData1();
                        break;
                }
                break;

            default:
                strMessage = MessageFormat.format(resources.getString("midi.recv.msg.unknown"),
                        message.getStatus(), message.getData1(), message.getData2());
                break;
        }
        if (message.getCommand() != 0xF0) {
            int nChannel = message.getChannel() + 1;
            strMessage = MessageFormat.format(resources.getString("midi.recv.msg.channel"), nChannel, strMessage);
        }
        smCount++;
        smByteCount += message.getLength();
        return "[" + getHexString(message) + "] " + strMessage;
    }

    public static String decodeMessage(SysexMessage message) {
        byte[] abData = message.getData();
        String strMessage = null;
        // System.out.println("sysex status: " + message.getStatus());
        if (message.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            strMessage = MessageFormat.format(resources.getString("midi.recv.msg.sysex_msg"), getHexString(abData));
        } else if (message.getStatus() == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
            strMessage = MessageFormat.format(
                    resources.getString("midi.recv.msg.cont_sysex_msg"), getHexString(abData));
            seByteCount--; // do not count the F7
        }
        seByteCount += abData.length + 1;
        seCount++; // for the status byte
        return strMessage;
    }

    public static String decodeMessage(MetaMessage message) {
        byte[] abData = message.getData();
        String strMessage;
        LogUtil.trace(MidiReceiver.class, "data array length: " + abData.length);
        switch (message.getType()) {
            case 0:
                int nSequenceNumber = ((abData[0] & 0xFF) << 8) | (abData[1] & 0xFF);
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.seq_num"), nSequenceNumber);
                break;

            case 1:
                String strText = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.txt_event"), strText);
                break;

            case 2:
                String strCopyrightText = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.copyright"), strCopyrightText);
                break;

            case 3:
                String strTrackName = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.seq_name"), strTrackName);
                break;

            case 4:
                String strInstrumentName = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.inst_name"), strInstrumentName);
                break;

            case 5:
                String strLyrics = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.lyric"), strLyrics);
                break;

            case 6:
                String strMarkerText = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.marker"), strMarkerText);
                break;

            case 7:
                String strCuePointText = new String(abData, Charset.defaultCharset());
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.cue_point"), strCuePointText);
                break;

            case 0x20:
                int nChannelPrefix = abData[0] & 0xFF;
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.midi_channel_prefix"), nChannelPrefix);
                break;

            case 0x2F:
                strMessage = resources.getString("midi.recv.msg.eot");
                break;

            case 0x51:
                int nTempo = ((abData[0] & 0xFF) << 16)
                        | ((abData[1] & 0xFF) << 8)
                        | (abData[2] & 0xFF);           // tempo in microseconds per beat
                float bpm = convertTempo(nTempo);
                // truncate it to 2 digits after dot
                bpm = (Math.round(bpm * 100.0f) / 100.0f);
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.set_tempo"), bpm);
                break;

            case 0x54:
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.smtpe_off"),
                        +(abData[0] & 0xFF) + ":"
                                + (abData[1] & 0xFF) + ":"
                                + (abData[2] & 0xFF) + "."
                                + (abData[3] & 0xFF) + "."
                                + (abData[4] & 0xFF));
                break;

            case 0x58:
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.time_sig"),
                        (abData[0] & 0xFF),
                        (1 << (abData[1] & 0xFF)),
                        (abData[2] & 0xFF),
                        (abData[3] & 0xFF));
                break;

            case 0x59:
                String strGender = (abData[1] == 1) ?
                        resources.getString("midi.recv.msg.key_sig.minor") :
                        resources.getString("midi.recv.msg.key_sig.major");

                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.key_sig"), sm_astrKeySignatures[abData[0] + 7], strGender);
                break;

            case 0x7F:
                // TODO: decode vendor code, dump data in rows
                String strDataDump = getHexString(abData);
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.seq_spec_meta"), strDataDump);
                break;

            default:
                String strUnknownDump = getHexString(abData);
                strMessage = MessageFormat.format(
                        resources.getString("midi.recv.msg.unknown_meta"), strUnknownDump);
                break;

        }
        return strMessage;
    }

    public void close() {
    }

    public void send(MidiMessage message, long lTimeStamp) {
        String strMessage;
        if (message instanceof ShortMessage) {
            strMessage = decodeMessage((ShortMessage) message);
        } else if (message instanceof SysexMessage) {
            strMessage = decodeMessage((SysexMessage) message);
        } else if (message instanceof MetaMessage) {
            strMessage = decodeMessage((MetaMessage) message);
        } else {
            strMessage = resources.getString("midi.recv.msg.unknown_msg_type");
        }

        noteHandler.note(message, lTimeStamp, strMessage);
    }
}