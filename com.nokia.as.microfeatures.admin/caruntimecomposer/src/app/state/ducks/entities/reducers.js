/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { combineReducers } from "redux";
import { utils } from "../../../utilities"
import { OBRS, FEATURES, ASSEMBLIES, SNAPSHOTS, SELECT_OBR } from "./types";
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

// Specific reducers for entities

const { msgTypes } = utils;
const { SUCCESS } = msgTypes

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
    }
});

const assemblies = createReducer( {} )( {
    [ ASSEMBLIES[SUCCESS] ] : ( state, action ) => {
        return merge({}, action.response.entities.assemblies);
    }
});

const snapshots = createReducer( {} )( {
    [ SNAPSHOTS[SUCCESS] ] : ( state, action ) => {
        return merge({}, action.response.entities.snapshots);
    }
});

export default combineReducers( {
    obrs,
    localobr,
    features,
    assemblies,
    snapshots,
    selectedObr
} );


