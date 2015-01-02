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

package com.xley.lfosc.http;

import com.xley.lfosc.IProtocol;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.http.server.IHttpConstants;
import com.xley.lfosc.impl.BaseProtocol;
import com.xley.lfosc.impl.SimpleProtocolData;
import com.xley.lfosc.util.LogUtil;

import java.util.Locale;
import java.util.ResourceBundle;

public class HttpProtocol extends BaseProtocol implements IProtocol {


    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(HttpProtocol.class.getSimpleName(),
            Locale.getDefault());

    /**
     * Get the protocol segment from the URL.
     */
    private static final int EVENT_PROTOCOL = 1;


    /**
     * Get the target segment from the URL.
     */
    private static final int EVENT_TARGET = 2;


    @Override
    public Object process(IProtocolData data) throws ProtocolException {
        //HTTP does not process any direct protocols (eg.. http)
        if (data.getType().equals(IHttpConstants.HTTP_PROTOCOL)) {
            throw new ProtocolException(resources.getString("http.error.cannot_process_http"));
        }
        return data.getProtocol().process(data);
    }

    @Override
    public IProtocolData createProtocolData(Object value) {
        if (value == null || !(value instanceof CharSequence)) {
            return null;
        }

        String url = String.valueOf(value);

        //make sure its a url
        if (!url.startsWith("/")) {
            return null;
        }

        //do we have a query string?
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf('?'));
        }
        String[] parts = url.split("/");
        if (parts.length >= 4) {
            try {
                String protocol = parts[EVENT_PROTOCOL];
                String target = parts[EVENT_TARGET];
                SimpleProtocolData pdata = new SimpleProtocolData(protocol, target, url.replace("/" + protocol + "/" + target + "/", ""), null);
                return pdata.configureProtocolData();
            } catch (Exception e) {
                LogUtil.warn(getClass(), e);
            }
        }
        return null;
    }

    @Override
    public IProtocolData configureProtocolData(IProtocolData data) {
        return data;
    }
}
