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

package com.xley.lfosc.osc.server;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.ProtocolManager;
import com.xley.lfosc.UnknownProtocolException;
import com.xley.lfosc.util.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.nio.ByteBuffer;

public class OSCServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final OSCByteArrayToJavaConverter converter = new OSCByteArrayToJavaConverter();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        //read the data in
        ByteBuffer byteBuffer = datagramPacket.content().nioBuffer();
        final byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.duplicate().get(bytes);

        OSCPacket packet = converter.convert(bytes, bytes.length);
        processPacket(packet);
    }

    /**
     * Process the incomming OSC packet using Netty worker pool.
     *
     * @param bundle the bundle to process
     */
    private void processPacket(OSCPacket bundle) throws ProtocolException {
        if (bundle instanceof OSCBundle) {
            for (OSCPacket packet : ((OSCBundle) bundle).getPackets()) {
                processPacket(packet);
            }
        } else {
            IProtocolData data = ProtocolManager.resolve(bundle);
            if (data == null) {
                throw new UnknownProtocolException("Unknown OSC packet protocol");
            }
            data.getProtocol().process(data);
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LogUtil.error(getClass(), cause);
    }

}
