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
import com.xley.lfosc.ProtocolManager;

import java.util.List;

public class SimpleProtocolData implements IProtocolData {
    protected String protocol;
    protected String target;
    protected String operation;
    protected List<Object> data;

    protected SimpleProtocolData() {
    }

    ;

    public SimpleProtocolData(String protocol, String target, String operation, List<Object> data) {
        this.protocol = protocol;
        this.target = target;
        this.operation = operation;
        this.data = data;
    }

    @Override
    public String getType() {
        return this.protocol;
    }

    @Override
    public IProtocol getProtocol() {
        return ProtocolManager.getProtocol(this.protocol);
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public String getOperation() {
        return this.operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public List<Object> getDataList() {
        return this.data;
    }

    @Override
    public Object getData() {
        if (data == null) {
            return null;
        }
        StringBuilder values = new StringBuilder();
        for (Object val : this.data) {
            values.append(val).append(" ");
        }
        return values.toString();
    }

    @Override
    public IProtocolData configureProtocolData() {
        IProtocol protocol = getProtocol();
        if (protocol != null) {
            return protocol.configureProtocolData(this);
        }
        return null;
    }
}
