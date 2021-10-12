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

const { msgTypes } = utils;



const InitialComplete = {
    mainConfig : false,
    params : true,
    functionparams: true,
    runtimes : true,
    execution : true
}

const InitialRoute = {
    open : false,
    state : null,
    name : '',
    type: '', // free and 'http', 'kafka'
    path: '',
    functionId: '',
    paramsList: [], // optional
    functionParamsList: [], // optional
    runtimesList: [], // optional ( max = 1 today )
    ttl: '0',
    completes: InitialComplete
}
const form = createReducer( InitialRoute )( {
    [ types.ADD_ROUTE ] : ( state, action ) => {
        return {...state, state: 'started', open : false }
    },
    [ types.ADDROUTE[msgTypes.SUCCESS] ] : ( state, action ) => {
        return {...state, state: 'ok' }
    },
    [ types.ADDROUTE[msgTypes.FAILURE] ] : ( state, action ) => {
        return {...state, state: 'error' }
    },
    [ types.FULL_RESET ] : ( state, action ) => {
        return { ...InitialRoute }
    },
    [ types.RESET_ROUTE ] : ( state, action ) => {
        return { ...InitialRoute }
    },
    [ types.PRESET_ROUTE ] : ( state, action ) => {
        const { completes, ...rest } = action.form;
        if( completes === undefined )
            return { ...state, ...action.form };
        const newCompletes = { ...state.completes, ...completes };
        return { ...state, ...rest, completes : newCompletes };       
    },
    [ types.OPENCLOSE_ROUTEPANEL ] : ( state, action ) => {
        return {...state, open : action.open }
    }  
} );

const initialWizardError = { };
const formErrors = createReducer( initialWizardError )( {
    [types.SET_FORM_ERR] : ( state, action ) => {
        return { ...state, ...action.errors }
    },
    [ types.FULL_RESET ] : ( state, action ) => {
        return { ...initialWizardError }
    },
    [ types.RESET_ROUTE ] : ( state, action ) => {
        return { ...initialWizardError }
    },
});


const selectedQuick = createReducer( 'NONE' )( {
    [ types.SET_SELECTED_QUICKROUTE ] : ( state, action ) => action.id,
    [ types.FULL_RESET ] : () => 'NONE'
} );


const initialDelete = { payload: {}, confirm:false, deleting : null}
const remove = createReducer( initialDelete )( {
    [ types.DELETE_ROUTE ] : ( state, action ) => {
        return {...state, payload : action.payload, confirm: true }
    },
    [ types.DELETEROUTE[msgTypes.SUCCESS] ] : ( state, action ) => {
        return Object.assign({},initialDelete)
    },
    [ types.DELETEROUTE[msgTypes.FAILURE] ] : ( state, action ) => {
        return Object.assign({},initialDelete)
    },
    [ types.CONFIRM_DELETE_ROUTE ] : ( state, action ) => {
        return {...state, confirm : false, deleting:'started' }
    },
    [ types.CANCEL_DELETE_ROUTE ] : ( state, action ) => {
        return { payload: {}, confirm:false}
    } 
} );


export default combineReducers( {
    form,
    formErrors,
    selectedQuick,
    remove
} );