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