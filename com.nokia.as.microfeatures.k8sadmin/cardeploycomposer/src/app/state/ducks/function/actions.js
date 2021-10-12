/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import {
    SET_SELECTED_QUICKFUNCTION,
    OPENCLOSE_FUNCTIONPANEL,

    ADDFUNCTION,
    ADD_FUNCTION,
    PRESET_FUNCTION,
    SET_FORM_ERR,
    FULL_RESET,
    RESET_FUNCTION,
    DELETEFUNCTION,
    DELETE_FUNCTION,
    CONFIRM_DELETE_FUNCTION,
    CANCEL_DELETE_FUNCTION
    
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const setQuickSelOption = (id) => action(SET_SELECTED_QUICKFUNCTION,{id})
export const setFunctionPanelStatus = (open) => action(OPENCLOSE_FUNCTIONPANEL, {open})



export const addFunction = () => action(ADD_FUNCTION)
export const presetFunction = (form) => action(PRESET_FUNCTION,{ form })
export const setFormErrors = (errors) => action(SET_FORM_ERR,{ errors })

export const fullResetFunction = () => action(FULL_RESET)
export const resetFunction = () => action(RESET_FUNCTION)

export const deleteFunction = (payload) => action(DELETE_FUNCTION,{payload})
export const confirmDeleteFunction = () => action(CONFIRM_DELETE_FUNCTION)
export const cancelDeleteFunction = () => action(CANCEL_DELETE_FUNCTION)

// Specific actions for async request
export const addfunction = {
    request: (id,payload) => action(ADDFUNCTION[REQUEST], {payload}),
    success : (id,response) => action(ADDFUNCTION[SUCCESS], {response}),
    failure: (id,error)=> action(ADDFUNCTION[FAILURE], {error},{id})
}

export const delfunction = {
    request: (id,payload) => action(DELETEFUNCTION[REQUEST], payload),
    success : (id,response) => action(DELETEFUNCTION[SUCCESS], {response}),
    failure: (id,error)=> action(DELETEFUNCTION[FAILURE], {error},{id})
}
