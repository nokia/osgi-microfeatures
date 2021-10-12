/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import { REQ_USER_INFO, LOGOUT, USERINFO, DISCONNECT } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const requestUserInfoData = () => ({ type: REQ_USER_INFO });
export const logout = () => ({type:LOGOUT});


// Specifics actions for requests
export const userinfodata = {
    request: () => action(USERINFO[REQUEST], {}),
    success: (id, response) => action(USERINFO[SUCCESS], { response }),
    failure: (id, error) => action(USERINFO[FAILURE], { error }, { id })
}

export const disconnectdata = {
    request: () => action(DISCONNECT[REQUEST], {}),
    success: (id, response) => action(DISCONNECT[SUCCESS], { response }),
    // On FAILURE, we consider as success, and let's be silent ( do not passing { error } )
    failure: (id, error) => action(DISCONNECT[FAILURE], { id })
}