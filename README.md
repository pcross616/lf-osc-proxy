lf-osc-proxy
============

[![Build Status](https://travis-ci.org/pcross616/lf-osc-proxy.svg?branch=master)](https://travis-ci.org/pcross616/lf-osc-proxy)

Light Factory OSC Proxy Service, enables Open Sound Control messages to be sent from Light Factory using the built in CONNECT and SEND functions.


Using Lightfactory create a connection to the OSC Proxy service (may want to create a macro so it can be used in a cue)
  * `CONNECT <server or ip with the proxy>` - Once connected you can then send commands to this enpoint.
  * `SEND <server or ip> osc@<target osc enpoint> /message/command data` - the SEND option can be used with or without the ip



Building Lightfactory OSC Proxy

  * `mvn clean assembly:assembly` - will create an all in one runnable jar file
  * `java -jar lf-osc-proxy-0.1-SNAPSHOT-jar-with-dependencies.jar` - Will run the proxy and listen on 127.0.0.1:3100, use -? to determine flags to change bind address and port.
