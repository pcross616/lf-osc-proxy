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

package com.xley.lfosc.impl;

import com.xley.lfosc.IProtocol;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.util.LogUtil;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

public abstract class BaseProtocol implements IProtocol {
    /**
     * The constant resources.
     */
    private static final ResourceBundle resources = ResourceBundle.getBundle(BaseProtocol.class.getSimpleName(),
            Locale.getDefault());

    @Override
    public IProtocolData configureProtocolData(IProtocolData data) {
        try {
            String operation = URLDecoder.decode((String) data.getOperation(), Charset.defaultCharset().name());

            List<Object> argsList = null;
            if (operation.contains(" ")) {
                String[] args = operation.substring(operation.indexOf(" ")).trim().split(" "); //find the start of the arguments
                operation = operation.substring(0, operation.indexOf(" ")); //find the start of the arguments
                argsList = new ArrayList<>();
                Collections.addAll(argsList, args);
            }
            if (argsList == null) {
                argsList = data.getDataList();
            } else if (data.getDataList() != null) {
                argsList.addAll(data.getDataList());
            }

            return new SimpleProtocolData(data.getType(),
                    URLDecoder.decode((String) data.getTarget(), Charset.defaultCharset().name()),
                    operation, argsList);

        } catch (Exception e) {
            LogUtil.warn(getClass(), e);
            return data;
        }
    }

    @Override
    public int command(String command, String[] args) throws ProtocolException {
        LogUtil.error(resources.getString("protocol.command.not_found"));
        return -1;
    }
}
