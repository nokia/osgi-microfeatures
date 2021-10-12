/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/**
 * Message Model
 * This model defines message names and their semantics
 */
var Models = function() {
	// PRIVATE
	// Defines model in according to the protocol
	var _diameterModel = {
			// Protocol id / label ( to be used later to display protocol selector )
			id : "diameter",
			name : "Diameter",
			// Used to query meters.
			query : {
				"mons" : "diameter",
				"meters" : "App",
			},
			elemType : {
				IOH : "ioh",
				AGENT : "agent",
				CLIENT : "client"
			},
			// An initiator of request determines the flow direction
			initiators : {
				client : 'clientinitiator', // Request direction is Client -> IOH -> Agent
				agent : 'agentinitiator'	// Request direction is Agent -> IOH -> Client
			},
			clientinitiator : {
				ioh : {
					request : "read.msg.req.App",
					response : "write.msg.resp.App",
					from : "Clients",
					to: "Engine"
				},
				client : {
					request : "read.msg.req.App",
					response : "write.msg.resp.App",
					from : "Client",
					to: "Engine"
				},
				agent : {
					request : "write.msg.req.App",
					response : "read.msg.resp.App",
					from : "Engine",
					to: "Agent"
				}

			},
			agentinitiator : {
				ioh : {
					request : "write.msg.req.App",
					response : "read.msg.resp.App",
					from : "Engine",
					to : "Clients"
				},
				client : {
					request : "write.msg.req.App",
					response : "read.msg.resp.App",
					from : "Engine",
					to : "Client"
				},
				agent : {
					request : "read.msg.req.App",
					response : "write.msg.resp.App",
					from : "Agent",
					to : "Engine"
				}
			}
	};
	
	// Hold the current model
	var _currentModel = _diameterModel;
	var _models = [_diameterModel];
	
	// Public methods
	return {
		getModel : function() { return _currentModel; },
		getModels : function() { return _models; }
	};
}();

export default Models;