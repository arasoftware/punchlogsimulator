package com.example;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.HighResolutionTimer;
import org.h2.tools.Server;

import com.example.functionality.DeviceThreadManager;
import com.example.functionality.H2DatabaseManager;
import com.example.functionality.PushInMemoryLogSimulator;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws MqttException, InterruptedException {
        chooseAndRunPath(args);
        logger.info("MAIN ENDS");
    }

    private static void chooseAndRunPath(String[] args) throws MqttException, InterruptedException {
        if (args.length == 0) {
            logger.error(
                    "PASS parameter 'static - to publish given number of message' or 'dynamic - dynamically publish without stopping' ");
            return;
        }
        String command = args[0];
        switch (command.toLowerCase()) {
            case "dynamic":
                dynamicLogPushLogic();
                break;
            case "static":
                if (args.length < 6) {
                    logger.error("number of devices, employees and logs are necessary to run... please provide it.");
                    return;
                }

                int numberOfDevices = Integer.parseInt(args[1]);
                int numberOfEmployees = Integer.parseInt(args[2]);
                int numberOfLogs = Integer.parseInt(args[3]);
                int deviceIdStart = Integer.parseInt(args[4]);
                String deviceNamePrefix = args[5];
                staticLogPush(numberOfDevices, numberOfEmployees, numberOfLogs, deviceIdStart, deviceNamePrefix);
                break;

            default:
                logger.error("Invalid command, must be either 'dynamic' or 'static' ");
                break;
        }
        logger.info("choose And Run Path done.");
    }

    private static void staticLogPush(int numberOfDevices, int numberOfEmployees, int numberOfLogs,
            int deviceIdStart, String deviceNamePrefix)
            throws MqttException {

        PushInMemoryLogSimulator pushInMemoryLogSimulator = new PushInMemoryLogSimulator(numberOfDevices,
                numberOfEmployees, numberOfLogs, deviceIdStart, deviceNamePrefix);

        pushInMemoryLogSimulator.prepareAndRun();
    }

    private static void dynamicLogPushLogic() throws MqttException, InterruptedException {
        // H2DatabaseManager.dropTable();

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
    }

}
