import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

// Basis actions types
export const SET_SELECTED_QUICKFUNCTION = 'function/SET_SELECTED_QUICK'
export const OPENCLOSE_FUNCTIONPANEL = 'function/OPENCLOSE_FUNCTIONPANEL'


export const ADD_FUNCTION = "function/ADD_FUNCTION"
export const FULL_RESET = 'function/FULL_RESET'
export const RESET_FUNCTION = 'function/RESET_FUNCTION'
export const PRESET_FUNCTION = 'function/PRESET_FUNCTION'
export const SET_FORM_ERR = 'function/SET_FORM_ERR'


export const DELETE_FUNCTION = 'function/DELETE_FUNCTION'
export const CONFIRM_DELETE_FUNCTION = 'function/CONFIRM_DELETE_FUNCTION'
export const CANCEL_DELETE_FUNCTION = 'function/CANCEL_DELETE_FUNCTION'

// Specifics actions types for requests
export const ADDFUNCTION = createRequestTypes('function/ADD-FUNCTION');
export const DELETEFUNCTION = createRequestTypes('function/DEL-FUNCTION');