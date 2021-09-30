import { utils } from "../../../utilities"
import { 
    CRSNAPSHOT,
    DELSNAPSHOT,
    CREATE_SNAPSHOT,
    DELETE_SNAPSHOT,
    RESET_OPERATION,
    CONFIRM_DELETE_SNAPSHOT,
    CANCEL_DELETE_SNAPSHOT
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
export const createSnapshot = (payload) => action(CREATE_SNAPSHOT,{payload})
export const deleteSnapshot = (payload) => action(DELETE_SNAPSHOT,{payload})
export const confirmDeleteSnapshot = () => action(CONFIRM_DELETE_SNAPSHOT)
export const cancelDeleteSnapshot = () => action(CANCEL_DELETE_SNAPSHOT)
export const reset = () => action(RESET_OPERATION)

// Specific actions for async request
export const crsnapshot = {
    request: (id,payload) => action(CRSNAPSHOT[REQUEST], payload),
    success : (id,response) => action(CRSNAPSHOT[SUCCESS], {response}),
    failure: (id,error) => action(CRSNAPSHOT[FAILURE], {error},{id})
}

export const delsnapshot = {
    request: (id,payload) => action(DELSNAPSHOT[REQUEST], payload),
    success : (id,response) => action(DELSNAPSHOT[SUCCESS], {response}),
    failure: (id,error)=> action(DELSNAPSHOT[FAILURE], {error},{id})
}