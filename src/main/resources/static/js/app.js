var dronesCount = 0;
// map with all the drones
const DRONES_MAP = new Map();

// reference current drone from the list
var SELECTED_DRONE;
var WORLD_MAP;

// initialize the app
google.maps.event.addDomListener(window, 'load', initializeApp);


function initializeApp() {

    // create our google map
	WORLD_MAP = new google.maps.Map( document.getElementById('map'), {
        // 0 zoom is further, greater zoom is closer
		zoom: 2,
        // starting location for map
		center: { lat: 0, lng: 0 }
	});

    // when we click on the map
	google.maps.event.addListener(WORLD_MAP, 'click', function (event) {
		addMarker(event.latLng);
	});

    // this is for controlling using keyboard, when we release key
    // every second this is going to call the backend to obtain data
    // about drone location
	document.addEventListener('keyup', function (event) {
		executeKeyboardCommand(event);
	});

    // every 1000ms this is executed
	setInterval( updateSystemData, 1000);
}

const updateSystemData = function () {
    // using ajax to do a get on our endpoint
	$.ajax({
		type: 'GET',
		url: '/updateSystemInfo',
	})
		.done(function (response) {

			loadDronesData(response);

            // if we selected a drone then set focus the map on it
			if (SELECTED_DRONE != undefined) {
				WORLD_MAP.setCenter({ lat: SELECTED_DRONE.lat, lng: SELECTED_DRONE.lng });
			}

		})
		.fail(function (data) {
			loadDronesData('[{}]');
		});
}



const loadDronesData = function (data) {
	var dronesDTOs = JSON.parse(data);

    // set the status to offline by default
	$("p[id*='onlineStatus']").html('OFFLINE');

	dronesDTOs.forEach(function (droneDTO) {

        // the drone dto object will contain the DroneInfo.java object
		if (droneDTO == undefined || droneDTO.id == undefined) {
			return;
		}

        // if the drone alreadt exists in the list or if not
		if (DRONES_MAP.has(droneDTO.id)) {
            // jquery to find components and update teh current value
			$('#onlineStatus' + droneDTO.id).html('ONLINE');
			$('#armedStatus' + droneDTO.id).html(droneDTO.state);

			var drone = DRONES_MAP.get(droneDTO.id);
			drone.setPosition(droneDTO.lattitude, droneDTO.longitude, droneDTO.alt);

			$('#infoAlt' + droneDTO.id).val(droneDTO.alt);
			$('#infoSpeed' + droneDTO.id).val(droneDTO.speed);
			$('#infoBat' + droneDTO.id).val(droneDTO.battery);
		}

		else {
            // if it doenst exist yet we create a new drone object
			var drone = new Drone(droneDTO.id, droneDTO.lattitude, droneDTO.longitude);
			drone.speed = droneDTO.speed;
			drone.altitude = droneDTO.alt;

            // add the new drone to the drone list/map
			DRONES_MAP.set(droneDTO.id, drone);

            // this will add the new row to the drone list on the uI
			$('.dronesList').append( renderDroneUIComponent(droneDTO));

            // jquery for the accordion effect on the list in the UI
			$(".dronesList").on("click", ".dronesList-header", function () {

				if ($(this).hasClass("active")) {
					return;
				}

                // all active items will be closed and removed the class
				$(".dronesList > .active").each(function (index) {
					$(this).removeClass("active").next().slideToggle();

					var drone = DRONES_MAP.get( $(this).attr('droneId'));
                    // stop the vieo since were closing the view
                    // and remove the points from the map
					drone.stopVideoFeed();
					drone.hidePoints();
				});
				
                // use the id of the drone/row that was clicked on and
                // grab it from the map
				SELECTED_DRONE = DRONES_MAP.get( $(this).attr('droneId'));
				SELECTED_DRONE.showPoints();
				SELECTED_DRONE.startVideoFeed();
				
				WORLD_MAP.setZoom(18);
								
				activateViewFPV(droneDTO.id);

				$(this).toggleClass("active").next().slideToggle();
			});

            // we initialize the buttons so that they link to the selected drone
            // the control buttons on the UI
			initializeDronesControls(drone.id);
		}
	});
}