import $ from 'jquery';
import admin from './main';
/**
* This singleton assumes the management of the data polling
* 
* Job:
*  - Loop on request to load Json
*  - Convert Json to entities objects ( Clients, Iohs, Agents )
*  - Call-backs any registered rendering manager when data has been received, and pass data and delta for update optimizations.
*  - Send a 'pollingStatusChange' event on starting or stopping polling activity
* @namespace
*/
var datamanager = function (options) {
	var URL_METERS = '/meters';

	if (process.env.NODE_ENV !== 'production') {
		URL_METERS = 'http://localhost:8080/meters';
		console.info("/meters is now proxied to", URL_METERS);

	}

	//
	// PRIVATE
	//
	// Create some defaults, extending them with any options that are provided
	var _settings = $.extend({
		"debug": false,
		"dynamic": true, // false means returns hard coded metering data.
		"reqRetryDelay": 2000 // 1 seconds between two http requests
	}, options);

	if (_settings.debug)
		console.info("Debug log active");

	var _stopped = true;
	var _retryTimer = true;
	var _pollingEventName = "pollingStatusChange";
	var _listeners = {};

	// Constants for delta attributes
	var ADDED_ATTR = "added";
	var UPDATED_ATTR = "updated";
	var DELETED_ATTR = "deleted";

	// Hold the last received data
	var _lastData = false;

	// PUBLIC
	return {
		// Variable
		getPollingEventName: function () { return _pollingEventName; },

		// METHODS
		addListener: _addListener,
		removeListener: _removeListener,

		startPolling: _startPolling,
		stopPolling: _stopPolling
	};

	// PRIVATE METHODS	
	function _startPolling() {

		// (Re)Start to fetch data
		_stopFetchActivity();
		// Sends an event to inform the start of any data request
		$('body').trigger({ type: _pollingEventName, activity: true });
		_stopped = false;
		_lastData = false;

		_fetchData();

	}

	function _stopPolling() {
		_stopFetchActivity();
		// Sends an event to inform the end of any data request
		$('body').trigger({ type: _pollingEventName, activity: false });
	}

	function _fetchData() {
		var requestManager = admin.getRequestManager();
		requestManager.getData({ url: URL_METERS }, !_settings.dynamic).done(function (data) {

			if (_settings.debug) console.log("Data received", data);
			// Convert data to pertinent entities
			_processingData(data);

			if (!_stopped) {
				// Loop on next request with delay
				_retryTimer = setTimeout(
					function () {
						_fetchData();
					},
					_settings.reqRetryDelay);
			}
		}).fail(function () {
			// By default, an error dialog has been displayed
			// Do not fetch again.
			// Sends an event to inform the end of any data request
			$('body').trigger({ type: _pollingEventName, activity: false });
		});
	}

	function _stopFetchActivity() {
		_stopped = true;
		if (_retryTimer)
			clearTimeout(_retryTimer);
		_retryTimer = false;
	}


	function _addListener(id, listener) {
		_listeners[id] = listener;
		if (_settings.debug) console.log("Listener [" + id + "] ADDED");
	}

	function _removeListener(id) {
		delete _listeners[id];
		if (_settings.debug) console.log("Listener [" + id + "] REMOVED");
	}


	/**
	 * Calls all registered listeners with the processed data and the 'delta'.
	 * delta is an object that express data in terms of differences between the previous data and the new one.
	 * 
	 * @param data the full meters as Json
	 */
	function _processingData(data) {
		// Here, we have the opportunity to do some processing
		// such as convert data to objects such as entities for example.
		// Today, no special processing is done		
		var processedData = data;

		// Compute the delta
		var delta = buildDelta(_lastData, processedData);

		// Save the last data for next comparison
		_lastData = data;

		// Notify listeners that data has changed
		var lostlisteners = [];
		$.each(_listeners, function (id, listener) {
			if (!_notifyListener(listener, id, processedData, delta)) {
				// Pb with listener, Should be removed later
				lostlisteners.push(id);
			}
		}
		);
		// Remove automatically dead listener 
		$.each(lostlisteners, function (index, id) {
			try {
				_removeListener(id);
			}
			catch (e) { }
		}
		);
	}

	function _notifyListener(listener, id, data, delta) {
		if (!listener) {
			console.log("_notifyListener NO listener => return!");
			return;
		}
		try {
//			console.log("_notifyListener listener",listener);
			listener.fn.call(listener.context, data, delta);
		}
		catch (e) {
			console.error("Can't execute callback for listener " + id + " => This listener will be removed automatically!", e);
			return false;
		}
		return true;
	}


	/*********************
	 *  DELTA MANAGEMENT
	 ********************/

	/**
	 * Builds a 'delta' representation which is deduced from a previous data and a new one.
	 * The delta object has the following attribute :
	 * delta.added : Where value is a list of key / value of new elements
	 * delta.updated : Where value is a list of key / value of elements whose metering has changed
	 * delta.deleted : Where value is an array of keys of deleted elements
	 * 
	 * @param previousData The previous data
	 * @param data The last data
	 */
	function buildDelta(previousData, data) {
		if (_settings.debug) console.log("buildDelta Compare", previousData, data);
		// First of all, check the data consistency
		if (!data || data.metering === undefined) {
			// invalid data => No delta available !
			console.error("Inconsistent data detected, no metering attribute found => no delta! ", data);
			return false;
		}
		var delta = { flat: data.flat }; // Forwards information about flat mode
		// Any previous data to compare with the new one?
		if (previousData === false) {
			// Occurs when the data manager is starting its activity. To be coherent, place all data as added
			// in the delta object.
			delta[ADDED_ATTR] = data.metering;
			return delta;
		}
		// Checks globally if there are some changes. We do this by checking the global hash value.
		var previousMeters = previousData.metering;
		var lastMeters = data.metering;
		if (previousMeters.hash === undefined || lastMeters.hash === undefined) {
			console.warn("No global hash found => DEGRADED PERFORMANCE! hash=true should be used in requests.");
		} else {
			if (previousMeters.hash === lastMeters.hash) {
				if (_settings.debug) console.log("No change on global hash value", previousMeters.hash, lastMeters.hash);
				// No change
				return delta;
			}
		}

		// What elements has(ve) been added, identical or removed ?
		// For that, builds arrays of keys, and then applies on
		// fabulous algorithms with Jquery to extract added, identical(*) and deleted keys.
		// *) About same key identifier. Later, 'Updated' elements will be determined
		// in according to their hash value.
		var previousKeys = Object.keys(previousMeters);
		var newKeys = Object.keys(lastMeters);

		var added = $(newKeys).not(previousKeys).get();
		var indentical = $(previousKeys).filter(newKeys).get();
		var removed = $(previousKeys).not(newKeys).get();

		if (_settings.debug) console.log("added,indentical,removed", added, indentical, removed);

		// Treats new elements
		if (added.length > 0) {
			var addedObj = {};
			$.each(added, function (i, key) {
				addedObj[key] = lastMeters[key];
			});

			delta[ADDED_ATTR] = addedObj;
		}

		// Treats updated elements
		if (indentical.length > 0) {
			// We have to verify the hash value for each element before
			// considers it as an element to be updated
			var updatedObj = {};
			var nbOfUpdated = 0;
			$.each(indentical, function (i, key) {
				// Skips the global "hash" attribute which is not a topology element
				if (key === "hash")
					return true;

				// Compares the hash values for the same element. First of all, gets their hash to be compared
				var forceUpdate = false; // To force the update
				var lastHash = lastMeters[key].hash;
				var prevHash = previousMeters[key].hash;
				if (lastHash === undefined || prevHash === undefined) {
					// This case can occurred for agents or clients when the 'flat=true' request parameter
					// has been omitted, or for all when the hash=true request parameters is not used
					// 1- Try to get the hash value in deep for agents or client
					try {
						var last = lastMeters[key];
						var prev = previousMeters[key];
						lastHash = last[Object.keys(last)[0]].hash;
						prevHash = prev[Object.keys(prev)[0]].hash;
						if (lastHash !== undefined && prevHash !== undefined) {
							console.warn("Hash in deep " + key + " => LOWER PERFORMANCE! flat=true should be used in requests.");
						}
					} catch (e) {
						console.trace(e);
					}
					if (lastHash === undefined || prevHash === undefined) {
						console.warn("Hash not found for " + key + " => DEGRADED PERFORMANCE! hash=true should be used in requests.");
						// Turns on the flag to force the refreshing of the element later.
						forceUpdate = true;
					}

				}

				// Takes the decision to consider the element as 'updated'				
				if (forceUpdate || lastHash !== prevHash) {
					// Meters seems different for this element => update
					updatedObj[key] = lastMeters[key];
					nbOfUpdated += 1;
				}
			});

			if (nbOfUpdated > 0)
				delta[UPDATED_ATTR] = updatedObj;
		}

		// Treats removed elements 
		// Note: Just pass their id is not sufficient because when the flat mode is not used, the identifier
		// does not contains the IOH identifier which is useful to delete only edges concerned by this ioh.
		if (removed.length > 0) {
			var deletedObj = {};
			$.each(removed, function (i, key) {
				deletedObj[key] = previousMeters[key];
			});

			delta[DELETED_ATTR] = deletedObj;
		}

		if (_settings.debug) console.log("delta", delta);

		return delta;
	}

};

export default datamanager;
