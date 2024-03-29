package com.odafa.cloudapp.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.odafa.cloudapp.configuration.ConfigReader;
import com.odafa.cloudapp.dto.DroneInfo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor 
public class BaseController {

    // read variables from yaml
	private final ConfigReader configurations;

    @GetMapping("/")
    public String indexPage(Model model) {

		// variables need to match front end
		model.addAttribute("publicIp", getPublicIpAddress());
		model.addAttribute("defaultSpeed", configurations.getDefaultSpeed());
		model.addAttribute("defaultAltitude", configurations.getDefaultAltitude());
		model.addAttribute("videoEndpoint", configurations.getVideoWsEndpoint());

        return "index";
    }
    
    @GetMapping("/v/{droneId}")
	public String getVideoFeed(Model model, @PathVariable("droneId") String droneId) {
		
        // model is used to send the object to front end
        // variables are named the same as in video.hmtl
		model.addAttribute("publicIp", getPublicIpAddress());
		model.addAttribute("droneId", droneId);
		model.addAttribute("videoEndpoint", configurations.getVideoWsEndpoint());
        
        return "video";
    }

    //dynamically get ip address
	private String getPublicIpAddress() {
		String ip = "";
		try {
			final URL whatismyip = new URL("http://checkip.amazonaws.com");

			try(final BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()))){
				ip = in.readLine();
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return "192.168.1.154";
	}
}