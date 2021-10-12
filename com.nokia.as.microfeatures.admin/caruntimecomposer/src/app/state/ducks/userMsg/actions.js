/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import * as types from "./types";

const { action } = utils;

export const setInfoMessage = (title,message) => {
    const usermsg = { type:'info', title:title, message:message, details:null }
    return action(types.SET_INFO_MESSAGE,{usermsg})
}
export const setWarnMessage = (title,warn1,warn2) => {
    const usermsg = { type:'warn', title:title, message:warn1, details:warn2 }
    return action(types.SET_WARN_MESSAGE,{usermsg})
}
export const setErrorMessage = (title,error,details) => {
    const message = (error)?error:new Date().toString();
    const usermsg = { type:'error', title:title, message:message, details:details }
    return action(types.SET_ERROR_MESSAGE,{usermsg})
}

export const resetUserMessage = () => action(types.RESET_USER_MESSAGE)