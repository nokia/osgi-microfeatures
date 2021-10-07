import * as types from  "./types";

import { createReducer } from "../../utils";
import { utils } from "../../../utilities";

const { msgTypes } = utils;

const initialState = {
    // Payload fields to create a runtime
    name: '',
    bsn: '',
    version : '',
    // pgci mandatory when chosen layout is LEGACY
    pgci : null,
    // Operations statuses ( null, 'started' , 'ok', 'error')
    create : null, // Creation operation
    layout : types.LAYOUT_TYPES.CSF,
    // Opened legacy panel (true means the panel to enter pgci is/should displayed)
    legacyPanel : false

}

const snapshot = createReducer( initialState )( {
    [ types.CREATE_RUNTIME ] : ( state, action ) => {
        const {name,bsn,version} = action.payload
       return ({...state, name:name, bsn:bsn,version:version,create:'started'})
    },
    [ types.OPEN_RUNTIME_LEGACY_PANEL] : (state,action) => {
        const {name,bsn,version} = action.payload
       return ({...state, name:name, bsn:bsn,version:version,legacyPanel:true})
    },
    [  types.CREATE_LEGACY_RUNTIME ] : (state, action) => {
        return ({...state, pgci : action.pgci, legacyPanel:false, create:'started'})
    },
    [ types.CANCEL_CREATE_LEGACY_RUNTIME ] : (state,action) => ({...state,legacyPanel:false}),
    [ types.CRRUNTIME[msgTypes.SUCCESS] ] : ( state, action ) =>  ({...state,create:'ok'}),
    [ types.CRRUNTIME[msgTypes.FAILURE] ] : ( state, action ) => ({...state,create:'error'}),
    [ types.SET_LAYOUT ] : (state,action) => ({...state,layout: action.layout}),
    [ types.RESET_OPERATION ] : ( state, action ) => ({...initialState})
})

export default snapshot;