/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */


import * as types from "./types";
import { createReducer } from "../../utils";

const initialStates = {
    open: false,
    id: '',
    pod: '',
    url: ''
}

const terminal = createReducer(initialStates)({
    [types.START_TERMINAL]: (state, action) => {
        const { id , pod, url } = action;
        return { open: false, id , pod, url };
    },
    [types.OPEN_TERMINAL]: (state, action) => {
        return { ...state, open: true };
    },
    [types.CLOSE_TERMINAL]: (state, action) => (initialStates)

});


export default terminal;