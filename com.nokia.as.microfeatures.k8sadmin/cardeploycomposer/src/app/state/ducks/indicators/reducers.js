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