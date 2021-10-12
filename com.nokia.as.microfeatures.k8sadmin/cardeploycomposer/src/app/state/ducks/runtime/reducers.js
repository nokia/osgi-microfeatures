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

const InitialRequirements = {
    fnames: ["config.advanced"], // Mandatory features names ( read-only )
    featuresIds: [],
    found: false
}
const requirements = createReducer( InitialRequirements )( {
    [ types.SET_DEPLOY_REQUIREMENTS ] : ( state, action ) => {
        return {...state, ...action.payload }
    },
    [ types.RESET_DEPLOY_REQUIREMENTS ] : ( state, action ) => {
        return {...InitialRequirements }
    }
});

const InitialComplete = {
    identity: false,
    replicat: true,
    ports: true,
    podLabels : true,
    overrides: true,
    envs : true,
    files : true,
    configs: true,
    secrets : true,
    prometheus : true   
}

const InitialDeploy = {
    state : null,
    name : '',
    namespace : 'namespace',
    replicas : 1,
    portslist : [],
    podLabelsList : [],
    overridesList: [],
    envsList : [],
    filesList: [],
    configMap: [],
    secretsList : [],
    prometheusList : [],
    completes : InitialComplete
}
const form = createReducer( InitialDeploy )( {
    [ types.DEPLOY_RUNTIME ] : ( state, action ) => {
        return {...state, state: 'started' }
    },
    [ types.DEPLOYRUNTIME[msgTypes.SUCCESS] ] : ( state, action ) => {
        return {...state, state: 'ok' }
    },
    [ types.DEPLOYRUNTIME[msgTypes.FAILURE] ] : ( state, action ) => {
        return {...state, state: 'error' }
    },
    [ types.FULL_RESET ] : ( state, action ) => {
        return { ...InitialDeploy }
    },
    [ types.RESET_DEPLOY ] : ( state, action ) => {
        return { ...InitialDeploy }
    },
    [ types.PRESET_DEPLOY ] : ( state, action ) => {
        const { completes, ...rest } = action.form;
        if( completes === undefined )
            return { ...state, ...action.form };
        const newCompletes = { ...state.completes, ...completes };
        return { ...state, ...rest, completes : newCompletes };       
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
    [ types.RESET_DEPLOY ] : ( state, action ) => {
        return { ...initialWizardError }
    },
});

const runtimeFeatureId = createReducer( null )( {
    [ types.SET_SELECTED_RUNTIMEFEATURE ] : ( state, action ) => action.id,
    [ types.SET_SELECTED_FEATURES ] : ( state, action ) => (action.runtimeFeatureId !== null)?action.runtimeFeatureId:state,
    [ types.FULL_RESET ] : () => null
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
    [ types.FULL_RESET ] : () => []
});

const selectedQuick = createReducer( 'NONE' )( {
    [ types.SET_SELECTED_QUICK ] : ( state, action ) => action.id,
    [ types.FULL_RESET ] : () => 'NONE'
} );


const initialUndeploy = { payload: {}, confirm:false, undeploying : null}
const undeploy = createReducer( initialUndeploy )( {
    [ types.UNDEPLOY_RUNTIME ] : ( state, action ) => {
        return {...state, payload : action.payload, confirm: true }
    },
    [ types.UNDEPLOYRUNTIME[msgTypes.SUCCESS] ] : ( state, action ) => {
        return Object.assign({},initialUndeploy)
    },
    [ types.UNDEPLOYRUNTIME[msgTypes.FAILURE] ] : ( state, action ) => {
        return Object.assign({},initialUndeploy)
    },
    [ types.CONFIRM_UNDEPLOY_RUNTIME ] : ( state, action ) => {
        return {...state, confirm : false, undeploying:'started' }
    },
    [ types.CANCEL_UNDEPLOY_RUNTIME ] : ( state, action ) => {
        return { payload: {}, confirm:false}
    } 
} );

const details = createReducer(null) ({
    [ types.OPEN_DETAILS ] : ( state, action ) => action.id,
    [types.CLOSE_DETAILS ]: (state, action) => null
})


export default combineReducers( {
    requirements,
    form,
    formErrors,
    featuresIds,
    runtimeFeatureId,
    selectedQuick,
    undeploy,
    details
} );