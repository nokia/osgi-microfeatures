/* eslint-disable no-constant-condition */
import { call, select } from 'redux-saga/effects'
import { api } from '../../../services'
import {
    assembly,
    delassembly,
    createAssembly,
    resetAssembly,
    presetAssembly,
    setAsmbPanelStatus,
    setRuntime,
    toggleFeature,
    setSelectedFeatures,
    setQuickSelOption,
    deleteAssembly,
    confirmDeleteAssembly,
    cancelDeleteAssembly
} from './actions'
import { utils } from "../../../utilities"

const { fetchEntity } = utils;
export const postAssembly = fetchEntity.bind(null, assembly, api.postRequest)
export const delAssembly = fetchEntity.bind(null, delassembly, api.deleteRequest)



// TO AVOID A DEPENDENCY WITH selectors of entities and userMsg modules
const getData = (state, entity) => {
    const error = (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null
    return ({ data: state[entity], error: error })
}
//
// GENERATORS
//
// Create assembly
export function* doCreateAssembly(payload) {
    yield call(postAssembly, 'Assembly creation', '/cmd/assembly', payload)
    // Returns the { data , error } object
    return yield select(getData, 'assembly');
}

// Delete assembly
export function* doDeleteAssembly(payload) {
    yield call(delAssembly, 'Assembly deletion', '/cmd/assembly', payload)
    // Returns the { data , error } object
    return yield select(getData, 'assembly');
}


export {
    createAssembly,
    resetAssembly,
    presetAssembly,
    setAsmbPanelStatus,
    setRuntime,
    toggleFeature,
    setSelectedFeatures,
    setQuickSelOption,
    deleteAssembly,
    confirmDeleteAssembly,
    cancelDeleteAssembly   
}
