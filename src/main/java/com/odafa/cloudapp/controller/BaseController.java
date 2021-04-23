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
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor 
public class BaseController {

    // read variables from yaml
	private final ConfigReader configurations;

	@ResponseBody // this tells spring that were returning json data and not a view/template
	@GetMapping("/updateSystemInfo")
	public String updateSystemInfo() {

		final Gson gson = new Gson();

		final List<DroneInfo> drones = new ArrayList<>();
		
		// mock data
		DroneInfo dto1 = new DroneInfo("1", -35.361768, 149.1669463, 0.0468, 7.1, 12.3, "ONLINE");
		DroneInfo dto2 = new DroneInfo("2", -35.363168, 149.1682463, 0.0468, 3, 21, "ON MISSION");
		drones.add(dto1);
		drones.add(dto2);

		// google library to return json
		// in actual practice this should be done not in the controller
		return gson.toJson(drones);
	}

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
		return ip;
	}
}