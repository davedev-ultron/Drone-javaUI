
package com.odafa.cloudapp.controller;

import java.io.IOException;

import com.odafa.cloudapp.service.VideoStreamManager;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSocketHandler extends AbstractWebSocketHandler {
	
    //object responsible for managing all connections and receiving udp data and store drone ids with sessions
	private final VideoStreamManager videoStreamManager;
	
    // after socket connection is established from front end this receives the session
    // and we log ip and sesh id
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.debug("WebSocket Connection OPEN. Session {} IP {}", session.getId(), session.getRemoteAddress());
	}
    
    // we log on connection close
    @Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.debug("WebSocket Connection CLOSED. Session {} IP {} {}", 
                       session.getId(), session.getRemoteAddress(), closeStatus);
	}
	
    // when the front end sends message we will receive it here
    // will extract id and can find appropiate session
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage droneId) throws IOException {
    	videoStreamManager.setVideoWebSocketSessionForDroneId(session, droneId.getPayload());
    } 
    
}