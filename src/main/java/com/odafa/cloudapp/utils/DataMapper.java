package com.odafa.cloudapp.utils;

import java.io.InputStream;
import java.net.Socket;
import java.util.List;

import com.odafa.cloudapp.dto.DataPoint;
import com.odafa.cloudapp.dto.DroneInfo;

public class DataMapper {

	// we use a socket connection and we send TCP IP to send byte array
	// how do we encode java objects into byte array, we will use protobuf
	// we could use json and encode it into string of bytes but thats not very effective
	// we want to make it light weight, protobuf is round 13 times more compact
	// protobuf receives object object and ecodes into byte array and vice versa
	
    private static final int START_MISSION_CODE = 14;

    public static String extractDroneIdFromNetwork(Socket droneSocket) throws Exception {
		// do not need protobuf here, the first network message is id
		return new String( NetworkFormatter.readNetworkMessage( droneSocket.getInputStream()));
    }

    public static DroneInfo fromNetworkToDroneInfo(InputStream streamIn) throws Exception {
		// we read from network
		byte[] result = NetworkFormatter.readNetworkMessage(streamIn);
		// we can use protobuf to parse and acccess data
		final ProtoData.DroneData droneData = ProtoData.DroneData.parseFrom(result);
		// convert from meters/sec to kph
		final float speedInKmH = droneData.getSpeed() * 3.6f;

		return new DroneInfo(droneData.getDroneId(), droneData.getLatitude(), droneData.getLongitude(), speedInKmH,
				                droneData.getAltitude(), droneData.getVoltage(), droneData.getState());
    }

    public static byte[] toNetworkMessage(List<DataPoint> dataPoints) {

		// make builder
		ProtoData.MissionData.Builder missionData = ProtoData.MissionData.newBuilder();

		for (DataPoint point : dataPoints) {
			missionData.addPoint( ProtoData.DataPoint.newBuilder()
					                                 .setLatitude(point.getLat())
					                                 .setLongitude(point.getLng())
					                                 .setSpeed(point.getSpeed())
					                                 .setAltitude(point.getHeight())
					                                 .setAction(point.getAction())
					                                 .build());
		}

		// sending commands with mission data as payload
		byte[] missionDataArr = ProtoData.Command.newBuilder().setCode(START_MISSION_CODE)
		                                         .setPayload( missionData.build().toByteString())
		                                         .build().toByteArray();

		return NetworkFormatter.createNetworkMessage(missionDataArr);
    }
    
    public static byte[] toNetworkMessage(int commandCode) {
		// Below we access our protodata model, then the command object, then the builder
		// we set the code to our command code,  build it, and set it to byte array
		// then network formatter takes care of sending the bytes
		byte[] command = ProtoData.Command.newBuilder().setCode(commandCode).build().toByteArray();
		return NetworkFormatter.createNetworkMessage(command);
    }
    
}