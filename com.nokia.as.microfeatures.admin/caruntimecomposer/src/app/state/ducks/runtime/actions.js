/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import { 
    CRRUNTIME,
    CREATE_RUNTIME,
    CREATE_LEGACY_RUNTIME,
    CANCEL_CREATE_LEGACY_RUNTIME,
    SET_LAYOUT,
    OPEN_RUNTIME_LEGACY_PANEL,
    RESET_OPERATION,
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const createRuntime = (payload) => action(CREATE_RUNTIME,{payload})
export const createLegacyRuntime = (pgci) => action(CREATE_LEGACY_RUNTIME,{pgci})
export const cancelLegacyRuntime = () => action(CANCEL_CREATE_LEGACY_RUNTIME)
export const setLayout = (layout) => action(SET_LAYOUT,{layout})
export const openLegacyPanel = payload => action(OPEN_RUNTIME_LEGACY_PANEL,{payload})

export const resetRuntime = () => action(RESET_OPERATION)

// Specific actions for async request
export const createRuntimeRequest = (id,payload) => action(CRRUNTIME[REQUEST], payload)
export const createRuntimeSuccess = (id,response) => action(CRRUNTIME[SUCCESS], {response})
export const createRuntimeFailure = (id,error) => action(CRRUNTIME[FAILURE], {error},{id})