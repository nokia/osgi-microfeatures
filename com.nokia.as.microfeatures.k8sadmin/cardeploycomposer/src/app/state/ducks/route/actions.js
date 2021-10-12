/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import {
    SET_SELECTED_QUICKROUTE,
    OPENCLOSE_ROUTEPANEL,

    ADDROUTE,
    ADD_ROUTE,
    PRESET_ROUTE,
    SET_FORM_ERR,
    FULL_RESET,
    RESET_ROUTE,
    DELETEROUTE,
    DELETE_ROUTE,
    CONFIRM_DELETE_ROUTE,
    CANCEL_DELETE_ROUTE
    
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const setQuickSelOption = (id) => action(SET_SELECTED_QUICKROUTE,{id})
export const setRoutePanelStatus = (open) => action(OPENCLOSE_ROUTEPANEL, {open})



export const addRoute = () => action(ADD_ROUTE)
export const presetRoute = (form) => action(PRESET_ROUTE,{ form })
export const setFormErrors = (errors) => action(SET_FORM_ERR,{ errors })

export const fullResetRoute = () => action(FULL_RESET)
export const resetRoute = () => action(RESET_ROUTE)

export const deleteRoute = (payload) => action(DELETE_ROUTE,{payload})
export const confirmDeleteRoute = () => action(CONFIRM_DELETE_ROUTE)
export const cancelDeleteRoute = () => action(CANCEL_DELETE_ROUTE)

// Specific actions for async request
export const addroute = {
    request: (id,payload) => action(ADDROUTE[REQUEST], {payload}),
    success : (id,response) => action(ADDROUTE[SUCCESS], {response}),
    failure: (id,error)=> action(ADDROUTE[FAILURE], {error},{id})
}

export const delroute = {
    request: (id,payload) => action(DELETEROUTE[REQUEST], payload),
    success : (id,response) => action(DELETEROUTE[SUCCESS], {response}),
    failure: (id,error)=> action(DELETEROUTE[FAILURE], {error},{id})
}
