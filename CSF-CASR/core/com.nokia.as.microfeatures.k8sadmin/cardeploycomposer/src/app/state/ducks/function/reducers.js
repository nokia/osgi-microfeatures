import { combineReducers } from "redux";
import * as types from "./types";
import { createReducer } from "../../utils";
import { utils } from "../../../utilities";

const { msgTypes } = utils;



const InitialComplete = {
    identity : false,
    handling: true,
    locations : false,
    params : true
}

const InitialFunction = {
    state : null,
    name : '',
    lazy: true,
    timeout: '10',
    locationsList : [], // one location at least
    paramsList : [], // optional   
    completes : InitialComplete
}
const form = createReducer( InitialFunction )( {
    [ types.ADD_FUNCTION ] : ( state, action ) => {
        return {...state, state: 'started', open : false }
    },
    [ types.ADDFUNCTION[msgTypes.SUCCESS] ] : ( state, action ) => {
        return {...state, state: 'ok' }
    },
    [ types.ADDFUNCTION[msgTypes.FAILURE] ] : ( state, action ) => {
        return {...state, state: 'error' }
    },
    [ types.FULL_RESET ] : ( state, action ) => {
        return { ...InitialFunction }
    },
    [ types.RESET_FUNCTION ] : ( state, action ) => {
        return { ...InitialFunction }
    },
    [ types.PRESET_FUNCTION ] : ( state, action ) => {
        const { completes, ...rest } = action.form;
        if( completes === undefined )
            return { ...state, ...action.form };
        const newCompletes = { ...state.completes, ...completes };
        return { ...state, ...rest, completes : newCompletes };       
    },
    [ types.OPENCLOSE_FUNCTIONPANEL ] : ( state, action ) => {
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
    [ types.RESET_FUNCTION ] : ( state, action ) => {
        return { ...initialWizardError }
    },
});


const selectedQuick = createReducer( 'NONE' )( {
    [ types.SET_SELECTED_QUICKFUNCTION ] : ( state, action ) => action.id,
    [ types.FULL_RESET ] : () => 'NONE'
} );


const initialDelete = { payload: {}, confirm:false, deleting : null}
const remove = createReducer( initialDelete )( {
    [ types.DELETE_FUNCTION ] : ( state, action ) => {
        return {...state, payload : action.payload, confirm: true }
    },
    [ types.DELETEFUNCTION[msgTypes.SUCCESS] ] : ( state, action ) => {
        return Object.assign({},initialDelete)
    },
    [ types.DELETEFUNCTION[msgTypes.FAILURE] ] : ( state, action ) => {
        return Object.assign({},initialDelete)
    },
    [ types.CONFIRM_DELETE_FUNCTION ] : ( state, action ) => {
        return {...state, confirm : false, deleting:'started' }
    },
    [ types.CANCEL_DELETE_FUNCTION ] : ( state, action ) => {
        return { payload: {}, confirm:false}
    } 
} );


export default combineReducers( {
    form,
    formErrors,
    selectedQuick,
    remove
} );