package com.example;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.example.functionality.DeviceThreadManager;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws MqttException, InterruptedException {

        DeviceThreadManager publisher = new DeviceThreadManager(15);
        // Start the publisher thread
        Thread publisherThread = new Thread(() -> publisher.publishWithRetry());
        publisherThread.start();

        // Start a thread to listen for input from the console
        Thread inputThread = new Thread(() -> {
            try {
                System.in.read();
                publisher.stop();
                System.out.println("Publisher stopped.");

            } catch (IOException e) {

                e.printStackTrace();
            }

        });
        inputThread.start();

        // Wait for both threads to finish
        publisherThread.join();
        inputThread.join();

        logger.info("MAIN ENDS");
    }

}
