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
    setQuickSelOption,
    setRoutePanelStatus,
    addRoute,
    deleteRoute,
    confirmDeleteRoute,
    cancelDeleteRoute,
    addroute,
    delroute,
    resetRoute,
    fullResetRoute,
    presetRoute,
    setFormErrors,
} from './actions'
import { utils } from "../../../utilities"

const { fetchEntity } = utils;

export const postRoute = fetchEntity.bind(null, addroute, api.postRequest)
export const delRoute = fetchEntity.bind(null,delroute,api.deleteRequest)



// TO AVOID A DEPENDENCY WITH selectors of entities and userMsg modules
const getData = (state, entity) => {
    const error = (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null
    return ({ data: state[entity], error: error })
}
//
// GENERATORS
//

// Add route
export function* doAddRoute(payload) {
    yield call(postRoute, 'Adding Route', '../cmd/route', payload)
    // Returns the { data , error } object
    return yield select(getData, 'route');
}

// Delete route
export function* doDeleteRoute(payload) {
    yield call(delRoute, 'Deleting Route', '../cmd/route', payload)
    // Returns the { data , error } object
    return yield select(getData, 'route');
}


export {
    setQuickSelOption,
    setRoutePanelStatus,
    addRoute,
    resetRoute,
    fullResetRoute,
    presetRoute,
    setFormErrors,
    deleteRoute,
    cancelDeleteRoute,
    confirmDeleteRoute
}
