import { combineReducers } from "redux";
import * as types from "./types";

import { createReducer } from "../../utils";

const loading = createReducer({ isLoading: false, msg: null })({
    [types.START_LOADING]: (state, action) => {
        //        console.log("action",action)
        return { ...state, isLoading: true, msg: action.msg };
    },
    [types.STOP_LOADING]: (state, action) => {
        //        console.log("action",action)
        return { ...state, isLoading: false, msg: null };
    }
});

export default combineReducers({
    loading
});