/*
 * Copyright (c) 2015. Peter Crossley (xley.com)
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

package com.xley.lfosc.http.server;

/**
 * HTTP Constants used for the HTTP protocol
 */
public interface IHttpConstants {

    String HTTP_PROTOCOL = "http";
    String HTTPS_PROTOCOL = "https";
    int HTTP_PROTOCOL_PORT = 80;
    int HTTPS_PROTOCOL_PORT = 443;

    String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    String CONTENT_TYPE_APPLICATION_JS = "application/javascript";

    String JSONP_CALLBACK = "callback";

    String JSON_ERROR = "error";
    String JSON_DATA = "data";

}
