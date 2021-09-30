import { combineReducers } from "redux";
import * as types from "./types";
import { InitialView } from "./constants"
import { createReducer } from "../../utils";

const activeView = createReducer( InitialView )( {
    [ types.SET_VIEW ] : ( state, action ) => {
//        console.log("activeView reducer action",action)
        return action.view;
    }
} );

export default combineReducers( {
    activeView
} );