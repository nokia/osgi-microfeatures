/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/* eslint-disable no-constant-condition */
import { call, select } from 'redux-saga/effects'
import { api } from '../../../services'
import {
    setRuntimeFeature,
    toggleFeature,
    setSelectedFeatures,
    setQuickSelOption,
    deployRuntime,
    undeployRuntime,
    confirmUndeployRuntime,
    cancelUndeployRuntime,
    depruntime,
    undepruntime,
    resetDeploy,
    fullResetDeploy,
    presetDeploy,
    setFormErrors,
    setDeployRequirements,
    resetDeployRequirements,
    openRuntimeDetails,
    closeRuntimeDetails
} from './actions'
import { utils } from "../../../utilities"

const { fetchEntity } = utils;

export const postFeatures = fetchEntity.bind(null, depruntime, api.postRequest)
export const delRuntime = fetchEntity.bind(null,undepruntime,api.deleteRequest)



// TO AVOID A DEPENDENCY WITH selectors of entities and userMsg modules
const getData = (state, entity) => {
    const error = (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null
    return ({ data: state[entity], error: error })
}
//
// GENERATORS
//

// Deploy runtime
export function* doDeployRuntime(payload) {
    yield call(postFeatures, 'Runtime deployment', '../cmd/deploy', payload)
    // Returns the { data , error } object
    return yield select(getData, 'runtime');
}

// Undeploy runtime
export function* doUndeployRuntime(payload) {
    yield call(delRuntime, 'Runtime undeploy', '../cmd/undeploy', payload)
    // Returns the { data , error } object
    return yield select(getData, 'runtime');
}


export {
    setRuntimeFeature,
    toggleFeature,
    setSelectedFeatures,
    setQuickSelOption,
    deployRuntime,
    resetDeploy,
    fullResetDeploy,
    presetDeploy,
    setFormErrors,
    undeployRuntime,
    cancelUndeployRuntime,
    confirmUndeployRuntime,
    setDeployRequirements,
    resetDeployRequirements,
    openRuntimeDetails,
    closeRuntimeDetails
}
