/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import {
    START_2D,
    STOP_2D
} from '../actions/actionTypes';


const d2Reducer = (state=false, action) => {
    switch (action.type) {
        case START_2D:
            return true;
        case STOP_2D:
            return false;
        default :
            return state;
    }
}

export default d2Reducer;
