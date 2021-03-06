LightFactory-OSC Proxy
============

[![Build Status](https://travis-ci.org/pcross616/lf-osc-proxy.svg?branch=master)](https://travis-ci.org/pcross616/lf-osc-proxy) [![Coverage Status](https://coveralls.io/repos/pcross616/lf-osc-proxy/badge.svg)](https://coveralls.io/r/pcross616/lf-osc-proxy)

LightFactory-OSC Proxy Service, enables Open Sound Control messages to be sent from LightFactory using the built in CONNECT and SEND functions. In addition to control LightFactory from a OSC enabled application.


Using LightFactory create a connection to the LightFactory-OSC Proxy service (may want to create a macro so it can be used in a cue)
  * `CONNECT <server or ip with the proxy>` - Once connected you can then send commands to this enpoint.
  * `SEND <server or ip> osc@<target osc endpoint> /message/command data` - the SEND option can be used with or without the ip

Using an OSC event to control LightFactory via LightFactory-OSC Proxy service
  * `connect to the LF-OSC Proxy from your OSC enabled application` - Once connected you can then send OSC events to this enpoint to hit any number of LightFactory instances.
  * `/lf/<lightfactory-ip>:<port>/<lf cli command data>` - the OSC event contains the destination LightFactory instance along with the LightFactory CLI command.


Download Binary Files
--------
  * Windows executable support
  * Self executing JAR file for other Operating Systems
  * https://bintray.com/pcross616/generic/lf-osc-proxy (https://dl.bintray.com/pcross616/generic/)

[![Get automatic notifications about new "lf-osc-proxy" versions](https://www.bintray.com/docs/images/bintray_badge_color.png)](https://bintray.com/pcross616/generic/lf-osc-proxy/view?source=watch)

Building LightFactory-OSC Proxy
--------
  * requires Java7 and Maven 3
  * `mvn clean package` - will create an all in one runnable jar file
  * `java -jar lf-osc-proxy-0.2-SNAPSHOT-shaded.jar` - Will run the proxy and listen on 127.0.0.1:3100 for LF commands and 127.0.0.1:3200 for OSC events, use -? to determine flags to change bind address and port.
 
Command Line Options
--------

    Option                                  Description
    ------                                  -----------
    -? [This help message]
    -b [bind address]                       (default: 127.0.0.1)
    -d [FATAL|ERROR|WARN|INFO|DEBUG|TRACE]
    -l [Integer: osc bind port]             (default: 3200)
    -m [Proxy mode (osc | bridge | both)]   (default: both)
    -p [Integer: bind port]                 (default: 3100)
    -t [Integer: max number of socket       (default: 100)
       threads]

Contribute
--------
Always looking to make this better.  If you have an idea but cannot code, create an issue and I will look at it.  Otherwise fork the project and have fun.  Once your done create a pull-request.  Please DO NOT increment the version in the POM file.  All PR should include tests and pass in Travis-CI.

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=GBMCJURP727AC)
