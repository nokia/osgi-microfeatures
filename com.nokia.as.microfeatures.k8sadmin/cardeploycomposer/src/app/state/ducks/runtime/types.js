import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

// Basis actions types
export const SET_SELECTED_RUNTIMEFEATURE = 'runtime/SET_SELECTED_RUNTIMEFEATURE'

export const TOGGLE_FEATURE = 'runtime/TOGGLE_FEATURE'
export const SET_SELECTED_FEATURES = 'runtime/SET_SELECTED_FEATURES'

export const SET_SELECTED_QUICK = 'runtime/SET_SELECTED_QUICK'

export const DEPLOY_RUNTIME = "runtime/DEPLOY_RUNTIME"
export const FULL_RESET = 'runtime/FULL_RESET'
export const RESET_DEPLOY = 'runtime/RESET_DEPLOY'
export const PRESET_DEPLOY = 'runtime/PRESET_DEPLOY'
export const SET_FORM_ERR = 'runtime/SET_FORM_ERR'

export const SET_DEPLOY_REQUIREMENTS = 'runtime/SET_DEPLOY_REQUIREMENTS'
export const RESET_DEPLOY_REQUIREMENTS = 'runtime/RESET_DEPLOY_REQUIREMENTS'

export const UNDEPLOY_RUNTIME = 'runtime/UNDEPLOY_RUNTIME'
export const CONFIRM_UNDEPLOY_RUNTIME = 'runtime/CONFIRM_UNDEPLOY_RUNTIME'
export const CANCEL_UNDEPLOY_RUNTIME = 'runtime/CANCEL_UNDEPLOY_RUNTIME'

export const OPEN_DETAILS = 'runtime/OPEN_DETAILS'
export const CLOSE_DETAILS = 'runtime/CLOSE_DETAILS'

// Specifics actions types for requests
export const DEPLOYRUNTIME = createRequestTypes('runtime/DEP-RUNTIME');
export const UNDEPLOYRUNTIME = createRequestTypes('runtime/UNDEP-RUNTIME');