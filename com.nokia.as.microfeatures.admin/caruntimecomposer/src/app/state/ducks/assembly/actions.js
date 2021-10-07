import { utils } from "../../../utilities"
import { ASSEMBLY,
    DELASSEMBLY,
    CREATE_ASSEMBLY,
    RESET_ASSEMBLY,
    PRESET_ASSEMBLY,
    DELETE_ASSEMBLY,
    CONFIRM_DELETE_ASSEMBLY,
    CANCEL_DELETE_ASSEMBLY,
    SET_SELECTED_RUNTIME,
    TOGGLE_FEATURE,
    SET_SELECTED_FEATURES,
    OPENCLOSE_ASMBPANEL,
    SET_SELECTED_QUICK
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const createAssembly = (payload) => action(CREATE_ASSEMBLY,{payload})
export const resetAssembly = () => action(RESET_ASSEMBLY)
export const presetAssembly = (form) => action(PRESET_ASSEMBLY,{ form })
export const deleteAssembly = (payload) => action(DELETE_ASSEMBLY,{payload})
export const confirmDeleteAssembly = () => action(CONFIRM_DELETE_ASSEMBLY)
export const cancelDeleteAssembly = () => action(CANCEL_DELETE_ASSEMBLY)

export const setRuntime = (id) => action(SET_SELECTED_RUNTIME, {id})
export const toggleFeature = (id) => action(TOGGLE_FEATURE,{id})
export const setSelectedFeatures = (featuresIds,runtimeId) => action(SET_SELECTED_FEATURES,{featuresIds},{runtimeId})
export const setQuickSelOption = (id) => action(SET_SELECTED_QUICK,{id})

export const setAsmbPanelStatus = (open) => action(OPENCLOSE_ASMBPANEL, {open})

// Specific actions for async request
export const assembly = {
    request: (id,payload) => action(ASSEMBLY[REQUEST], payload),
    success : (id,response) => action(ASSEMBLY[SUCCESS], {response}),
    failure: (id,error) => action(ASSEMBLY[FAILURE], {error},{id})
}

export const delassembly = {
    request: (id,payload) => action(DELASSEMBLY[REQUEST], payload),
    success : (id,response) => action(DELASSEMBLY[SUCCESS], {response}),
    failure: (id,error)=> action(DELASSEMBLY[FAILURE], {error},{id})
}
