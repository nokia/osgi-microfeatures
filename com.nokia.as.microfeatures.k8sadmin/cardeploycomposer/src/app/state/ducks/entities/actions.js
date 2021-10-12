/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import { 
    REQ_DATA, 
    POLL_START_RUNTIMES,
    POLL_STOP_RUNTIMES,
    POLL_START_ROUTES,
    POLL_STOP_ROUTES,
    POLL_START_FUNCTIONS,
    POLL_STOP_FUNCTIONS,
    OBRS,
    FEATURES,
    RUNTIMES,
    ROUTES,
    FUNCTIONS,
    RELOADREPOS,
    SELECT_OBR } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const requestData = () => ({ type: REQ_DATA });
export const selectObr = (url) => action(SELECT_OBR,{url});

export const pollStartRuntimes = () => action(POLL_START_RUNTIMES)
export const pollStopRuntimes = () => action(POLL_STOP_RUNTIMES)

export const pollStartRoutes = () => action(POLL_START_ROUTES)
export const pollStopRoutes = () => action(POLL_STOP_ROUTES)

export const pollStartFunctions = () => action(POLL_START_FUNCTIONS)
export const pollStopFunctions = () => action(POLL_STOP_FUNCTIONS)



// Specifics actions for requests
export const obrs = {
    request: () => action(OBRS[REQUEST], {}),
    success: (id, response) => action(OBRS[SUCCESS], { response }),
    failure: (id, error) => action(OBRS[FAILURE], { error }, { id })
}

export const reloadrepos = {
    request: (id, payload) => action(RELOADREPOS[REQUEST], payload),
    success: (id, response) => action(RELOADREPOS[SUCCESS], { response }),
    failure: (id, error) => action(RELOADREPOS[FAILURE], { error }, { id })
}

export const features = {
    request: (params) => action(FEATURES[REQUEST], { params }),
    success: (id, response) => action(FEATURES[SUCCESS], { response }),
    failure: (id, error) => action(FEATURES[FAILURE], { error }, { id })
}

export const runtimes = {
    request: (params) => action(RUNTIMES[REQUEST], { params }),
    success: (id, response) => action(RUNTIMES[SUCCESS], { response }),
    failure: (id, error) => action(RUNTIMES[FAILURE], { error }, { id })
}

export const routes = {
    request: (params) => action(ROUTES[REQUEST], { params }),
    success: (id, response) => action(ROUTES[SUCCESS], { response }),
    failure: (id, error) => action(ROUTES[FAILURE], { error }, { id })
}

export const functions = {
    request: (params) => action(FUNCTIONS[REQUEST], { params }),
    success: (id, response) => action(FUNCTIONS[SUCCESS], { response }),
    failure: (id, error) => action(FUNCTIONS[FAILURE], { error }, { id })
}
