package com.odafa.cloudapp.controller;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.odafa.cloudapp.dto.DataPoint;
//import com.odafa.cloudapp.dto.DataPoint;
import com.odafa.cloudapp.dto.DroneInfo;
import com.odafa.cloudapp.service.ControlManager;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j // logging
@RestController // by default indicates we will return data not template
@RequiredArgsConstructor
public class BaseRestController {
    
	private final ControlManager controlManager; 

	@GetMapping("/updateSystemInfo")
	public String updateSystemInfo() {

		final Gson gson = new Gson();

        final List<DroneInfo> drones = controlManager.getDroneStatusAll();
        //final List<DroneInfo> drones = new ArrayList<>();

		// mock data
		DroneInfo dto1 = new DroneInfo("3", -35.361768, 149.1669463, 0.0468, 7.1, 12.3, "ONLINE");
		DroneInfo dto2 = new DroneInfo("4", -35.363168, 149.1682463, 0.0468, 3, 21, "ON MISSION");
		drones.add(dto1);
		drones.add(dto2);

		// google library to return json
		// in actual practice this should be done not in the controller
		return gson.toJson(drones);
	}

    @PostMapping("/startMission")
	public String startMission( @RequestParam("points") String points, @RequestParam("droneId") String droneId) {
		// receives point data from UI
        log.debug("From Drone ID {} Received Points Data: {}", droneId, points);
		if(points == null || points.trim().length() < 5) {
			return "fail";
		}

		final Gson gson = new Gson();
		final List<DataPoint> deserializedPoints = new ArrayList<>();

		// parsing object from UI with each point
		for (Object obj : gson.fromJson(points, List.class)) {
			deserializedPoints.add( gson.fromJson(obj.toString(), DataPoint.class));
		}

		controlManager.sendMissionDataToDrone(droneId, deserializedPoints);

        return "ok";
    }

    @PostMapping("/sendCommand")
	public String sendCommand(@RequestParam("droneId") String droneId, @RequestParam("commandCode") String commandCode) {
		log.debug("Received command code {} for Drone ID {}", commandCode, droneId);
		if(commandCode == null || commandCode.trim().length() < 1) {
			return "null";
		}

		controlManager.sendMessageFromUserIdToDrone(droneId, Integer.parseInt(commandCode));

        return "ok";
    }
    
}