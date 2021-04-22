package com.odafa.cloudapp.configuration;

import com.odafa.cloudapp.controller.VideoSocketHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final VideoSocketHandler videoSocketHandler;
    private final ConfigReader configurations;

    // necessary for websocket
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(102400);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // allowed connections from anywhere
        // here we also link the endpoint from the config /videofeed
        // to this handler
        registry.addHandler(videoSocketHandler, configurations.getVideoWsEndpoint()).setAllowedOrigins("*");
    }
}