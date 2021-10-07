/* eslint-disable no-constant-condition */
import { call, select } from 'redux-saga/effects'
import { api } from '../../../services'
import {
    setQuickSelOption,
    setFunctionPanelStatus,
    addFunction,
    deleteFunction,
    confirmDeleteFunction,
    cancelDeleteFunction,
    addfunction,
    delfunction,
    resetFunction,
    fullResetFunction,
    presetFunction,
    setFormErrors,
} from './actions'
import { utils } from "../../../utilities"

const { fetchEntity } = utils;

export const postFunction = fetchEntity.bind(null, addfunction, api.postRequest)
export const delFunction = fetchEntity.bind(null,delfunction,api.deleteRequest)



// TO AVOID A DEPENDENCY WITH selectors of entities and userMsg modules
const getData = (state, entity) => {
    const error = (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null
    return ({ data: state[entity], error: error })
}
//
// GENERATORS
//

// Add function
export function* doAddFunction(payload) {
    yield call(postFunction, 'Function deployment', '../cmd/function', payload)
    // Returns the { data , error } object
    return yield select(getData, 'function');
}

// Delete function
export function* doDeleteFunction(payload) {
    yield call(delFunction, 'Function deletefunction', '../cmd/function', payload)
    // Returns the { data , error } object
    return yield select(getData, 'function');
}


export {
    setQuickSelOption,
    setFunctionPanelStatus,
    addFunction,
    resetFunction,
    fullResetFunction,
    presetFunction,
    setFormErrors,
    deleteFunction,
    cancelDeleteFunction,
    confirmDeleteFunction
}
