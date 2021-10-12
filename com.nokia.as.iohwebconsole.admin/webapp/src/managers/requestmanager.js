/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import $ from 'jquery'
import Models from './msgmodel'

/**
* This singleton assumes the communication with
* the meters servlet in order to get the
* topology against IOH(s)
* 
* See servlet implementation source :
* http://cebu.nextenso.alcatel.fr/viewvc/branches/FRPP2800/
* com/alcatel/as/service/metering2/impl/MeteringServlet.java?revision=210&root=MeteringService2&view=markup
* 
*/

var requestmanager = function (options) {
	//
	// PRIVATE
	//
	// Create some default, extending them with any options that are provided
	var _settings = $.extend({
		"debug": false,
		"indicatorevent": "indicator", // Event name to trigg when data is received.
		"popupOnError": true, // true to display an error dialog to the user in case of request failure
		"url": "/meters",
		"mon": false,
		"meter": false,
		"mons": Models.getModel().query.mons,
		"meters": Models.getModel().query.meters,
		"hash": "true",
		"flat": "true"
	}, options);

	if (_settings.debug)
		console.info("Debug log active",_settings);

	var _deferredObject = false;

	// PUBLIC
	return {
		// Variable

		// METHODS
		/**
		 * Retrieves data as metering from the ioh console.
		 * @function getData
		 * @memberof requestmanager
		 */
		getData: _getData
	};

	// PRIVATE METHODS
	/**
	 * Build the url with its arguments
	 * @param args Json to overrides default values
	 * @returns Object with the used options and the built url with its arguments
	 */
	function buildUrl(params) {

		if (!params || !$.isPlainObject(params))
			params = {};

		// Merges client arguments with default ones
		var resolvedParams = $.extend({}, _settings, params);

		var url = resolvedParams.url + "?cache=" + new Date().getTime();

		// Disengage extra attributes which are not parameters
		resolvedParams.indicatorevent = false;
		resolvedParams.popupOnError = false;
		resolvedParams.debug = false;
		resolvedParams.url = false;
		resolvedParams.setErrorMessage = false;

		for (var key in resolvedParams) {
			if (resolvedParams[key] !== false)
				url += "&" + key + "=" + resolvedParams[key];
		}
		var result = { options: resolvedParams, url: url };
		if (_settings.debug) console.log("built url", result);
		return result;
	}

	/**
	 * Launch an HTTP request to obtains data OR a static data.
	 * By default, an error popup is displayed on http failure.
	 * @param args An empty json or with any attribute to override default values
	 * @param test true means just return a static hard-coded value ( no HTTP request )
	 * @returns a deferred object
	 * @private
	 */
	function _getData(args, test) {

		_deferredObject = $.Deferred();
		if (test === true) {
			// Returns an hard-coded data for a test goal.
			setTimeout(
				function () {
					_deferredObject.resolve(_test());
				}, 10
			);
			return _deferredObject;
		}

		// Build the request with its arguments
		var urlObj = buildUrl(args);
		$.ajaxSetup({
			timeout: 20000 //Time in milliseconds
		});
		$.getJSON(urlObj.url, function (data) {
			// Success, returns the data , appends some contextual information such as
			// the use of hash and flat parameters.
			data.hash = (urlObj.options.hash === "true");
			data.flat = (urlObj.options.flat === "true");

			// Send an event to display request activity
			$('body').trigger({ type: _settings.indicatorevent });

			_deferredObject.resolve(data);
		})
			.fail(function (jqXHR, textStatus) {
				console.warn("getData failed", urlObj.url, jqXHR.status, textStatus);
				// Display an error dialog
				if (_settings.popupOnError) {
					_displayErrorDialog(urlObj.url, jqXHR, textStatus);
				}

				var errorReason = { "code": jqXHR.status, "textStatus": textStatus };
				_deferredObject.reject(errorReason);
			});

		return _deferredObject;

	}

	function _displayErrorDialog(url, jqXHR, textStatus) {
		var details = ("timeout" === textStatus) ? "Error Timeout." : "";
		details += "Status code:" + jqXHR.status;
		$('.messageContent > p').text(details);
		_settings.setErrorMessage("Server Error","Unable to load meters for server.",details);
	}

	function _test() {
		console.info("DATA MANAGER RETURNS HARD CODED METERING DATA!");
		return {
			"hash": true,
			"flat": true,
			"metering": {
				// IOH
				"as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 100,
					"description": "as.ioh.engine.diameter",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 100,
					"hash": -1498756531
				},

				// AGENTS
				"agent.remote.group__instance.1:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 10,
					"description": "agent.remote.group__instance.1:as.ioh.engine.diameter",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 5,
					"hash": -1498763486
				},

				"agent.remote.group__instance.2:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},

				"agent.remote.group__instance.3:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group__instance.4:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group__instance.5:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group__instance.6:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group2__instance.7:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group__instance.8:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group2__instance.9:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group__instance.10:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 10,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.2:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},


				// CLIENT
				"R.tcp.127.0.0.1.46828:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 10,
					"description": "client1.realm.com@realm.com",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 9,
					"hash": -1498763494
				},

				"R.tcp.127.0.0.1.46829:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 40,
					"description": "client1.realm.com@realm.com",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 40,
					"hash": -1498763494
				},
				"R.tcp.127.0.0.1.46830:as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 50,
					"description": "client1.realm.com@realm.com",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 50,
					"hash": -1498763494
				},



				// IOH 2
				"as.ioh.engine.diam2@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 1000,
					"description": "as.ioh.engine.diameter",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 1000,
					"hash": -1498763492
				},

				// AGENTS
				"agent.remote.group__instance.11:as.ioh.engine.diam2@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 100,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.1:as.ioh.engine.diameter",
					"read.msg.resp.App": 10,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				"agent.remote.group__instance.12:as.ioh.engine.diam2@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 800,
					"read.msg.req.App": 0,
					"description": "agent.remote.group__instance.1:as.ioh.engine.diameter",
					"read.msg.resp.App": 800,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},
				// CLIENT
				"R.tcp.127.0.0.2.46829:as.ioh.engine.diam2@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 1000,
					"description": "client1.realm.com@realm.com",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 910,
					"hash": -1498763494
				},

				// PART WHERE AGENTS ARE INITIATOR OF THE REQUEST

				// IOH 3
				"as.ioh.engine.diam3@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 10,
					"description": "as.ioh.engine.diameter",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 10,
					"hash": -1498763492
				},
				// AGENTS
				"agent.remote.group__instance.11:as.ioh.engine.diam3@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 87878,
					"description": "agent.remote.group__instance.1:as.ioh.engine.diameter",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 6660,
					"hash": -1498763494
				},
				"agent.remote.group__instance.14:as.ioh.engine.diam3@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 0,
					"read.msg.req.App": 9,
					"description": "agent.remote.group__instance.1:as.ioh.engine.diameter",
					"read.msg.resp.App": 0,
					"write.msg.resp.App": 9,
					"hash": -1498763494
				},
				// CLIENT
				"R.tcp.127.0.0.2.46829:as.ioh.engine.diam3@remote.csf.group__component.instance__MetersIOH": {
					"write.msg.req.App": 9,
					"read.msg.req.App": 0,
					"description": "client1.realm.com@realm.com",
					"read.msg.resp.App": 9,
					"write.msg.resp.App": 0,
					"hash": -1498763494
				},

				"hash": -283284746
			}
		};

	}
};


export default requestmanager;
