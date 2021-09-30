import { utils } from "../../../utilities"
import {
    SET_SELECTED_RUNTIMEFEATURE,
    TOGGLE_FEATURE,
    SET_SELECTED_FEATURES,
    SET_SELECTED_QUICK,
    SET_DEPLOY_REQUIREMENTS,
    RESET_DEPLOY_REQUIREMENTS,
    DEPLOYRUNTIME,
    DEPLOY_RUNTIME,
    PRESET_DEPLOY,
    SET_FORM_ERR,
    FULL_RESET,
    RESET_DEPLOY,
    UNDEPLOYRUNTIME,
    UNDEPLOY_RUNTIME,
    CONFIRM_UNDEPLOY_RUNTIME,
    CANCEL_UNDEPLOY_RUNTIME,
    OPEN_DETAILS,
    CLOSE_DETAILS
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const setRuntimeFeature = (id) => action(SET_SELECTED_RUNTIMEFEATURE, {id})
export const toggleFeature = (id) => action(TOGGLE_FEATURE,{id})
export const setSelectedFeatures = (featuresIds,runtimeFeatureId) => action(SET_SELECTED_FEATURES,{featuresIds},{runtimeFeatureId})
export const setQuickSelOption = (id) => action(SET_SELECTED_QUICK,{id})


export const setDeployRequirements = (payload) => action(SET_DEPLOY_REQUIREMENTS,{payload})
export const resetDeployRequirements = () => action(RESET_DEPLOY_REQUIREMENTS)

export const deployRuntime = () => action(DEPLOY_RUNTIME)
export const presetDeploy = (form) => action(PRESET_DEPLOY,{ form })
export const setFormErrors = (errors) => action(SET_FORM_ERR,{ errors })

export const fullResetDeploy = () => action(FULL_RESET)
export const resetDeploy = () => action(RESET_DEPLOY)

export const undeployRuntime = (payload) => action(UNDEPLOY_RUNTIME,{payload})
export const confirmUndeployRuntime = () => action(CONFIRM_UNDEPLOY_RUNTIME)
export const cancelUndeployRuntime = () => action(CANCEL_UNDEPLOY_RUNTIME)

export const openRuntimeDetails = (id) => action(OPEN_DETAILS, {id})
export const closeRuntimeDetails = () => action(CLOSE_DETAILS)

// Specific actions for async request
export const depruntime = {
    request: (id,payload) => action(DEPLOYRUNTIME[REQUEST], {payload}),
    success : (id,response) => action(DEPLOYRUNTIME[SUCCESS], {response}),
    failure: (id,error)=> action(DEPLOYRUNTIME[FAILURE], {error},{id})
}

export const undepruntime = {
    request: (id,payload) => action(UNDEPLOYRUNTIME[REQUEST], payload),
    success : (id,response) => action(UNDEPLOYRUNTIME[SUCCESS], {response}),
    failure: (id,error)=> action(UNDEPLOYRUNTIME[FAILURE], {error},{id})
}
