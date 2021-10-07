import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

// Constants for runtime layouts
export const LAYOUT_TYPES = { CSF : 'CSF', LEGACY : 'LEGACY'}

// Basis actions types
export const CREATE_RUNTIME = 'runtime/CREATE_RUNTIME'
export const CREATE_LEGACY_RUNTIME = 'runtime/CREATE_LEGACY_RUNTIME'
export const CANCEL_CREATE_LEGACY_RUNTIME = 'runtime/CANCEL_CREATE_LEGACY_RUNTIME'
export const SET_LAYOUT = 'runtime/SET_LAYOUT'
export const OPEN_RUNTIME_LEGACY_PANEL = 'runtime/OPEN_RUNTIME_LEGACY_PANEL'
export const RESET_OPERATION = 'runtime/RESET_OPERATION'

// Specifics actions types for requests
export const CRRUNTIME = createRequestTypes('runtime/CR-RUNTIME');