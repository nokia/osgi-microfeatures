/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { combineReducers } from 'redux';
import isStarted2d from './d2Reducer'
import config from './configReducer'
import userMessage from './userMsgReducer'

const rootReducer = combineReducers({
    isStarted2d,
    config,
    userMessage
});

export default rootReducer;