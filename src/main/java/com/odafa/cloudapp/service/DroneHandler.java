package com.odafa.cloudapp.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.odafa.cloudapp.dto.DataPoint;
import com.odafa.cloudapp.dto.DroneInfo;
import com.odafa.cloudapp.utils.DataMapper;

import lombok.extern.slf4j.Slf4j;
// the core of our application
@Slf4j
public class DroneHandler {
	private static final long MAX_WAIT_TIME = 10_000L;
	private final String droneId;

	private volatile long lastUpdateTime;
	private DroneInfo lastStatus;
	
	private final Socket droneSocket;
	private final InputStream streamIn;
	private final OutputStream streamOut;
	
	private final BlockingQueue<byte[]> indoxMessageBuffer;
	private final ExecutorService handlerExecutor;
	private final ControlManager manager;

    public DroneHandler(ControlManager controlManager, Socket clientSocket) {
		this.manager = controlManager;
		this.droneSocket = clientSocket;
		this.indoxMessageBuffer = new ArrayBlockingQueue<>(1024);
		// executor contains 2 threads one for sending and one for receiving
		this.handlerExecutor = Executors.newFixedThreadPool(2);
		try {
			// two streams one thread sending one receiving
			// our app can send many commands and may take some time to send
			// we do not want that to block any input operation
			// outgoing data will be added to indoxMessageBuffer until its sent
			// app thread will be adding data and drone handler will be consuming and sending
			this.streamIn  = droneSocket.getInputStream();
			this.streamOut = droneSocket.getOutputStream();
			// network managing entity, can extract data from network and map to objects and the other way when sending
			// here we use to extract id
			// Data mapper can help us extract data
			droneId = DataMapper.extractDroneIdFromNetwork(droneSocket);
		} catch (Exception e) {
			close();
			throw new RuntimeException(e);
		}
		// if pulling drone id is successful we register the handler here
		manager.setControlHadlerForDroneId(droneId, this);
		log.info("Control Connection Established ID {}, IP {} ", droneId, droneSocket.getInetAddress().toString());
    }

    public void activate() {
		// receiveer thread
		// lambda implements runnable interface
		// the thread will be executing the code inside the lambda 
		// will run in endless loop until socket is open
		handlerExecutor.execute( () -> {
			while (!droneSocket.isClosed()) {
				try {
					// extracts data from  the network (streamIn) and maps it
					// to drone info (laststatus) object
					// last update time we know when the last time we successfully received data
					// this method will be blocking since its reading from network
					// if there is no data the thread will block and wait until data received
					// everytime it receives data it creates a new droneinfo object so garbage collector will have to
					// clean this up, this can be optimized to reuse object instead
					this.lastStatus = DataMapper.fromNetworkToDroneInfo(streamIn);
					this.lastUpdateTime = System.currentTimeMillis();
				} catch (Exception e) {
					log.info("Control Connection with {} Closed, reason: {}", droneSocket.getInetAddress().toString(), e.getMessage());
					close();
					// we close objects if it fails
				}
			}
			close();
		});

		// thread to send data also runs endlessly until socket is open
		handlerExecutor.execute( () -> {
			while (!droneSocket.isClosed()) {
				try {
					// extracts data from buffer, sends it to straeam, and we flush it
					// flush forcefully will push data to stream
					// also blocking until data is sent
					streamOut.write( indoxMessageBuffer.take());
					streamOut.flush();
				} catch (SocketException se) {
					log.info("Socket has been closed: {}", se.getMessage());
					close();
				} catch (Exception e) {
					log.error(e.getMessage());
				}
			}
		});

		// we have decoupled input output in differenct thread so we dont blocked
    }

	// application thread adds data
	// drone thread receives and sends
	// application thread reads

    public void sendMissionData(List<DataPoint> dataPoints) {
		// we rely on data mapper to translate to byte array and adds to buffer
		final byte[] message = DataMapper.toNetworkMessage(dataPoints);
		this.indoxMessageBuffer.add(message);

		log.debug("Sending Mission Data: {}", dataPoints);
    }

    public void sendCommand(int commandCode) {
		// we extract from the message command code
		// we add it to the message buffer
		final byte[] message = DataMapper.toNetworkMessage(commandCode);
		this.indoxMessageBuffer.add(message);
		
		log.debug("Sending Command Code: {} For Drone ID {}", commandCode, droneId);
    }

    public DroneInfo getDroneLastStatus() {
		// front end each second is polling
		// so we need to make sure that this data is fresh and not stale
		// if its been more than 10 seconds it will send blanks data and will close socket
		if (isMaxWaitTimeExceeded()) {
			
			log.warn("Maximum Wait Time for Drone ID {} exceeded. Control socket closed", droneId);
			close();
		}
		// from thread
		return this.lastStatus;
    }

	private boolean isMaxWaitTimeExceeded() {
		return System.currentTimeMillis() - lastUpdateTime > MAX_WAIT_TIME;
	}

	private void close() {
		try {
			// close drone socket
			droneSocket.close();
		} catch (Exception e) {
			log.error(e.getMessage());
		} 
		try {
			streamIn.close();
		} catch (Exception e) {
			log.error(e.getMessage());
		} 
		try {
			streamOut.close();
		} catch (Exception e) {
			log.error(e.getMessage());
		} 
		// remove from drone handler
		manager.removeControlHadlerForDroneId(droneId);
		// we call thread executor and shut it down
		handlerExecutor.shutdownNow(); 
	}
}