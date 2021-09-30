import { utils } from "../../../utilities"
import { REQ_DATA, OBRS, FEATURES, ASSEMBLIES, SNAPSHOTS, RELOADREPOS, SELECT_OBR } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const requestData = () => ({ type: REQ_DATA });
export const selectObr = (url) => action(SELECT_OBR,{url});



// Specifics actions for requests
export const obrs = {
    request: () => action(OBRS[REQUEST], {}),
    success: (id, response) => action(OBRS[SUCCESS], { response }),
    failure: (id, error) => action(OBRS[FAILURE], { error }, { id })
}

export const reloadrepos = {
    request: (id, payload) => action(RELOADREPOS[REQUEST], payload),
    success: (id, response) => action(RELOADREPOS[SUCCESS], { response }),
    failure: (id, error) => action(RELOADREPOS[FAILURE], { error }, { id })
}

export const features = {
    request: (params) => action(FEATURES[REQUEST], { params }),
    success: (id, response) => action(FEATURES[SUCCESS], { response }),
    failure: (id, error) => action(FEATURES[FAILURE], { error }, { id })
}

export const assemblies = {
    request: (params) => action(ASSEMBLIES[REQUEST], { params }),
    success: (id, response) => action(ASSEMBLIES[SUCCESS], { response }),
    failure: (id, error) => action(ASSEMBLIES[FAILURE], { error }, { id })
}

export const snapshots = {
    request: (params) => action(SNAPSHOTS[REQUEST], { params }),
    success: (id, response) => action(SNAPSHOTS[SUCCESS], { response }),
    failure: (id, error) => action(SNAPSHOTS[FAILURE], { error }, { id })
}
