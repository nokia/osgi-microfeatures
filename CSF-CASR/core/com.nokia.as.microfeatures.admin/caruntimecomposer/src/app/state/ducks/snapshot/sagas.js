/* eslint-disable no-constant-condition */
import { call, select } from 'redux-saga/effects'
import { api } from '../../../services'
import {
    crsnapshot,
    delsnapshot,
    createSnapshot,
    deleteSnapshot,
    confirmDeleteSnapshot,
    cancelDeleteSnapshot,
    reset
} from './actions'
import { utils } from "../../../utilities"

const { fetchEntity } = utils;
export const postSnapshot = fetchEntity.bind(null, crsnapshot, api.postRequest)
export const delSnapshot = fetchEntity.bind(null, delsnapshot, api.deleteRequest)

// TO AVOID A DEPENDENCY WITH selectors of entities and userMsg modules
const getData = (state, entity) => {
    const error = (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null
    return ({ data: state[entity], error: error })
}
//
// GENERATORS
//
// Create snapshot
export function* doCreateSnapshot(payload) {
    yield call(postSnapshot, 'Snapshot creation', '/cmd/snapshot', payload)
    // Returns the { data , error } object
    return yield select(getData, 'snapshot');
}

// Delete snapshot
export function* doDeleteSnapshot(payload) {
    yield call(delSnapshot, 'Snapshot deletion', '/cmd/snapshot', payload)
    // Returns the { data , error } object
    return yield select(getData, 'snapshot');
}

export {
    createSnapshot,
    deleteSnapshot,
    confirmDeleteSnapshot,
    cancelDeleteSnapshot,
    reset
}