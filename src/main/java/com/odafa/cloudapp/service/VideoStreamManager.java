package com.odafa.cloudapp.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.odafa.cloudapp.configuration.ConfigReader;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VideoStreamManager implements Runnable {
	//max udp packet size
    private static final int UDP_MAX_SIZE = 65507;

    private final int ID_LENGTH;

    //to save the different sessions
    // 1 to many relationship because 1 id can have many viewers and streams
	private final Map<String, Set<WebSocketSession>> droneIdToWebSocketSession;

    private final DatagramSocket videoReceiverDatagramSocket;
	// executor has thread pool because we need 1 thread that is consistently running listening and dispatching new data
	// you never want to start a thread yourself, you want to use executor to manage
	private final ExecutorService serverRunner;

	// this executes automatically upon start of spring app
    public VideoStreamManager(ConfigReader configuration) {
		// create datagram socket to listen on certain port
		try {
			videoReceiverDatagramSocket = new DatagramSocket(configuration.getVideoServerPort());
			log.info("Video Stream Manager Port: {}", configuration.getVideoServerPort());
		} catch (IOException e) {
            log.error(e.getMessage(), e);
			// blow up application if cannot open video feed
			throw new RuntimeException(e);
		}

		// if everything is successful we will create our executor
		serverRunner = Executors.newSingleThreadExecutor();
		// we have a drone id to session relationship, since we want to do it in a thread safe manner
		// we will use concurrent hash map
		droneIdToWebSocketSession = new ConcurrentHashMap<>();
        
        ID_LENGTH = configuration.getDroneIdLength();

		// we can start listening
        activate();
    }

	public void activate() {
		// since this class implements Runnable - this will execute the run() below
		serverRunner.execute(this);
        log.info("Video Stream Manager is Active");
	}

	public void run() {
		// continuously run until socket is closed
        while(!videoReceiverDatagramSocket.isClosed()){
			// we are receiving udp packet with drone id and then jpg data
			// dron id should be first 8 bytes because its only 1 char
			// we have to retrieve drone id
            try {
				// buffer data will be storing data from UDP packet
				byte[] buf = new byte[UDP_MAX_SIZE];
				// new datagram packet to store buffer
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				
				// here we will actually receive data
				// this is a blocking call and will block until we receive
				videoReceiverDatagramSocket.receive(packet);

				// extract drone id and the rest is raw jpeg data
				String droneId = new String( packet.getData(), 0, ID_LENGTH);
				String data = new String(packet.getData(), ID_LENGTH, packet.getLength());
                
				// using the droneId we will retrieve all the sessions that are currently
				// registered to receive frames
				Set<WebSocketSession> droneIdWebSessions = droneIdToWebSocketSession.get(droneId);
				
				// when there is no viewers or sessions
				if (droneIdWebSessions == null || droneIdWebSessions.isEmpty()) {
                    continue;
				}

				// if we did extract something then there is viewers
                // we do not want to use for loop or while
				// we may want to change the underlying collection
				Iterator<WebSocketSession> it = droneIdWebSessions.iterator();
				
				while(it.hasNext()) {
					// we extract socket session
					WebSocketSession session = it.next();
					if (!session.isOpen()) {
						// remove the session to prevent memory leak
						it.remove();
						continue;
					}
					// we send message if its open
					session.sendMessage(new TextMessage(data));
				}

            } catch(Exception e) {
				// throwing exception in here will only stop this thread and it will not
				// affect the rest of the app so 
				log.error(e.getMessage());
            }
        }
    }

    // save the session
    // thread safe mathod
    // to prevent adding 2 or more hash sets into the same collection
	public void setVideoWebSocketSessionForDroneId(WebSocketSession session, String droneId) {
        // if it does not exist then it will add it
        // and it will return the hash set in droneidSessions
        // if it already exists then it will return rull
		Set<WebSocketSession> droneIdSessions = droneIdToWebSocketSession.putIfAbsent(droneId, new HashSet<>());
		// null means its already created and we just need to extract it
        if(droneIdSessions == null) {
			droneIdSessions = droneIdToWebSocketSession.get(droneId);
		}
        // when we add it here it will make sure its the latest set of web socket session and will add new session
		droneIdSessions.add(session);
		log.debug("Drone ID {} has {} active Web Socket Sessions", droneId, droneIdSessions.size());
	}

	public boolean isServerClosed() {
		return videoReceiverDatagramSocket.isClosed();
	}

	public void shutdown() {
		if (!videoReceiverDatagramSocket.isClosed()) {
			try {
				videoReceiverDatagramSocket.close();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		serverRunner.shutdown();
	}
}

// tomcat is servlet based web container
// servlet is multi threaded so that means each request
// a new thread will be started
// so we need to implement method above in a thread safe manner