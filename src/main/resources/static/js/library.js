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
            // finding html element with video + drone id and adding attribute src, byte array data
            $('#video' + activeDroneId).attr("src", "data:image/jpg;base64," + event.data);
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

//control buttons
const initializeDronesControls = function (id) {
    // use jquery to find button with id
    $("input[id*='fKill" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.KILL);
    });

    $("input[id*='fActivate" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.ACTIVATE_FUNCTION);
    });

    $("input[id*='cameraLEFT" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.CAMERA_LEFT);
    });

    $("input[id*='cameraRIGHT" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.CAMERA_RIGHT);
    });

    $("input[id*='cameraMID" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.CAMERA_MID);
    });

    $("input[id*='btnF" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.FORWARD);
    });

    $("input[id*='btnMvL" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.MLEFT);
    });

    $("input[id*='btnMvR" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.MRIGHT);
    });

    $("input[id*='btnCncl" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.STOP);
    });

    $("input[id*='btnB" + id + "']").click(function () {
        SELECTED_DRONE.sendCommand(CommandType.BACKWARD);
    });
}

// command enum
const CommandType = {
    FORWARD: 1,
    BACKWARD: 2,
    MLEFT: 3,
    MRIGHT: 4,
    STOP: 5,
    KILL: 5,
    CAMERA_LEFT: 10,
    CAMERA_MID: 11,
    CAMERA_RIGHT: 12,
    ACTIVATE_FUNCTION: 13,
}

// keyboard control
const executeKeyboardCommand = function (event) {
    switch (event.key) {
        case 'w':
            SELECTED_DRONE.sendCommand(CommandType.FORWARD);
            break;
        case 's':
            SELECTED_DRONE.sendCommand(CommandType.BACKWARD);
            break;
        case 'a':
            SELECTED_DRONE.sendCommand(CommandType.MLEFT);
            break;
        case 'd':
            SELECTED_DRONE.sendCommand(CommandType.MRIGHT);
            break;
        case ' ':
            SELECTED_DRONE.sendCommand(CommandType.STOP)
            break;

        case 'r':
            SELECTED_DRONE.sendCommand(CommandType.CAMERA_LEFT);
            break;
        case 'f':
            SELECTED_DRONE.sendCommand(CommandType.CAMERA_MID);
            break;
        case 'v':
            SELECTED_DRONE.sendCommand(CommandType.CAMERA_RIGHT);
            break;
        case 'c':
            SELECTED_DRONE.sendCommand(CommandType.ACTIVATE_FUNCTION);
            break;
    }
}

// called from within the maps pop up
const removePoint = function (form) {
    SELECTED_DRONE.removePoint(form["key"].value);
}

const updatePointValue = function (form) {
    var pointData = SELECTED_DRONE.getPointDataForID(form["key"].value);
    pointData.speed = form["speed"].value;
    pointData.height = form["height"].value;
    pointData.action = form["action"].value;
}