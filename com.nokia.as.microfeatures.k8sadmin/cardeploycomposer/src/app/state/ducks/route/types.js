import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

// Basis actions types
export const SET_SELECTED_QUICKROUTE = 'route/SET_SELECTED_QUICK'
export const OPENCLOSE_ROUTEPANEL = 'route/OPENCLOSE_ROUTEPANEL'


export const ADD_ROUTE = "route/ADD_ROUTE"
export const FULL_RESET = 'route/FULL_RESET'
export const RESET_ROUTE = 'route/RESET_ROUTE'
export const PRESET_ROUTE = 'route/PRESET_ROUTE'
export const SET_FORM_ERR = 'route/SET_FORM_ERR'


export const DELETE_ROUTE = 'route/DELETE_ROUTE'
export const CONFIRM_DELETE_ROUTE = 'route/CONFIRM_DELETE_ROUTE'
export const CANCEL_DELETE_ROUTE = 'route/CANCEL_DELETE_ROUTE'

// Specifics actions types for requests
export const ADDROUTE = createRequestTypes('route/ADD-ROUTE');
export const DELETEROUTE = createRequestTypes('route/DEL-ROUTE');