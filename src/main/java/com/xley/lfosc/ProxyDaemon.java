package com.xley.lfosc;

import com.illposed.osc.OSCPortIn;
import com.xley.lfosc.impl.OSCBridgeListener;
import com.xley.lfosc.impl.OSCProxyThread;
import joptsimple.OptionSet;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import static com.xley.lfosc.OSCProxy.logger;

public class ProxyDaemon implements Runnable {
    //configuration
    private OptionSet options;

    //daemon vars
    private Boolean shutdown = false;
    private Thread runner;

    //connections
    private ServerSocket serverSocket;
    private OSCPortIn receiver = null;


    ProxyDaemon(OptionSet options) {
        this.options = options;
    }

    public void shutdown() {
        logger.info("Shutting down...");
        shutdown = true;
        runner.interrupt();

        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (Throwable e) {
                logger.trace(e);
            }
        }
    }

    public boolean isAlive() {
        return runner.isAlive();
    }

    @Override
    public void run() {
        runner = Thread.currentThread();

        int portNumber = (int) options.valueOf("p");
        int oscPortNumber = (int) options.valueOf("l");
        int threads = (int) options.valueOf("t");
        boolean oscEnabled = true;
        boolean lfBridgeEnabled = true;

        switch (String.valueOf(options.valueOf("m"))) {
            case "osc":
                oscEnabled = true;
                lfBridgeEnabled = false;
                break;
            case "bridge":
                oscEnabled = false;
                lfBridgeEnabled = true;
                break;
            default:
                break;
        }

        String host = String.valueOf(options.valueOf("b"));

        try {
            //bindings
            OSCBridgeListener listener;
            InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), portNumber);
            InetSocketAddress oscBinding = new InetSocketAddress(InetAddress.getByName(host), oscPortNumber);

            if (oscEnabled) {
                logger.info("Listening for OSC Events on " + host + ":" + oscPortNumber);
                receiver = new OSCPortIn(new DatagramSocket(oscBinding));
                listener = new OSCBridgeListener();
                receiver.addListener("/lf/*/*", listener);
                receiver.startListening();

                //check to see if we are the only listener to run.
                if (!lfBridgeEnabled) {
                    while (!shutdown) {
                        if (!receiver.isListening() || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    }
                }

            }
            if (lfBridgeEnabled) {
                try {
                    serverSocket = new ServerSocket(binding.getPort(), threads, binding.getAddress());
                    logger.info("Listening for LightFactory connection on " + host + ":" + portNumber);
                    while (!shutdown && !Thread.currentThread().isInterrupted()) {
                        new OSCProxyThread(serverSocket.accept()).start();
                    }
                } catch (IOException e) {
                    if (!shutdown) {
                        logger.fatal("Could not listen for LightFactory on " + host + ":" + portNumber, e);
                    }
                }
            }

        } catch (Exception e) {
            logger.fatal("LightFactory - OSC Proxy encountered an error.", e);
        }
        finally {
            if (receiver != null)
                receiver.stopListening();
        }
    }
}
