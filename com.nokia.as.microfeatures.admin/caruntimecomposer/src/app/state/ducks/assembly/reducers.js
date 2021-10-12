/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { combineReducers } from "redux";
import * as types from "./types";
import { createReducer } from "../../utils";
import { utils } from "../../../utilities";
import { merge, xor } from 'lodash'

const { msgTypes } = utils;

// Assembly structure
const InitialAssembly = {
    name: "",
    version: "",
    desc: "",
    doc:"",
    open : false,
    state : null
}

// reducers
const form = createReducer( InitialAssembly )( {
    [ types.CREATE_ASSEMBLY ] : ( state, action ) => {
        return {...state, ...action.payload, state: 'started', open : false }
    },
    [ types.ASSEMBLY[msgTypes.SUCCESS] ] : ( state, action ) => {
        return {...state, state: 'ok' }
    },
    [ types.ASSEMBLY[msgTypes.FAILURE] ] : ( state, action ) => {
        return {...state, state: 'error' }
    },
    [ types.RESET_ASSEMBLY ] : ( state, action ) => {
        return { ...InitialAssembly }
    },
    [ types.PRESET_ASSEMBLY ] : ( state, action ) => {
        return { ...state, ...action.form }
    },
    [ types.OPENCLOSE_ASMBPANEL ] : ( state, action ) => {
        return {...state, open : action.open }
    }   
} );

const runtimeId = createReducer( null )( {
    [ types.SET_SELECTED_RUNTIME ] : ( state, action ) => action.id,
    [ types.SET_SELECTED_FEATURES ] : ( state, action ) => (action.runtimeId !== null)?action.runtimeId:state
} );

const featuresIds = createReducer( [] )( {
    [ types.TOGGLE_FEATURE ] : ( state, action ) => {
        const newState = merge([],state.map(x=>x));
        if( newState === state)
            console.log("featuresIds newState == state");
        else
        console.log("featuresIds newState != state");

        return xor( merge([], newState), [action.id] )
    },
    [ types.SET_SELECTED_FEATURES ] : ( state, action ) => merge([], action.featuresIds),
    [ types.RESET_ASSEMBLY ] : ( state, action ) => {
        return []
    }
} );

const selectedQuick = createReducer( 'NONE' )( {
    [ types.SET_SELECTED_QUICK ] : ( state, action ) => action.id
} );

const initialDelete = { payload: {}, confirm:false, remove : null}
const deletion = createReducer( initialDelete )( {
    [ types.DELETE_ASSEMBLY ] : ( state, action ) => {
        return {...state, payload : action.payload, confirm: true }
    },
    [ types.DELASSEMBLY[msgTypes.SUCCESS] ] : ( state, action ) => {
        return Object.assign({},initialDelete)
    },
    [ types.DELASSEMBLY[msgTypes.FAILURE] ] : ( state, action ) => {
        return Object.assign({},initialDelete)
    },
    [ types.CONFIRM_DELETE_ASSEMBLY ] : ( state, action ) => {
        return {...state, confirm : false, remove:'started' }
    },
    [ types.CANCEL_DELETE_ASSEMBLY ] : ( state, action ) => {
        return { payload: {}, confirm:false}
    } 
} );

export default combineReducers( {
    form,
    featuresIds,
    runtimeId,
    selectedQuick,
    deletion
} );