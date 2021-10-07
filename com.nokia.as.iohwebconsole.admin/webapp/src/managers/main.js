import $ from "jquery";
import Utils from './utils'
import datamanager from './datamanager';
import requestmanager from './requestmanager';
import manager2d from './2dmanager';


/**
 * This singleton has in charge of dom and managers initialization of this web tool
 * 
 */
var admin = function() {
	//
	// PRIVATE
	//
	// Create some default settings
	var _settings = {
		debug : {
			all : false, // true to active debug log in all sources
			m : false, // true to active debug logs in menu manager
			d : false, // true to active debug logs in data manager
			r : false, // true to active debug logs in request manager
			d2 : false // true to active debug logs in 2D manager
		},
		dynamic : {
			d : true, // false to use static data ( no ajax request )
			d2 : true // false to use hard coded nodes and edges
		},
		autoconfig : false, // True simulate user configuration for 2D rendering
		indicatorevent : "datareceived" // Event name sent by the request manager on data reception.
	};
	
	// The container to show fetching activity
	var _indicator = false; 
	var _indicatorTimeout = false;

	// Manager
	var _dataManager = false;
	var _requestManager = false;
	var _2DManager  = false;

	// Start / Stop activity callback methods
	var _startActivity = false;
	var _stopActivity = false


	// PUBLIC
	return {
		// Variable
		getDataManager : function() { return _dataManager; },
		getRequestManager : function() { return _requestManager; },
		get2DManager : function() { return _2DManager; },

		// METHODS
		/**
		 * Uses {@link admin.init()}.
		 * @function
		 */
		init : _init,	
	};

	// PRIVATE METHODS

	function _init(startActivity,stopActivity, setErrorMessage){
		console.log("_init")

		_startActivity = startActivity;
		_stopActivity = stopActivity;
		
		// First of all, configure settings from parameters of url home page.
		_configureSettings();
		
		// Instantiates sub-managers
		_dataManager = datamanager({debug:( _settings.debug.all || _settings.debug.d ), dynamic : _settings.dynamic.d });
		_requestManager = requestmanager({debug:( _settings.debug.all || _settings.debug.r ), indicatorevent : _settings.indicatorevent, setErrorMessage : setErrorMessage });

		_2DManager = manager2d({debug:( _settings.debug.all || _settings.debug.d2 ),
								dynamic : _settings.dynamic.d2 ,
								autoconfig : _settings.autoconfig });


		// Binds to the event sent by the dataManager
		var eventName = _dataManager.getPollingEventName();
		// to update the start/stop button
		$('body').on(eventName, function(event) { _toggle2DButton(event);} );
		

		
		// Initializes quick dom accessor
		// create the indicator container
		_indicator = $('<div>')
						.attr('class','activityIndicator' )
						.attr('style', 'visibility: hidden')
						.prependTo($('#myTb-stateless-title'));

		// Adds an event handler to update the indicator
		$('body').on(_settings.indicatorevent, _showIndicator );
		
		
		// Initialization of rendering managers
		_2DManager.init();
		
		if( _settings.debug.all ) console.log("init done");
	}

	/**
	 * Updates the 2D button in according to the 2D manager activity
	 * @param event
	 */
	function _toggle2DButton(event) {
		if( _settings.debug ) console.log("_toggle2DButton",event.activity);
		if( event.activity) {
			_startActivity();			
		} else {
			_stopActivity();
		}		
	}
	
	/**
	 * Overrides default settings from parameters of url home page.
	 */
	function _configureSettings() {
		// 1 - Extracts general setting from the page url
		// About debug parameter, its value should take the following choice :
		// To set debug on all sources, use 'debug=all' or give a list of source separate
		// by a comma.Value are:
		// m for menu manager,
		// d for data manager
		// r for request manager
		// 2d for 2D manager.
		//
		// Example : debug=m,d,r,2d with  
		var debugList = Utils.getUrlVar("debug");
		if( debugList ) {
			var debugTab = debugList.split(",");
			$.each(debugTab,function(){
				_settings.debug[this] = true;				
			});
		}
		if( _settings.debug.all )
			console.info("Debug log active");
		
		// 2 - Asks to use static data
		// Value are :
		// d for data manager
		// 2d for 2D manager.
		// Example : debug=d,2d
		var staticList = Utils.getUrlVar("static");
		if( staticList ) {
			var staticTab = staticList.split(",");
			$.each(staticTab,function(){
				_settings.dynamic[this] = false;				
			});
		}
		
		// 3 - Active the auto simulation of user configuration change for 2D rendering
		// Value are : 
		var autoconfig = Utils.getUrlVar("autoconfig");
		if( autoconfig === "true" )
			_settings.autoconfig = true;
	}
	
	/**
	 * Shows the indicator for a short time  to indicate the reception of data after fetching meters
	 * @param event
	 */

	function _showIndicator(event) {
		if( _indicatorTimeout ) {
			clearTimeout(_indicatorTimeout);
			_indicatorTimeout = false;
		}
		_indicator.css('visibility','visible');
		_indicatorTimeout= setTimeout(function() { _indicator.css('visibility','hidden');}, 150);		
	}

}();

export default admin;