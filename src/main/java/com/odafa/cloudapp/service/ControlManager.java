package com.odafa.cloudapp.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.odafa.cloudapp.configuration.ConfigReader;
import com.odafa.cloudapp.dto.DataPoint;
import com.odafa.cloudapp.dto.DroneInfo;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// in enterprise development we should code against interface
@Slf4j
@Component
public class ControlManager implements Runnable {
    // when python app creates connection with back end (this)
    // python will connect to a certain port
    // this will be done by serverSocket so we need it to open the port
    // when raspi connects to the port we will create drone handler object
    // we will pass this object and we will put dronehandler in the map
    // drone handler is abstraction that contains all the logic to
    // manage the connection and data coming back and forth from raspi
	private final ServerSocket serverSocket;
	private final ExecutorService serverRunner;
	
	private final Map<String, DroneHandler> droneIdToHandler; // this collection keeps all drone id and handlers
	
    // need to pass config reader to know which port to open
	public ControlManager(ConfigReader configurations) {
		try {
			// activating socket
			serverSocket = new ServerSocket( configurations.getControlServerPort());
		} catch (IOException e) {
            // if resource is taken then we will blow up app
			log.error(e.getMessage());
			throw new RuntimeException(e);
		}

        // create singlethreadexecutor because we dont want to manage threads ourselves
        // we will have one thread listening and accepting connections on our socket
		serverRunner = Executors.newSingleThreadExecutor();
		droneIdToHandler = new ConcurrentHashMap<>();

        // since this class is runnable and we are passing it into the following method
        // it will execute the run() method below
		serverRunner.execute(this);
	} 
	
	public void run() {
        // will execute until the socket is open
		while (!serverSocket.isClosed()) {
            // when connetion happens it will pack connection into drone handler
            // and will activate then it does this over and over again
			try {
				Socket clientSocket = serverSocket.accept();

                // we are passing in client socket because we are going to be using TCP
                // TCP does not lose packages unlike UDP
                // TCP is slower but guarantees every package is received
				// we are passing it because we want it to read from the soket

                // if connection is lost it will remove it from the map and close
                // resources to avoid memory leak

                // connection is mapped to drone id
				final DroneHandler handler = new DroneHandler(this, clientSocket);
				handler.activate();
				// this thread will run until socketis open exception will not kill it

			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}

    public void sendMissionDataToDrone(String droneId, List<DataPoint> dataPoints) {
        // we want to send data to certain drone so we extract the handler for that id
        // controller does not care how handler sends data
		final DroneHandler handler = droneIdToHandler.get(droneId);
		if(handler != null) {
			handler.sendMissionData(dataPoints);
		}
    }

    public void sendMessageFromUserIdToDrone(String droneId, int commandCode) {
        // we want to send data to certain drone so we extract the handler for that id
        // controller does not care how handler sends data
		final DroneHandler handler = droneIdToHandler.get(droneId);
		if(handler != null) {
			handler.sendCommand(commandCode);
		}
    }

    public List<DroneInfo> getDroneStatusAll() {
		List<DroneInfo> drones = new ArrayList<>();

        // we are iterating through all the values that are drone handlers
        // and we are obtaining status
		droneIdToHandler.values().forEach( handler -> {
			drones.add(handler.getDroneLastStatus());
		});
		return drones;
    }
	
	// this method will allow it to register itself
	// set control handler for drone id
	public void setControlHadlerForDroneId(String droneId, DroneHandler handler) {
		droneIdToHandler.put(droneId, handler);
	}
	
	// will allow it to remove itself if connection is lost
	public void removeControlHadlerForDroneId(String droneId) {
		droneIdToHandler.remove(droneId);
	}
    
}