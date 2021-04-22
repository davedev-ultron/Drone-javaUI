class VideoStreamClient {

    constructor(droneId, hostname, port, endpoint) {
        this.droneId = droneId;
        this.webSocket = null;
        this.hostname = hostname;
        this.port = port;
        this.endpoint = endpoint;
    }

    activateStream() {
        // creating web socket and passing in the url
        this.webSocket = new WebSocket(this.getServerUrl());

        var activeDroneId = this.droneId;

        // when socket is created and connects to backed
        this.webSocket.onopen = function (event) {
            this.send(activeDroneId);
        }

        // when receiving message from backend
        this.webSocket.onmessage = function (event) {
            // finding html element with drone id and adding attribute src, byte array data
            $('#' + activeDroneId).attr("src", "data:image/jpg;base64," + event.data);
        }
    }

    // sending data to the backend
    // sendign drone id
    send(message) {
        if (this.webSocket != null && this.webSocket.readyState == WebSocket.OPEN) {
            this.webSocket.send(message);
        }
    }

    getServerUrl() {
        // generating url using variables
        return "ws://" + this.hostname + ":" + this.port + this.endpoint;
    }

    disconnect() {
        if (this.webSocket != null) {
            this.webSocket.close();
        }
    }
}

class Drone {
    constructor(id, lat, lng) {
        // can access variables from other files in js after compile WORLD_MAP
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.videoSocket = new VideoStreamClient(id, PUBLIC_IP, 80, VIDEO_ENDPOINT);
        this.posMark = new google.maps.Marker({
            position: { lat: lat, lng: lng },
            map: WORLD_MAP, 
            label: id + '',
            icon: 'drone.svg'
        });
        this.locationToPointDataMap = new Map();
        // label counter to increase the label of the markers
        this.labelCounter = 0;
        this.speed = 0.0;
        this.alt = 0.0
    }

    startMission() {
        $.ajax({
            // ajax call
            type: 'POST',
            url: '/startMission',
            data: {
                points: this.getPointDataJSON(),
                droneId: this.id
            }
        })
            .done(function (response) {
                console.log(response)
            })
            .fail(function (data) {
                console.log(data)
            });
    }

    sendCommand(commandId) {
        $.ajax({
            type: 'POST',
            url: '/sendCommand',
            data: { commandCode: commandId, droneId: this.id }
        })
            .done(function (response) {
                console.log(response)
            })
            .fail(function (data) {
                console.log(data)
            });
    }

    startVideoFeed() {
        // first stop all video feed then start needed one
        this.videoSocket.disconnect();
        this.videoSocket.activateStream();
    }

    stopVideoFeed() {
        this.videoSocket.disconnect();
    }

    setPosition(lat, lng, alt) {
        // to set position on map and update it
        this.posMark.setPosition({ lat: lat, lng: lng, alt: alt });
        this.lat = lat;
        this.lng = lng;
        this.alt = alt;
    }

    addPoint(marker) {
        // to add marker on map
        var pointId = Drone.createPointID(marker);
        var pointData = new PointData(marker, DEFAULT_SPEED, DEFAULT_ALTITUDE);
        this.locationToPointDataMap.set(pointId, pointData);
        return pointId;
    }

    static createPointID(marker) {
        return marker.getPosition().lat() + "" + marker.getPosition().lng();
    }

    getPointDataJSON() {
        var result = '[';
        this.locationToPointDataMap.forEach(function (pointData) {
            result += '{"lat":"' + pointData.marker.getPosition().lat() + '",' +
                '"lng":"' + pointData.marker.getPosition().lng() + '",' +
                '"speed":' + pointData.speed + ',' +
                '"height":' + pointData.height + ',' +
                '"action":' + pointData.action + '},';
        });
        return result.substring(0, result.length - 1) + ']';
    }

    getPointDataForID(key) {
        return this.locationToPointDataMap.get(key);
    }

    removePoint(key) {
        this.locationToPointDataMap.get(key).marker.setMap(null);
        this.locationToPointDataMap.delete(key);
    }

    hidePoints() {
        // will hide markers from view but they will still exist in memory
        this.locationToPointDataMap.forEach(function (pointData) {
            pointData.marker.setMap(null);
        });
    }

    showPoints() {
        this.locationToPointDataMap.forEach(function (pointData) {
            pointData.marker.setMap(WORLD_MAP);
        });
    }

    removePoints() {
        this.hidePoints();
        this.locationToPointDataMap = new Map();
        this.labelCounter = 0;
    }

    getNextLabelIndex() { 
        return ++this.labelCounter + "";
    }
}

class PointData {
    constructor(marker, speed, height) {
        // marker is from the google api
        this.marker = marker;
        this.speed = speed;
        this.height = height;
        this.action = 0;
    }
}

// when clicking on the map that will provide lat and long
// that will be passed in here
const addMarker = function (location) {
    //if there is no drone selected nothing wil happen
    if (SELECTED_DRONE == null || SELECTED_DRONE == undefined) {
        return;
    }

    var marker = new google.maps.Marker({
        position: location,
        draggable: true,
        label: SELECTED_DRONE.getNextLabelIndex(),
        map: WORLD_MAP
    });

    var pointId = SELECTED_DRONE.addPoint(marker);

    var contentString = renderMapPointDataComponent(pointId, DEFAULT_ALTITUDE, DEFAULT_SPEED);

    var infowindow = new google.maps.InfoWindow({
        // for the popup that shows up
        content: contentString
    });

    marker.addListener('click', function (event) {
        infowindow.open(WORLD_MAP, marker);
    });

    // when done dragging marker set new position
    marker.addListener('dragend', function (event) {
        marker.setPosition(event.latLng);
    });
}