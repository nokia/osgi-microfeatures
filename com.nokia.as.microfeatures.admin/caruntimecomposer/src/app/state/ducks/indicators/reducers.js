import { combineReducers } from "redux";
import * as types from "./types";

import { createReducer } from "../../utils";

const loading = createReducer({ isLoading: false, msg: null, welcome: true })({
    [types.START_LOADING]: (state, action) => {
        //        console.log("action",action)
        return { ...state, isLoading: true, msg: action.msg };
    },
    [types.STOP_LOADING]: (state, action) => {
        //        console.log("action",action)
        return { ...state, isLoading: false, msg: null };
    },
    [types.HIDE_WELCOME]: (state, action) => {
        //        console.log("action",action)
        return { ...state, welcome: false };
    }
});

export default combineReducers({
    loading
});