/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import * as types from "../actions/actionTypes";

// Updates error message to notify about the failed fetches.
function userMessage(state = null, action) {
    const { type, error, id } = action
    if (error) {
        const title = (id) ? id + ' error' : 'Operation error';
        const message = new Date().toString();
        const details = action.error;
        return {
            type: 'error',
            title: title,
            message: message,
            details: details
        }
    }
    switch (type) {
        case types.RESET_USER_MESSAGE:
            return null;
        case types.SET_INFO_MESSAGE:
        case types.SET_WARN_MESSAGE:
        case types.SET_ERROR_MESSAGE:
            return {...action.usermsg}

        default:
            return state
    }
}

export default userMessage

export const getUserMessage = (state) => state.userMessage
export const getErrorMessage = (state) => (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null