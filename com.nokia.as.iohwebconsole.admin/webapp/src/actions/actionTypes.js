/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/* Actions are defined here */
export const START_2D = "START_2D";
export const STOP_2D = "STOP_2D";

export const SET_VIEW = "SET_VIEW";
export const SET_FLUX = "SET_FLUX";

export const SET_INFO_MESSAGE='userMsg/SET_INFO'
export const SET_WARN_MESSAGE='userMsg/SET_WARN'
export const SET_ERROR_MESSAGE='userMsg/SET_ERROR'
export const RESET_USER_MESSAGE='userMsg/RESET_MESSAGE'

/* view selector values */
export const viewSelectorValues = {
    standard : {"hierarchical" : false},
    horizontal: {"hierarchical" : true},
    vertical :  {"hierarchical" : { "orientation" : "vertical" }}
}
/* mode selector value */
// O = Overview, C = Client only, A = Agent Only , F = Full open
export const modeSelectorValues = {
    overview : "O",
    clientonly : "C",
    agentonly : "A",
    fullopen : "F"
}

/* flux selector values */
export const fluxSelectorValues = {
    requestResponse: "detailed",
    request: "request",
    response: "response",
    lost: "lost",
    percentage: "percentage"
}

// default values for selectors
export const defaultConfig = {
    view : viewSelectorValues.standard,
    mode : modeSelectorValues.fullopen,
    flux : fluxSelectorValues.requestResponse
}