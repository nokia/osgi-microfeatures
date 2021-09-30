import * as types from  "./types";

import { createReducer } from "../../utils";
import { utils } from "../../../utilities";

const { msgTypes } = utils;

const initialState = {
    // Payload fields to create / remove a snapshot
    name: '',
    bsn: '',
    version : '',
    // Operations statuses ( null, 'started' , 'ok', 'error')
    create : null, // Creation operation
    remove : null, // Delete operation
    // Flag to true means a confirmation to remove a snapshot must be displayed
    confirm : false
}

const snapshot = createReducer( initialState )( {
    [ types.CREATE_SNAPSHOT ] : ( state, action ) => {
        const {name,bsn,version} = action.payload
       return ({...state, name:name, bsn:bsn,version:version,create:'started'})
    },
    [ types.CRSNAPSHOT[msgTypes.SUCCESS] ] : ( state, action ) =>  ({...state,create:'ok'}),
    [ types.CRSNAPSHOT[msgTypes.FAILURE] ] : ( state, action ) => ({...state,create:'error'}),
    [ types.DELETE_SNAPSHOT ] : ( state, action ) => {
        const {name,bsn,version} = action.payload
       return ({...state, name:name, bsn:bsn,version:version,confirm:true})
    },
    [ types.DELSNAPSHOT[msgTypes.SUCCESS] ] : ( state, action ) => ({...state,remove:'ok'}),
    [ types.DELSNAPSHOT[msgTypes.FAILURE] ] : ( state, action ) => ({...state,remove:'error'}),
    [ types.CONFIRM_DELETE_SNAPSHOT ] : ( state, action ) => ({...state,confirm:false,remove:'started'}),
    [ types.CANCEL_DELETE_SNAPSHOT ] : ( state, action ) => ({...state,confirm:false}),
    [ types.RESET_OPERATION ] : ( state, action ) => ({...initialState})
})

export default snapshot;