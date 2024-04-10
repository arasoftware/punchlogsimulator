package com.example;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.HighResolutionTimer;
import org.h2.tools.Server;

import com.example.functionality.DeviceThreadManager;
import com.example.functionality.H2DatabaseManager;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger logger = LogManager.getLogger(App.class);


    public static void main(String[] args) throws MqttException, InterruptedException {
        
   

       //  H2DatabaseManager.dropTable();

        H2DatabaseManager.initializeDatabase();

        DeviceThreadManager publisher = new DeviceThreadManager(50);
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
