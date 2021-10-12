/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { combineReducers } from "redux";
import { utils } from "../../../utilities"
import {
    OBRS,
    FEATURES,
    RUNTIMES,
    SELECT_OBR,
    POLL_START_RUNTIMES,
    POLL_STOP_RUNTIMES,
    ROUTES,
    POLL_START_ROUTES,
    POLL_STOP_ROUTES,
    FUNCTIONS,
    POLL_START_FUNCTIONS,
    POLL_STOP_FUNCTIONS
} from "./types";
import { createReducer } from "../../utils";
import { merge } from 'lodash'
import Cookies from 'universal-cookie';
const cookies = new Cookies();

// Basic reducers
const selectedObr = createReducer( null )( {
    [ SELECT_OBR ] : ( state, action ) => {
        cookies.set('preferedObr', action.url);
        return action.url
    }
});
const pollingRuntimes = createReducer( false )( {
    [ POLL_START_RUNTIMES ] : () => true,
    [ POLL_STOP_RUNTIMES ] : () => false
});

const pollingRoutes = createReducer( false )( {
    [ POLL_START_ROUTES ] : () => true,
    [ POLL_STOP_ROUTES ] : () => false
});

const pollingFunctions = createReducer( false )( {
    [ POLL_START_FUNCTIONS ] : () => true,
    [ POLL_STOP_FUNCTIONS ] : () => false
});

// Specific reducers for entities

const { msgTypes } = utils;
const { SUCCESS, FAILURE } = msgTypes

const obrs = createReducer( [] )( {
    [ OBRS[SUCCESS] ] : ( state, action ) => {
//        console.log(OBRS[SUCCESS] + " 1)action.response.entities",action.response.entities)
        return merge([], action.response.entities.obrs);
    }
});
const localobr = createReducer( null )( {
    [ OBRS[SUCCESS] ] : ( state, action ) => {
//        console.log(OBRS[SUCCESS] + " 2)action.response.entities",action.response.entities)
        return action.response.entities.localobr;
    }
});

const features = createReducer( {} )( {
    [ FEATURES[SUCCESS] ] : ( state, action ) => {
        return merge({}, action.response.entities.features);
    },
    [ FEATURES[FAILURE] ] : ( state, action ) => {
        return {};
    }
});


const runtimes = createReducer( {} )( {
    [ RUNTIMES[SUCCESS] ] : ( state, action ) => {
        return merge({}, action.response.entities.runtimes);
    },
    [ RUNTIMES[FAILURE] ] : ( state, action ) => {
        return {};
    }
});

const routes = createReducer( {} )( {
    [ ROUTES[SUCCESS] ] : ( state, action ) => {
        return merge({}, action.response.entities.routes);
    },
    [ ROUTES[FAILURE] ] : ( state, action ) => {
        return {};
    }
});

const functions = createReducer( {} )( {
    [ FUNCTIONS[SUCCESS] ] : ( state, action ) => {
        return merge({}, action.response.entities.functions);
    },
    [ FUNCTIONS[FAILURE] ] : ( state, action ) => {
        return {};
    }
});


export default combineReducers( {
    obrs,
    localobr,
    features,
    runtimes,
    selectedObr,
    pollingRuntimes,
    routes,
    pollingRoutes,
    functions,
    pollingFunctions
} );


