import $ from 'jquery';
import admin from './main';
import Models from './msgmodel';
import { DataSet, Network } from 'vis/index-network.js'
/**
* This singleton assumes the management of the 2D rendering
* 
* Job: 
*  - Initialize dom with canvas 2D
*  - Add Listener to the dataManager to be called when data has been processed
*  - Graphs processed data
* 
*/
var manager2d = function (options) {
	//
	// PRIVATE
	//
	// Create some default, extending them with any options that are provided
	var _settings = $.extend({
		debug: false,
		dynamic: true, // false means 2D manager uses hard coded nodes & edges.
		autoconfig: false // true to simulate configuration change
	}, options);

	if (_settings.debug)
		console.info("Debug log active");

	// Few quick dom accessor
	var _right2dContainer = false;
	var _networkContainer = false;
	var _domInitialized = false;

	// VIS objects
	var _visNetwork = false; // The vis object to display our network with nodes and edges.
	var _nodes = false; // Nodes as a DataSet object
	var _edges = false; // Edges as a DataSet object

	// Constants
	var IOH = "ioh";
	var CLIENT = "client";
	var AGENT = "agent";
	var REGEPS_IOH = /((\w+)(\.(.+)\.(.+))\.(\w+))@((\w+)\.(.*))$/;
	var REGEPS_AGENT = /^(agent\w*)\.(remote|local)\.((\w+)__(\w+\.\d+))/;
	var REGEPS_CLIENT = /^(([R|I])\.((.+)\.(.*))):(.*)@(.*)$/;

	var DEFAULT_INFO = "Double click on a node to see its meters.";

	var _userConfig = false; // The user configuration
	var _infoContainer = { container: false }; // The container to display node/edge information
	var _infoSelectedIds = false; // Json that contains identifiers of selected nodes/edges to be shown in the info container


	// Configuration of the resolution mode to display 'percentage'
	// To calculate the percentage, two possibilities : 
	// -a) Use of global IOH meters which should correspond to the total of received / sent messages ( i.e 100% ), or
	// -b) Total all received / sent messages between clients / agents to determine the 100% ( NOT IMPLEMENTED YET ! )
	var percentageFromIOHmeters = true; // true means a) , false b).

	// Network options templates
	// To get all available attributes see http://visjs.org/docs/network/
	var _options = {
		standard: {
			physics: {
				forceAtlas2Based: {
					gravitationalConstant: -26,
					centralGravity: 0.005,
					springLength: 230,
					springConstant: 0.18
				},
				maxVelocity: 50,
				solver: 'forceAtlas2Based',
				timestep: 0.35,
				stabilization: { iterations: 150 }
			},
			layout: {
				randomSeed: 40,
			},
			/*
								edges: {
								},
			*/
			nodes: {
				mass: 1
// PUT IN COMMENT physics here See https://github.com/almende/vis/issues/3562 ( from vis 4.21 )
//				physics: true
			}

		},
		standard_ori: {
			physics: {
				forceAtlas2Based: {
					gravitationalConstant: -26,
					centralGravity: 0.005,
					springLength: 230,
					springConstant: 0.18
				},
				maxVelocity: 50,
				solver: 'forceAtlas2Based',
				timestep: 0.35,
				stabilization: { iterations: 150 }
			},
			layout: {
				randomSeed: 4,
			},
			edges: {
			},
			nodes: {
				mass: 1,
				physics: true
			}
		},
		hierarchical: {
			physics: {
				forceAtlas2Based: {
					gravitationalConstant: -26,
					centralGravity: 0.005,
					springLength: 30,
					springConstant: 0.18
				},
				maxVelocity: 146,
				solver: 'forceAtlas2Based',
				timestep: 0.35,
				stabilization: { iterations: 150 }
			},
			layout: {
				//						randomSeed:2,
				hierarchical: {
					levelSeparation: 800,
					nodeSpacing: 10,
					edgeMinimization: false,
					//							sortMethod: "directed",
					direction: "LR" // UD, DU , LR , RL
				}
			},
			edges: {
				// When smooth is used, multi edges between two nodes are not displayed, only one!!!
				smooth: {
					type: 'cubicBezier',
					forceDirection: 'horizontal', // 'vertical' 'horizontal'
					roundness: 0.4 // 0.4
				},
				color: { color: '#000000' }
			},
			nodes: {
				mass: 1
// PUT IN COMMENT physics here See https://github.com/almende/vis/issues/3562 ( from vis 4.21 )
//				physics: true
			}
		}
	};


	// PUBLIC
	return {
		// Variable

		// METHODS
		init: _init,
		changeConfig: _onChangeConfig,
		open: _open,
		close: _close,
		startDisplay: _startDisplay,
		display: _display
	};

	// PRIVATE METHODS
	function _init(parent) {
	}

	function _startDisplay(parent, infoContainer) {
		var self = this;
		_right2dContainer = $(parent);
		_infoContainer.container = $(infoContainer);
		_right2dContainer.empty();
		_infoContainer.container.text(DEFAULT_INFO);

		// Add listener to be call be back on data change.
		admin.getDataManager().addListener(
			'2Dmanager',
			{
				context: self,
				fn: self.display,
			}
		);

		_domInitialized = true;
	}



	function _onChangeConfig(config) {
		// Called when the user configuration has changed.
		_userConfig = $.extend(true, {}, config);
		/*		
				// When the 2D manager is not displayed, no action
				if( _2dContainer.css('display') === 'none')
					return;
		*/
		// Do some cleaning
		if (_visNetwork) {
			_visNetwork.destroy();
			_visNetwork = false;
		}
		if (_right2dContainer)
			_right2dContainer.empty();
		_networkContainer = false;

		//			if( _settings.debug)
		console.info("new configuration!", _userConfig);
	}

	function _open() {
		// Ask to start activity
		admin.getDataManager().startPolling();
	}

	function _close() {
		if (!_domInitialized) return;

		// Debug: Deactivation of automatic configuration change
		/*
				if( _settings.autoconfig )
					_configurator2d.stopAutoConfig();
		*/
		// Removes listener
		admin.getDataManager().removeListener('2Dmanager');

		// Ask to stop activity
		admin.getDataManager().stopPolling();

		// Do some cleaning
		if (_visNetwork) {
			_visNetwork.destroy();
			_visNetwork = false;
		}
		_right2dContainer.empty();
		_networkContainer = false;

		_infoSelectedIds = false;
		// Cleans the info container
		//		_infoContainer.container.empty();

		// hides the 2D container.
		//		_2dContainer.hide();

	}



	/**
	 * Callback method triggered by the data manager once it has been received some data
	 * 
	 * Treating data to display elements as an network.
	 * Here we use the vis api ( See http://visjs.org/ ) with the 'Network' and 'DataSet' modules
	 * for a 2D rendering which display our elements ( ioh, client, agent ) as nodes and their relations
	 * as edges
	 * 
	 * @param fulldata the full meters as Json
	 * @param delta Json that contains the difference between the last data and the previous one.
	 */
	function _display(fulldata, delta) {
		if (!_domInitialized) return;

		if (_settings.debug) console.log("_display", fulldata, delta);
		// First of all, we have to create the container for network rendering if needed
		if (!_networkContainer) {
			// Create the canvas.
			_networkContainer = $('<div>')
				.attr('id', 'network2d')
				.appendTo(_right2dContainer);
		}

		// Checks if the network is currently displayed. If not, the nodes and edges will be
		// produced from the full meters ( i.e data parameters ).
		// To be efficient, modes and edges will be created and stored into DataSet objects
		// and passed as parameters on the Network creation time.
		if (!_visNetwork) {
			// Reset and Create new nodes and edges to hold data as DataSet
			_nodes = new DataSet(); // new vis.DataSet();
			_edges = new DataSet(); // new vis.DataSet();

			if (!_settings.dynamic)
				// Create static hard-coded nodes and edges for debugging purpose
				_createFakeNodeAndEdgesForTest(_nodes, _edges);
			else
				_createAllNodesAndEdgesFromData(_nodes, _edges, fulldata);

			// Create the Network to display elements
			// whose global option depends on the configurator.
			var optionNetwork = {};
			if (_userConfig.view.hierarchical === true || _userConfig.view.hierarchical.orientation) {
				optionNetwork = $.extend(true, {}, _options.hierarchical);
				if (_userConfig.view.hierarchical.orientation === "vertical") {
					optionNetwork.layout.hierarchical = {
						levelSeparation: 400,
						nodeSpacing: 10,
						//							edgeMinimization : false,
						//							sortMethod: "directed",
						direction: "UD" // UD, DU , LR , RL
					};

				}

			} else {
				optionNetwork = $.extend({}, _options.standard);
			}

			if (_settings.debug) console.log("optionNetwork", optionNetwork);

			var data = { nodes: _nodes, edges: _edges };
			try {
			_visNetwork = new Network(_networkContainer.get(0), data, optionNetwork);
			} catch(e) {
				console.error("new Network failed",e);
			}

			if (_settings.debug) console.info("_visNetwork.getSeed()", _visNetwork.getSeed());

			// Add interaction handlers to be able to display some information on nodes/edges
			_addInteractionHandlersOnNetwork(_visNetwork);

			return;
		}

		// Here, the delta parameters must be checked for any update.
		// When delta contains pertinent operation such as 'added','updated' or 'deleted',
		// nodes and edges must be re-arranged and then the Network is refreshed.
		if (!delta || (!delta.added && !delta.updated && !delta.deleted)) {
			// No change from previous data, do nothing.
			return;
		}
		try {
			_updateNetWork(delta);
		} catch (e) {
			console.error("_updateNetWork(" + delta + ")", e);
			if (_settings.dynamic)
				throw e;
		}


	}

	/*************************************
	 * CREATE METHODS FOR NODES AND EDGES
	 *************************************/

	/**
	 * Parse data as json and create/add nodes and edges as DataSet objects
	 * 
	 * @param nodes The DataSet to store nodes
	 * @param edges The DataSet to store edges
	 * @param data
	 */
	function _createAllNodesAndEdgesFromData(nodes, edges, data) {
		if (_settings.debug) console.log("_createAllNodesAndEdgesFromData", data);

		// Loop on each elements to create nodes and edges
		var flatMode = data.flat;
		$.each(data.metering, function (key, value) {
			// Skip global hash if present in keys
			if ("hash" === key)
				return true;
			// IOH ?
			var iohNode = null;
			if ((!flatMode && key.indexOf("ioh") !== -1) || (flatMode && key.indexOf(":") === -1)) {
				// Check if the ioh node already exist ( created previously by an agent or a client )
				iohNode = nodes.get(key.hashCode());
				if (iohNode === null) {
					iohNode = _createNodeForIoh(nodes, key);
				}
				// Updates the ioh node with its meters
				_updateNodeMeters(nodes, iohNode, value);
				if (_settings.debug) console.log("IOH meters updated", key, value);

				// Updates the information displayed into the configurator space
				_updateConfiguratorInfo(iohNode)

				return true; // continue
			}
			// At this step, we treats either an agent or a client.
			// Converts key and value to a unique Json representation whatever the flat mode including meters
			var elemData = _getElementData(key, value, flatMode);
			// Create the ioh node if does not exist yet
			iohNode = nodes.get(elemData.iohid);
			if (iohNode === null) {
				iohNode = _createNodeForIoh(nodes, elemData.iohKey);
			}
			if (elemData.elemKey.startsWith("agent")) {
				// By default elemData.id which is used as node identifier is built from the fullKey.
				// Because a agent node share several linked IOHs, its node identifier shall not depends on these IOHs but
				// only from its agent key.
				elemData.id = elemData.elemKey.hashCode();
				_createAgentNodeAndEdges(nodes, edges, elemData);
			} else {
				_createClientNodeAndEdges(nodes, edges, elemData);
			}
		});
	}

	/**
	 * Create a node for any IOH without meters
	 * 
	 * Standard key value : as.ioh.engine.diameter@remote.csf.group__component.instance__MetersIOH
	 * Where: 
	 * 	- diameter is the name of the ioh engine ( as.ioh.engine is a fixed string )
	 * 	- right part after '@' corresponds to the load balancer instance which contains one or more engine whose:
	 * 		- csh/group/component/instance__MetersIOH is equivalent to a p/g/c/i definition.
	 * 
	 * Note: 
	 *  - The engine name is used as node label.
	 *  - The p/g/c/i is used as a node group in order to distinguish two nodes whose engines use the same name.
	 *  
	 * Important : node group is dynamic, so vis decide for the color of the shape.
	 * 
	 * 
	 * @param nodes DataSet to store nodes
	 * @param key Identifier of the ioh
	 * @returns the node
	 */
	function _createNodeForIoh(nodes, key) {

		var node = {
			id: key.hashCode(),
			level: 1,
			label: "?",
			group: "none",
			shape: 'circularImage',
			image: 'images/ioh.svg',
			title: key,
			shadow: {
				enabled: true
			},
			size: 40,
			type: IOH
		};

		// Parses the key to complete the node information needed for rendering,
		// and a 'descriptions' object which will be used to display information about this node.
		var descriptions = [];

		// Reset global regexp		
		REGEPS_IOH.lastIndex = 0;
		var object = REGEPS_IOH;
		var match = object.exec(key);

		if (match && match.length > 9) {
			// Label
			node.label = match[6];

			// remote / local for pgci
			var s = match[8];
			node.remote = ("remote" === s) ? true : false;

			// Group ( used to colorize )
			node.group = match[9];

			// Descriptions
			descriptions.push({ "IOH": match[6] });
			descriptions.push({ "Engine": match[1] });
			descriptions.push({ "@": match[9] });
		}
		node.descriptions = descriptions;

		// Add the new node to the DataSet
		if (_settings.debug) console.log("ioh node CREATED", node);
		nodes.add(node);

		return node;
	}



	/**
	 * Create / update a agent node and its edges against the ioh node and add them to the
	 * DataSets.
	 * Remark: The client node holds meters of all IOHs on which it is connected.
	 * 
	 * Standard key : agent.remote.group__instance.1
	 * Where :
	 * 	- agent : fixed key word
	 *	- remote : remote agent else local is used
	 *	- group : group name of the agent
	 *	- instance.1 : Instance name.
	 *
	 * Note: 
	 * 	- The instance name is used as node label.
	 * 	- The group name is used as node group.
	 * 
	 * Important : node group is dynamic, so vis decide for the color of the shape.
	 * 
	 * @param nodes DataSet to store nodes
	 * @param edges DataSet to store edges
	 * @param elemData The Json element that corresponds to the agent to create or update.
	 */
	function _createAgentNodeAndEdges(nodes, edges, elemData) {
		var node = nodes.get(elemData.id);
		if (node === null) {
			// Create the node
			node = {
				id: elemData.id,
				level: 2,
				label: "?",
				group: AGENT,
				shape: 'box',
				type: AGENT,
				meters: {},
				title: elemData.elemKey,
				shadow: {
					enabled: true
				}
			};

			// Add meters for the linked IOH
			node.meters[elemData.iohKey] = elemData.meters;

			// Parses the key to complete the node information needed for rendering,
			// and a 'descriptions' object which will be used to display information about this node.
			var descriptions = [];

			// Reset global regexp
			REGEPS_AGENT.lastIndex = 0;
			var object = REGEPS_AGENT;
			var match = object.exec(elemData.elemKey);

			if (match && match.length > 5) {
				// Label
				node.label = match[5];

				// Group ( used to colorize )
				node.group = match[4];

				// remote / local
				var s = match[2];
				node.remote = ("remote" === s) ? true : false;

				descriptions.push({ Agent: match[5] });
				descriptions.push({ "Full name": match[0] });
				descriptions.push({ "Deployed in": match[4] });
			}
			node.descriptions = descriptions;


			// Add the new node to the DataSet
			if (_settings.debug)
				console.log("agent node CREATED", node);
			nodes.add(node);

			// When a new agent node is created, we have to create a fake meter called 'topology' to obtain
			// at least an edge even no edge are created for current meters.
			elemData.meters["topology"] = 1;

		} else {
			// Existing node => update meters linked with the ioh
			node.meters[elemData.iohKey] = elemData.meters;
			nodes.update(node);
		}

		// Build additional meters from current meters to show lost message
		if (_userConfig.flux === "lost") {
			_addLostMeters(elemData.meters);
		}
		// Build additional meters from current meters to show repartition in percent
		else if (percentageFromIOHmeters && _userConfig.flux === "percentage") {
			_addPercentMeters(nodes, true, elemData);
		}

		// Updates the information displayed into the configurator space
		_updateConfiguratorInfo(node)

		// Create/update edges between agent and ioh nodes
		// The identifier of each edges will be the hash value from a string which respect the following format :
		//  the full key + the meter name
		var edgeList = [];
		var edge;
		$.each(elemData.meters, function (meter, value) {
			if (meter === "hash" || meter === "description")
				return true;

			// Do not use edge when value is less than 1 ( excepting in cases of lost or percentage messages )
			if (_userConfig.flux !== "lost" && _userConfig.flux !== "percentage" && value < 1)
				return true;

			// In according to the user configuration, the edge could be not eligible to be created and the
			// arrow rendering can differs too.
			var capability = _getEdgeCapability(meter, value);
			if (!capability.draw) {
				return true;
			}

			// Creates an edge for a meter ( or update ) in according to the required flux			
			if (_settings.debug)
				console.log("meter,value", meter, value);
			var edgeId = (elemData.fullKey + meter).hashCode();
			edge = edges.get(edgeId);
			if (edge === null) {
				// Create the edge
				edge = {
					id: edgeId,
					// 'from' shall be always IOH to be compliant with capability arrows definition.
					from: elemData.iohid,
					to: node.id,
					label: "" + value,
					title: meter,
					arrowStrikethrough: false, // False to see multi edges between two nodes
					font: capability.font,
					// length : 15,
					color: "#000000"
				};

				//A 'topology' meter is independent of configuration
				if (meter === "topology") {
					delete edge.label;
					delete edge.title;
					delete edge.font;
					edge.dashes = true;
					edge.hidden = true;
				} else {

					// Add % to label when we display percentage
					if (_userConfig.flux === "percentage")
						edge.label = value + "%";

					// Apply edge arrows rendering
					if (capability.arrows)
						edge.arrows = capability.arrows;

					// When flux is 'percentage' or 'lost, the edge thick depends on the value.
					if (_userConfig.flux === "percentage" || _userConfig.flux === "lost")
						edge.value = value;
				}

				edgeList.push(edge);
				if (_settings.debug)
					console.log("Edge [" + edge.id + "] for agent CREATED", edge);
			} else {
				// Update if needed the edge attributes
				if (_updateEdge(edge, capability, meter, value)) {
					edgeList.push(edge);
					if (_settings.debug)
						console.log("Edge [" + edge.id + "] for agent UPDATED", edge);
				}
			}

		});

		// Adds/updates all edges in one shoot
		if (edgeList.length > 0) {
			edges.update(edgeList);
		}

		// The topology edge should be at least displayed when no any pertinent edge exist
		_updateTopologyEdge(edges, elemData);
	}


	/**
	 * Create / update a client node and its edges against the ioh node and add them to the
	 * DataSets.
	 * 
	 * Standard key : R.tcp.127.0.0.1.46828
	 * Where :
	 * 	- R or I : R for remote, I for local
	 *	- client name is the rest of the string
	 *
	 * Note: 
	 * 	- The R / I is used as the node label
	 * 	- The group node has been fixed as "client".
	 *
	 * 
	 * @param nodes DataSet to store nodes
	 * @param edges DataSet to store edges
	 * @param elemData The Json element that corresponds to the new client.
	 */
	function _createClientNodeAndEdges(nodes, edges, elemData) {
		var node = nodes.get(elemData.id);
		if (node === null) {
			node = {
				id: elemData.id,
				level: 0,
				label: "?",
				group: CLIENT,

				shape: 'circularImage',
				image: 'images/ic_happy_face.svg',
				type: CLIENT,
				remote: "R/L?",
				title: elemData.elemKey,
				shadow: {
					enabled: true
				},
				meters: elemData.meters
			};

			// Parses the key to complete the node information needed for rendering,
			// and a 'descriptions' object which will be used to display information about this node.
			var descriptions = [];

			// Reset global regexp
			REGEPS_CLIENT.lastIndex = 0;
			var object = REGEPS_CLIENT;
			var match = object.exec(elemData.fullKey);

			if (match && match.length > 7) {
				// remote / local
				var s = match[2];
				node.remote = ("R" === s) ? true : false;

				// Label
				node.label = match[5];

				descriptions.push({ Client: match[5] });
				descriptions.push({ "Full name": match[1] });
				descriptions.push({ "Remote/Local": match[2] });
				descriptions.push({ "Connected to": [{ Engine: match[6] }, { "@": match[7] }] });
			}
			node.descriptions = descriptions;

			// Add the new node to the DataSet
			if (_settings.debug) console.log("client node CREATED", node);
			nodes.add(node);

			// When a new agent node is created, we have to create a fake meter called 'topology' to obtain
			// at least an edge even no edge are created for current meters.
			elemData.meters["topology"] = 1;

		} else {
			// Existing node => update meters linked with the ioh
			_updateNodeMeters(nodes, node, elemData.meters);
			nodes.update(node);
		}

		// Build additional meter from current meters to show lost message
		if (_userConfig.flux === "lost") {
			_addLostMeters(elemData.meters);
		}
		// Build additional meters from current meters to show repartition in percent
		else if (percentageFromIOHmeters && _userConfig.flux === "percentage") {
			_addPercentMeters(nodes, false, elemData);
		}

		// Updates the information displayed into the configurator space
		_updateConfiguratorInfo(node)

		// Create edges between client and ioh nodes
		// The identifier of each edges will be the hash value from a string which respect the following format :
		//  the full key + the meter name
		var edgeList = [];
		var edge;

		$.each(node.meters, function (meter, value) {
			if (meter === "hash" || meter === "description")
				return true;

			// Do not use edge when value is less than 1 ( excepting in cases of lost or percentage messages )
			if (_userConfig.flux !== "lost" && _userConfig.flux !== "percentage" && value < 1)
				return true;

			// In according to the user configuration, the edge could be not eligible to be created and the
			// arrow rendering can differs too.
			var capability = _getEdgeCapability(meter, value);
			if (!capability.draw) {
				return true;
			}

			// Creates an edge for a meter ( or update ) in according to the required flux			
			if (_settings.debug)
				console.log("meter,value", meter, value);
			var edgeId = (elemData.fullKey + meter).hashCode();
			edge = edges.get(edgeId);
			if (edge === null) {
				// Create the edge
				edge = {
					id: edgeId,
					// 'from' shall be always IOH to be compliant with capability arrows definition. 
					from: elemData.iohid,
					to: node.id,
					label: ""+ value,
					title: meter,
					arrowStrikethrough: false, // False to see multi edges between two nodes
					font: capability.font,
					color: "#000000"
				};

				//A 'topology' meter is independent of configuration
				if (meter === "topology") {
					delete edge.label;
					delete edge.title;
					delete edge.font;
					edge.dashes = true;
					edge.hidden = true;
				} else {
					// Add % to label when we display percentage
					if (_userConfig.flux === "percentage")
						edge.label = value + "%";

					// Apply edge arrows rendering
					if (capability.arrows)
						edge.arrows = capability.arrows;

					// When flux is 'percentage' or 'lost, the edge thick depends on the value.
					if (_userConfig.flux === "percentage" || _userConfig.flux === "lost")
						edge.value = value;
				}
				edgeList.push(edge);
				if (_settings.debug)
					console.log("Edge [" + edge.id + "] for client CREATED", edge);
			} else {
				// Update if needed the edge attributes
				if (_updateEdge(edge, capability, meter, value)) {
					edgeList.push(edge);
					if (_settings.debug)
						console.log("Edge [" + edge.id + "] for client UPDATED", edge);
				}
			}

		});

		// Adds/updates all edges in one shoot
		if (edgeList.length > 0) {
			edges.update(edgeList);
		}

		// The topology edge should be at least displayed when no any pertinent edge exist
		_updateTopologyEdge(edges, elemData);

	}

	/**
	 * Returns the capability of an edge to be drawn and the arrow rendering type
	 * The returned object has the following attributes:
	 * - draw : true means the edge must be created/displayed else false,
	 * - arrows : the same definition of the edges.arrows defined by the visJs api.
	 * - font : The css font applicable to this edge
	 * 
	 * To build the arrows object, we assume that the 'from' and 'to' always refer
	 * the IOH to the Agent/Client
	 * 
	 * @param meter The meter name
	 * @param value The meter value
	 * @returns {Object}
	 */
	function _getEdgeCapability(meter, value) {
		// Parse the meter name to get pertinent semantic
		var meterSemantics = {
			request: false,
			response: false,
			read: false,
			write: false,
			topology: false,
			percentage: false,
			lost: false
		};
		var tokens = meter.split('.');
		$.each(tokens, function () {
			var token = this.trim();
			if (token === "topology") {
				meterSemantics.topology = true;
				return true;
			}
			if (token === "percentage") {
				meterSemantics.percentage = true;
				return true;
			}
			if (token === "lost") {
				meterSemantics.lost = true;
				return true;
			}
			if (token === "req") {
				meterSemantics.request = true;
				return true;
			}
			if (token === "resp") {
				meterSemantics.response = true;
				return true;
			}
			if (token === "read") {
				meterSemantics.read = true;
				return true;
			}
			if (token === "write") {
				meterSemantics.write = true;
				return true;
			}
		});

		// A 'topology' meter is independent of configuration
		if (meterSemantics.topology)
			return { draw: true };

		// Sets the draw value in according to the chosen flux
		var flux = _userConfig.flux;
		//detailed,request,response,percentage,lost
		if ((flux === "percentage" && !meterSemantics.percentage) ||
			(flux === "lost" && !meterSemantics.lost) ||
			(flux === "detailed" && (!meterSemantics.request && !meterSemantics.response)) ||
			(flux === "request" && !meterSemantics.request) ||
			(flux === "response" && !meterSemantics.response)
		) {
			return { draw: false };
		}

		var capability = { draw: true, font: { align: 'horizontal' } };
		// Builds the arrows in according the flux type
		// Percentage
		if (meterSemantics.percentage && flux === "percentage") {
			capability.arrows = (meterSemantics.write) ? 'to' : 'from';
			return capability;
		}

		// Lost
		if (meterSemantics.lost && flux === "lost") {
			// The direction of the arrow depends of the message initiator 
			var currentMsgModel = Models.getModel();
			var iohModelKey = currentMsgModel.elemType.IOH;
			var model = currentMsgModel.clientinitiator[iohModelKey];
			var meterName = 'lost.' + model.request;

			// Assumes there is only two lost message type. Not more.
			capability.arrows = (meter === meterName) ? 'from' : 'to';
			if (value !== 0)
				capability.font.color = "#FF0000"; //"red";
			else
				capability.font.color = "#000000";

			return capability;
		}

		var viewtype = _userConfig.view.hierarchical;

		if (meterSemantics.request && (flux === "request" || flux === "detailed")) {
			if (meterSemantics.read)
				capability.arrows = 'from';
			else if (meterSemantics.write)
				capability.arrows = 'to';

			// Changes the font for the horizontal view for a best label rendering
			if (viewtype !== false && ( viewtype === true || viewtype.orientation === "horizontal"))
				capability.font = { align: 'top' };
		}
		if (meterSemantics.response && (flux === "response" || flux === "detailed")) {
			if (meterSemantics.read)
				capability.arrows = 'from';
			else if (meterSemantics.write)
				capability.arrows = 'to';

			// Changes the font for the horizontal view for a best label rendering
			if (viewtype !== false && (viewtype === true || viewtype.orientation === "horizontal"))
				capability.font = { align: 'bottom' };
		}

		return capability;

	}

	/**
	 * Appends to meters for a client /agent a 'lost' meters to count the number of lost messages. 
	 * A 'lost' meter corresponds to the difference between the number of requests and
	 * the number of response in according to the message initiator.
	 * 
	 * Note1: No lost meter is provided when the number of request/response is equal to zero.
	 * Node2: A lost meter name when generated has the following format:
	 * meter name : 'lost' + request ( in according to the message initiator )
	 *
	 * @param meters the object that contains original meters where lost meters will be added.
	 */
	function _addLostMeters(meters) {
		// 1- Treat the case when the client is the message initiator
		var currentMsgModel = Models.getModel();
		var iohModelKey = currentMsgModel.elemType.IOH;
		var model = currentMsgModel.clientinitiator[iohModelKey];

		var meterName = 'lost.' + model.request;

		var n1 = Number(meters[model.request]);
		var n2 = Number(meters[model.response]);

		var lost = n1 - n2;

		if (n1 !== 0 && n2 !== 0 && lost >= 0) {
			meters[meterName] = lost;
			if (_settings.debug) {
				if (lost > 0)
					console.warn(meterName, lost);
			}

		} else {

			// 2- Treat the case when the agent is the message initiator
			model = currentMsgModel.agentinitiator[iohModelKey];

			meterName = 'lost.' + model.request;

			n1 = Number(meters[model.request]);
			n2 = Number(meters[model.response]);

			lost = n1 - n2;

			if (n1 !== 0 && n2 !== 0 && lost >= 0) {
				meters[meterName] = lost;
				if (_settings.debug) {
					if (lost > 0)
						console.warn(meterName, lost);
				}

			}
		}
	}



	/**
	 * Adds to the client/agent meters a new meter which represents the repartition of message in percent.
	 * IMPORTANT: This method is ONLY used when the percentage is calculated from the global IOH meters
	 * ( i.e percentageFromIOHmeters = true )
	 * 
	 * From Client point of view, the repartition is calculate between all clients links to the same IOH.
	 * From Agent point of view, the repartition shows how the load balancing is made by the IOH against
	 * connected agents.
	 * 
	 * Remark: When the initiator of request messages is agent, the repartition has no sense.
	 * 
	 * For an IOH, its meters correspond to the whole message received by Clients (to be sent towards agents) and their opposite.
	 *
	 * @param nodes
	 * @param isAgent true for agent, false for client
	 * @param elemData
	 * 
	 */
	function _addPercentMeters(nodes, isAgent, elemData) {
		var repartition = 0; // in %
		var meter = 0; // Nb of requests sent by the element
		// Gets the meter name for requests in according to the element type ( client / agent )
		var currentMsgModel = Models.getModel();
		var modelKey = (isAgent) ? currentMsgModel.elemType.AGENT : currentMsgModel.elemType.CLIENT;
		var model = currentMsgModel.clientinitiator[modelKey];
		var meterName = model.request;

		// Get the meters of the IOH linked to the agent
		var iohNode = nodes.get(elemData.iohid);
		if (iohNode === null || iohNode.meters === undefined) {
			console.warn("Unable to calculate percentage on message. No ioh meters for " + elemData.iohKey);
			elemData.meters["percentage." + meterName] = repartition;
			return;
		}
		// Looking for the total number of requests received by the IOH
		model = currentMsgModel.clientinitiator[currentMsgModel.elemType.IOH];
		var iohTotal = Number(iohNode.meters[model.request]);
		if (iohTotal === 0) {
			// Before sending a warning, check if the message initiator is not the agent.
			var model2 = currentMsgModel.agentinitiator[currentMsgModel.elemType.IOH];
			var iohTotal2 = Number(iohNode.meters[model2.request]);
			if (iohTotal2 !== 0) {
				console.info("No percentage is displayed when agents are message initiators!");
			} else {
				console.warn("Unable to calculate percentage on message. ioh " + model.request + "=0");
				elemData.meters["percentage." + meterName] = repartition;
			}
			return;
		}

		if (_settings.debug) console.log("iohTotal=" + iohTotal, elemData.meters);



		meter = Number(elemData.meters[meterName]);
		if (meter !== 0) {
			repartition = ((meter * 100) / iohTotal);
			// Limitation to 100% !
			repartition = Math.min(100, repartition);
			repartition = repartition.toFixed(2).toString();
			var idx = repartition.indexOf(".00");
			if (idx !== -1) {
				repartition = repartition.substring(0, idx);
			}
		}

		elemData.meters["percentage." + meterName] = repartition;

	}

	/*************************************
	 * UPDATE METHODS FOR NODES AND EDGES
	 *************************************/

	/**
	 * Update the network by updating DataSet(s) from delta 
	 */
	function _updateNetWork(delta) {
		var flatMode = delta.flat;

		// Any node to delete?
		if (delta.deleted) {
			if (_settings.debug) console.log("updateNetWork TO BE DELETED", delta.deleted);
			$.each(delta.deleted, function (key, value) {
				var idToBeDeleted = false;
				// IOH ?
				if ((!flatMode && key.indexOf("ioh") !== -1) || (flatMode && key.indexOf(":") === -1)) {
					idToBeDeleted = key.hashCode();
				} else {
					// Agent / Client ?
					var elemData = _getElementData(key, value, delta.flat);
					if (elemData.elemKey.startsWith("agent")) {
						// An agent node hold one or more IOH Meters. Here, we have to delete only meters and
						// edges in according to the concerned IOH. Finally, only when all edges has been deleted,
						// the agent node shall be deleted.
						// By default elemData.id which is used as node identifier is built from the fullKey.
						// Because a agent node share several linked IOHs, its node identifier shall not depends on these IOHs but
						// only from its agent key.
						elemData.id = elemData.elemKey.hashCode();
						_removeIohConnectionOnAgentNode(_nodes, _edges, elemData);
					} else {
						// Client
						idToBeDeleted = elemData.id;
					}

				}

				// For IOH or Client, the node must be removed and the associated edges
				if (idToBeDeleted) {
					try {
						// Deleted all edges connected to the element to be removed
						var edgesToDelete = _edges.get({
							filter: function (item) {
								return (item.from === idToBeDeleted || item.to === idToBeDeleted);
							}
						});
						_edges.remove(edgesToDelete);

						// Delete the node
						if (_settings.debug) console.log("Delete node Id=" + idToBeDeleted);
						_nodes.remove(idToBeDeleted);

						// Clean the configurator info space if necessary
						_removeConfiguratorInfo(idToBeDeleted);

					} catch (e) {
						console.error("Unable to delete node&egdes for " + idToBeDeleted, e);
					}
				}
			});
		}

		// Any node to add ?
		if (delta.added) {
			var elementsTobeAdded = delta.added;

			// Should parse elements and do some _nodes.add calls
			if (_settings.debug) console.log("updateNetWork TO BE ADDED", elementsTobeAdded);
			// Create a metering attribute to be compliant with the existing method in charge of creating elements
			delta.metering = delta.added;
			_createAllNodesAndEdgesFromData(_nodes, _edges, delta);

		}

		if (delta.updated) {
			if (_settings.debug) console.log("updateNetWork TO BE UPDATED", delta.updated);
			// Create a metering attribute to be compliant with the existing method in charge of updating elements
			delta.metering = delta.updated;
			_createAllNodesAndEdgesFromData(_nodes, _edges, delta);

		}
	}

	/**
	 * Removes meters corresponding to the connection between an agent and a IOH.
	 * Because the agent node can hold several connections with iohs, remove meters
	 * consists on:
	 *  1- Remove meters associated to the ioh
	 *  2- Remove all existing edges between the agent and the ioh
	 *  3- If there is no more connection with ioh, remove the agent node.
	 *  
	 * @param _nodes
	 * @param _edges
	 * @param elemData 
	 */
	function _removeIohConnectionOnAgentNode(_nodes, _edges, elemData) {
		// Get the agent node from its id
		var node = _nodes.get(elemData.id);
		if (node === null) {
			console.warn("Can't find the agent node for id=" + elemData.elemKey);
			return;
		}
		// 1- Remove meters node associated to the ioh
		delete node.meters[elemData.iohKey];

		// 2- Remove all existing edges between the agent and the ioh
		try {
			var edgesToDelete = _edges.get({
				filter: function (item) {
					return ((item.from === elemData.id && item.to === elemData.iohid) ||
						(item.to === elemData.id && item.from === elemData.iohid));
				}
			});
			_edges.remove(edgesToDelete);

		} catch (e) {
			console.error("Unable to delete egdes between [" + elemData.elemKey + "]<=>[" + elemData.iohKey + "]", e);
		}

		// 3- If there is no more connection with ioh, remove the agent node.
		if ($.isEmptyObject(node.meters)) {
			if (_settings.debug) console.log("Delete agent node Id=" + elemData.id, node);
			_nodes.remove(elemData.id);

			// Clean the configurator info space if necessary
			_removeConfiguratorInfo(elemData.id);
		}
	}

	/**
	 * Update meters for an IOH
	 * @param nodes
	 * @param edges
	 * @param key
	 * @param meters
	 *
	function _updateIohNodeMeters(nodes, edges, key, meters) {
		var node = nodes.get(key.hashCode());
		_updateNodeMeters(nodes, node, meters);
		if (_settings.debug) console.log("IOH meters updated", key, meters);
	}
	*/


	/**
	 * Update edge attributes from a new meter value
	 * @param edge The edge to be updated
	 * @param capability The edge capability
	 * @param meter The meter name
	 * @param value the new value
	 * @returns true means the edges attributes have been updated
	 */
	function _updateEdge(edge, capability, meter, value) {
		// Replace old edge attributes by new ones if a change has been detected
		var hasChanged = false;
		var newLabel = "" + value;
		if (_userConfig.flux === "percentage")
			newLabel += "%";

		if (edge.label !== newLabel) {
			edge.label = newLabel;
			hasChanged = true;
		}

		if (_userConfig.flux === "lost") {
			// Font color could having changed and/or arrows rendering
			edge.font = capability.font;
			edge.arrows = capability.arrows;
			hasChanged = true;
		}

		// When flux is 'percentage' or 'lost, the edge thick depends on the value.
		if (_userConfig.flux === "percentage" || _userConfig.flux === "lost") {
			if (edge.value !== value) {
				edge.value = value;
				hasChanged = true;
			}

		}

		if (_settings.debug) {
			var message = ((hasChanged) ? "" : "not ") + " updated";
			console.log("Edge [" + edge.id + "]" + message, meter, value);
		}

		return hasChanged;
	}


	/**
	 * Hides or shows the 'topology' edge after a creation or an update of client/agent node with its meters.
	 * 
	 * @param edges The current DataSet of edges
	 * @param elemData The client/agent data
	 */
	function _updateTopologyEdge(edges, elemData) {
		var topologyEdgeId = (elemData.fullKey + "topology").hashCode();
		// Gets the list of pertinent existing edges without the topology edge
		var pertinentEdges = edges.get({
			filter: function (item) {
				return ((item.id !== topologyEdgeId && (
					(item.from === elemData.id && item.to === elemData.iohid) ||
					(item.to === elemData.id && item.from === elemData.iohid)
				)));
			}
		});

		// Shows the topology edge when no pertinent edge exist else hides it.
		var hidden = (pertinentEdges.length > 0) ? true : false;
		var topologyEdge = edges.get(topologyEdgeId);
		if (topologyEdge !== null && topologyEdge.hidden !== hidden) {
			topologyEdge.hidden = hidden;
			edges.update(topologyEdge);
			if (_settings.debug)
				console.log("Topology edge[" + topologyEdgeId + "] update with hidden=" + hidden, elemData.fullKey);
		}

	}

	/**
	 * Update the node with its meters
	 * @param nodes The DataSet where node must be updated
	 * @param node The node
	 * @param meters the meters
	 */
	function _updateNodeMeters(nodes, node, meters) {
		node.meters = meters;
		nodes.update(node);
	}

	/**
	 * Parses the key of an agent/client and returns an object with the following attributes:
	 * - elemKey : The key of the element ( i.e an agent or a client )
	 * - iohKey : The ioh key
	 * - meters : The object that contains meters
	 * - fullKey : the concatenation of elemKey + ":" + iohKey
	 * - id : the hash value of the concatenation of fullKey
	 * - iohid : The hash value of the identifier of iohKey
	 * 
	 * Important: The id attribute will be replaced in external way for any agent. Its id will be
	 * calculated for its elemKey.
	 * 
	 * @param key The original key whose format is depending of the used flat mode
	 * @param value Json object
	 * @param flatMode
	 * @returns the Json object
	 */
	function _getElementData(key, value, flatMode) {
		var element = {};
		if (flatMode) {
			var split = key.split(':');
			element.elemKey = split[0];
			element.iohKey = split[1];
			element.meters = value;
			element.fullKey = key;
			element.id = element.fullKey.hashCode();
			element.iohid = element.iohKey.hashCode();
		} else {
			element.elemKey = key;
			element.iohKey = Object.keys(value)[0];
			element.meters = value[element.iohKey];
			element.fullKey = element.elemKey + ":" + element.iohKey;
			element.id = element.fullKey.hashCode();
			element.iohid = element.iohKey.hashCode();
		}
		//		console.log("_getElementData("+key+","+value+","+flatMode, element);
		return element;
	}

	/*************************
	 * INTERACTION MANAGEMENT
	 *************************/
	/**
	 * Add all handlers to react on user event on the network
	 */
	function _addInteractionHandlersOnNetwork(network) {
		network.on("doubleClick", _changeDisplayInfo);
	}

	/**
	 * Starts the display into the info container ( located in the configurator space )
	 * of the selected node and edges.
	 * This method has in charge of:
	 * -clean the info container,
	 * -build all dom elements to display node meters which will be updated when
	 * their values change.
	 *  
	 * @param params
	 */
	function _changeDisplayInfo(params) {
		if (params.nodes.length === 0 && params.edges.length === 0)
			_infoSelectedIds = false;
		else
			_infoSelectedIds = { nodes: params.nodes, edges: params.edges };

		_infoContainer.container.empty();
		if (!_infoSelectedIds) {
			_infoContainer.container.text(DEFAULT_INFO);
			return;
		}

		var $parent = $('<div>').appendTo(_infoContainer.container);

		// Build all elements to display
		var nodeid = _infoSelectedIds.nodes[0];
		if (!nodeid) {
			_infoSelectedIds = false;
			_infoContainer.container.text(DEFAULT_INFO);
			return;
		}
		var node = _nodes.get(nodeid);

		//		console.log("nodid/node",nodeid,node);

		// Node descriptions contains a list of key/value of information whatever the element ( client/ioh/agent )
		// Each description has only one key/value; Value can be a single string or an array of descriptions which
		// should be displayed with indentation.
		// As a common rule, the first one is considered as an header
		var descriptions = node.descriptions.slice(); // Need to clone this array to use shift method on next call.
		var header = descriptions.shift();
		var fieldName = Object.keys(header)[0];
		var value = header[fieldName];
		$parent.append($('<h1>').text(fieldName + ' : ' + value));

		// Displays remaining descriptions as standard rendering
		var $ul = $('<ul>').appendTo($parent);
		_displayDescriptionsField(descriptions, $ul);

		// Display meters
		_displayMetersInfo($parent, node);
	}

	/**
	 * Displays on a container a list of description objects. A description object has one key/value
	 * where value can be a single string or a list of description objects to be displayed with indentation.
	 * @param descList The array of descriptions objects
	 * @param container The container dom to append descriptions
	 */
	function _displayDescriptionsField(descList, container) {
		$.each(descList, function (i, description) {
			// Each description has only one key/value
			var fieldName = Object.keys(description)[0];
			var value = description[fieldName];
			// Create a <li> to the current field
			var $li = $('<li>').text(fieldName + " : ").appendTo(container);
			if ($.isArray(value)) {
				// When value is a list of descriptions, they should be displayed with indentation.
				var $ul = $('<ul>').appendTo($li);
				return _displayDescriptionsField(value, $ul);
			}
			// Complete the li tag with the string value
			$li.append($('<span>').text(value));
		});

	}

	/**
	 * Complete the configurator information space with meters
	 * @param container
	 * @param node
	 */
	function _displayMetersInfo(container, node) {
		var nbIOH = Object.keys(node.meters).length;
		var chapter = "Meters?"
		if (node.type === IOH)
			chapter = "Total meters against all connected clients";
		else if (node.type === CLIENT)
			chapter = "Engine meters";
		else if (node.type === AGENT) {
			var plurial = (nbIOH > 1) ? "s" : "";
			var connected = (nbIOH > 1) ? " (Connected:" + nbIOH + ")" : "";
			chapter = "IOH" + plurial + " meters" + connected;
		}
		container.append($('<h2>').append($('<span>').addClass("info2dMeters").text(chapter)));

		// Display the call flows in according to the current message model and the node type
		var currentMsgModel = Models.getModel();
		if (node.type === IOH) {
			_displayCallFlows(container, currentMsgModel.elemType.IOH, '', node.meters);
		} else if (node.type === CLIENT) {
			_displayCallFlows(container, currentMsgModel.elemType.CLIENT, '', node.meters);
		} else if (node.type === AGENT) {
			// Agent meters is referenced by their connected IOH => Display meters for each ioh
			$.each(node.meters, function (iohkey, meters) {
				var $ul = $('<ul>').appendTo(container);
				var split = iohkey.split('@');
				_displayDescriptionsField([{ Engine: split[0] }, { "@": split[1] }], $ul);

				// Gives the hash value of the connected IOH to build an unique container identifier
				// of meter value.
				_displayCallFlows(container, currentMsgModel.elemType.AGENT, iohkey.hashCode(), meters);
			});

		} else
			container.append('<pre id="info2dMeters" >' + JSON.stringify(node.meters, null, 2) + '</div>');
	}

	/**
	 * Displays meters as call flows in according to the topology element
	 * Note: Lost messages are calculated for each message initiator in local ( i.e The lost
	 * message previously added in meters is not used here).
	 * 
	 * @param container
	 * @param elemType The element type used as key against the current message model
	 * @param prefixId Used to obtain an unique identifier for container of meter values ( Required for agent meters )
	 * @param meters
	 */
	function _displayCallFlows(container, elemType, prefixId, meters) {
		var currentMsgModel = Models.getModel();

		// build a table two display two rows of call flow
		var $div = $('<div>')
			.addClass('infocallflowstable')
			.appendTo(container);

		// First main row is used to display when the client is the message initiator
		var $row = $('<div>').addClass('infocallflowtablerows').appendTo($div);
		var nbLost = _displayCallFlow($row, currentMsgModel.clientinitiator[elemType], prefixId, meters);
		$row = $('<div>').addClass('infocallflowtablerows infolostrow').appendTo($div);
		_displayCallFlowLostMsg($row, prefixId, currentMsgModel.initiators.client, nbLost);

		// Second main row is used to display when the agent is the message initiator
		$row = $('<div>').addClass('infocallflowtablerows').appendTo($div);
		nbLost = _displayCallFlow($row, currentMsgModel.agentinitiator[elemType], prefixId, meters);
		$row = $('<div>').addClass('infocallflowtablerows infolostrow').appendTo($div);
		_displayCallFlowLostMsg($row, prefixId, currentMsgModel.initiators.agent, nbLost);


	}

	/**
	 * Display a specific call flow.
	 * Note: As a convention, each meter value is displayed via a dom container whose
	 * identifier has the following format: 'info' + prefixId + meter name ( Without any dot character! )
	 * @param container
	 * @param model The model that contains meters names used to represent the call flow
	 * @param prefixId Used to obtain unique identifier for meter value ( only needed for agent meters )
	 * @param meters
	 * @return Number of lost messages
	 */
	function _displayCallFlow(container, model, prefixId, meters) {
		var req = meters[model.request];
		var resp = meters[model.response];

		var idr = 'info' + prefixId + model.request.replace(/\./g, '-');
		var idp = 'info' + prefixId + model.response.replace(/\./g, '-');

		var $spanreq = $('<span>').attr('id', idr).addClass('infometervalue').text(req);
		var $spanresp = $('<span>').attr('id', idp).addClass('infometervalue').text(resp);


		container
			.append($('<div>')
				.attr('style', 'display:table-cell;vertical-align:middle;')
				.text(model.from))
			.append($('<div>')
				.attr('style', 'display:table-cell;white-space:nowrap;')
				.append($('<div>')
					.attr('style', 'display: list-item')
					.append($('<span>').addClass("ion-md-remove"))
					.append($spanreq)
					.append($('<span>').addClass("ion-md-arrow-round-forward"))
				)
				.append($('<div>')
					.attr('style', 'display: list-item')
					.append($('<span>').addClass("ion-md-arrow-round-back"))
					.append($spanresp)
					.append($('<span>').addClass("ion-md-remove"))
				))
			.append($('<div>')
				.attr('style', 'display:table-cell;vertical-align:middle;')
				.text(model.to));

		// Compute the number of lost messages
		var nbLost = Number(req) - Number(resp);
		return (nbLost < 0) ? 0 : nbLost;
	}

	/**
	 * Display the number of the lost message for a call flow
	 * Note: The container identifier of the lost message value has the following format:
	 * 'infolost' + prefixId + initiator
	 * Remark: The row of lost will be not visible when lost message is equal to zero.
	 * @param container
	 * @param prefixId
	 * @param initiator
	 * @param lost
	 */
	function _displayCallFlowLostMsg(container, prefixId, initiator, lost) {
		var lostid = 'infolost' + prefixId + initiator;
		var $spanlost = $('<span>').attr('id', lostid).addClass('infometervalue').text(lost);

		container
			.append($('<div>')
				.attr('style', 'display:table-cell;')
				.text("Lost(s)"))
			.append($('<div>')
				.attr('style', 'display:table-cell;white-space:nowrap;')
				.append($('<div>')
					.attr('style', 'display: list-item')
					.append($('<span>').addClass("ion-md-remove"))
					.append($spanlost)
					.append($('<span>').addClass("ion-md-arrow-round-forward"))
				)
			)
			.append($('<div>').attr('style', 'display:table-cell;'));

		// When there is no lost message, just hide the row
		if (lost <= 0) {
			container.css("visibility", "hidden");
		}
	}

	/**
	 * Updates the node information space in according to the pre-selected node.
	 * Note: Nothing is done when the node does not corresponds to the pre-selected node
	 * @param node
	 */
	function _updateConfiguratorInfo(node) {
		if (!_infoSelectedIds || node.id !== _infoSelectedIds.nodes[0])
			return;
		var currentMsgModel = Models.getModel();

		if (node.type === IOH) {
			_updateCallFlows(currentMsgModel.elemType.IOH, '', node.meters);
		} else if (node.type === CLIENT) {
			_updateCallFlows(currentMsgModel.elemType.CLIENT, '', node.meters);
		} else if (node.type === AGENT) {
			// Agent meters is referenced by their connected IOH => Update meters for each ioh
			$.each(node.meters, function (iohkey, meters) {
				// Gives the hash value of the connected IOH to get an unique container identifier
				// of meter value.
				_updateCallFlows(currentMsgModel.elemType.AGENT, iohkey.hashCode(), meters);
			});

		} else {
			$("#info2dMeters").html($('<pre>' + JSON.stringify(node.meters, null, 2) + '</pre>'));
		}
	}

	/**
	 * Update call flows for an element
	 * @param elemType The element type used as key against the current message model
	 * @param prefixId Used to obtain unique identifier for meter value ( only needed for agent meters )
	 * @param meters
	 */
	function _updateCallFlows(elemType, prefixId, meters) {
		if (_settings.debug)
			console.log("_updateCallFlows", elemType, prefixId, meters)
		var currentMsgModel = Models.getModel();

		// Updates meters when client is the messages initiator
		var model = currentMsgModel.clientinitiator[elemType];

		_updateMeter('#info' + prefixId + model.request, meters[model.request]);
		_updateMeter('#info' + prefixId + model.response, meters[model.response]);

		_updateCallFlowLostMsg(prefixId, currentMsgModel.initiators.client, meters[model.request], meters[model.response]);


		// Updates meters when agent is the messages initiator
		model = currentMsgModel.agentinitiator[elemType];

		_updateMeter('#info' + prefixId + model.request, meters[model.request]);
		_updateMeter('#info' + prefixId + model.response, meters[model.response]);

		_updateCallFlowLostMsg(prefixId, currentMsgModel.initiators.agent, meters[model.request], meters[model.response]);

	}

	/**
	 * Update container that contains the old meter value
	 * @param id Container identifier
	 * @param value Meter value
	 */
	function _updateMeter(id, value) {
		var htmlCompliantID = id.replace(/\./g, '-');
		$(htmlCompliantID).text(value);
	}
	/**
	 * Updates the lost message for a call flow information.
	 * 
	 * @param prefixId Needed for agent information update
	 * @param initiator The initiator of the messages
	 * @param requests Number of requests
	 * @param responses Number of responses
	 */
	function _updateCallFlowLostMsg(prefixId, initiator, requests, responses) {
		// Gets the container of the lost message and the row which is depends on
		var lostid = '#infolost' + prefixId + initiator;
		var $spanlost = $(lostid);
		var $row = $spanlost.parents('.infocallflowtablerows').first();

		// Computes the number of lost messages
		var nbLost = Number(requests) - Number(responses);
		if (nbLost > 0) {
			// Update the lost value and show the rows
			$spanlost.text(nbLost);
			$row.css("visibility", "visible");
		} else {
			$row.css("visibility", "hidden");
		}
	}

	/**
	 * 
	 * @param nodeId
	 * @returns
	 */
	function _removeConfiguratorInfo(nodeId) {
		if (_infoSelectedIds && nodeId === _infoSelectedIds.nodes[0]) {
			_infoSelectedIds = false;
			_infoContainer.container.empty().text(DEFAULT_INFO);
		}
	}





	/***********
	 * TEST PART
	 ***********/
	function _createFakeNodeAndEdgesForTest(nodes, edges) {
		nodes.add([
			{ id: 1, label: 'Client1' },
			{ id: 2, label: 'Ioh' },
			{ id: 3, label: 'Agent1' },
			{ id: 4, label: 'Agent2' }
		]);

		edges.add([
			{ from: 1, to: 2 },
			{ from: 2, to: 1 },
			{ from: 2, to: 3 },
			{ from: 3, to: 2 },
			{ from: 2, to: 4 },
			{ from: 4, to: 2 }
		]);
		console.info("USING STATIC FAKE NODES & EDGES FOR RENDERING !");
	}

};

export default manager2d;