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

package com.xley.lfosc;

import com.xley.lfosc.util.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

public abstract class ProtocolManager {

    /**
     * The constant resources.
     */
    private static final ResourceBundle resources = ResourceBundle.getBundle(ProtocolManager.class.getSimpleName(),
            Locale.getDefault());


    private static final Properties config = new Properties();
    private static final Map<String, IProtocol> protocolMap = new HashMap<>();

    static {
        InputStream configData = null;
        try {
            configData = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("protocol.properties");

            config.load(configData);
            Enumeration protoTypes = config.propertyNames();

            //load all of the protocols defined
            while (protoTypes.hasMoreElements()) {
                String key = (String) protoTypes.nextElement();
                protocolMap.put(key, (IProtocol) Thread.currentThread().getContextClassLoader()
                        .loadClass(config.getProperty(key)).newInstance());
            }
        } catch (Exception e) {
            LogUtil.fatal(ProtocolManager.class, e);
        } finally {
            if (configData != null) {
                try {
                    configData.close();
                } catch (IOException e) {
                    LogUtil.fatal(ProtocolManager.class, e);
                }
            }
        }


    }

    public static IProtocol getProtocol(String mode) {
        return protocolMap.get(mode.toLowerCase());
    }

    public static IProtocolData resolve(Object value) {
        for (IProtocol protocol : protocolMap.values()) {
            IProtocolData data = protocol.createProtocolData(value);
            if (data != null) {
                LogUtil.trace(ProtocolManager.class, MessageFormat.format(resources.getString("protocol.data.dump"),
                        data.getProtocol(),
                        data.getTarget(),
                        data.getOperation(),
                        data.getType(),
                        data.getData()));
                return data.configureProtocolData();
            }
        }
        return null;
    }

    public static void addProtocol(String mode, IProtocol protocol) {
        protocolMap.put(mode, protocol);
    }

    public static int executeCommand(String mode, String command, String[] args) throws ProtocolException {
        IProtocol protocol = getProtocol(mode);
        if (protocol == null) {
            return -1;
        }
        return protocol.command(command, args);
    }
}
