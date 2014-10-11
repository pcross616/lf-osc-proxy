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

package com.xley.lfosc.util;

import com.xley.lfosc.OSCProxy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogUtil {

    /**
     * The constant LogUtil.
     */

    private static Map<Class, Logger> loggerMap = new ConcurrentHashMap<>();
    private static Level level = null;

    public static void trace(Object message) {
        getLogger(OSCProxy.class).trace(message);
    }

    public static void debug(Object message) {
        getLogger(OSCProxy.class).debug(message);
    }

    public static void info(Object message) {
        getLogger(OSCProxy.class).info(message);
    }

    public static void warn(Object message) {
        getLogger(OSCProxy.class).warn(message);
    }

    public static void error(Object message) {
        getLogger(OSCProxy.class).error(message);
    }

    public static void error(Object message, Throwable throwable) {
        getLogger(OSCProxy.class).error(message, throwable);
    }

    public static void fatal(Object message) {
        getLogger(OSCProxy.class).fatal(message);
    }

    public static void fatal(Object message, Throwable throwable) {
        getLogger(OSCProxy.class).fatal(message, throwable);
    }

    public static void trace(Class clazz, Object message) {
        getLogger(clazz).trace(message);
    }

    public static void debug(Class clazz, Object message) {
        getLogger(clazz).debug(message);
    }

    public static void info(Class clazz, Object message) {
        getLogger(clazz).info(message);
    }

    public static void warn(Class clazz, Object message) {
        getLogger(clazz).warn(message);
    }

    public static void error(Class clazz, Object message) {
        getLogger(clazz).error(message);
    }

    public static void error(Class clazz, Object message, Throwable throwable) {
        getLogger(clazz).error(message, throwable);
    }

    public static void fatal(Class clazz, Object message) {
        getLogger(clazz).fatal(message);
    }

    public static void fatal(Class clazz, Object message, Throwable throwable) {
        getLogger(clazz).fatal(message, throwable);
    }

    private static Logger getLogger(Class clazz) {
        Logger log = Logger.getLogger(clazz);
        if (level != null && !loggerMap.containsKey(clazz)) {
            log.setLevel(level);
        }
        loggerMap.put(clazz, log);
        return log;
    }

    public static void setLevel(Level logLevel) {
        level = logLevel;
        for (Logger log : loggerMap.values()) {
            log.setLevel(level);
        }
    }
}

