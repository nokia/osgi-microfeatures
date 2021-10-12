/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/* eslint-disable no-constant-condition */
import { call } from 'redux-saga/effects'
import { api } from '../../../services'
import {
    loadBundles,
    bundles,
    resolveBundle,
    bresolution,
    findDepsBundle,
    bdependent,
    closeBundlePanel
} from './actions'
import { utils } from "../../../utilities"

const { fetchEntity } = utils;
const fetchBundles = fetchEntity.bind(null, bundles, api.fetchBundles)
const fetchResolveds = fetchEntity.bind(null, bresolution, api.fetchResolveds)
const fetchDependents = fetchEntity.bind(null, bdependent, api.fetchDependents)

// load the bundles in according to the obr url
export function* doLoadBundles(url, local) {
    const loadText = (local === true)?'Local': 'Current'
    console.log("doLoadBundles",url,loadText)
    const params = { local:local , url:url}
       
   yield call(fetchBundles,loadText + ' Bundles loading', url ,params)

}

// resolve a bundle
export function* doResolveBundle(bundle,obrs) {
    console.log("doResolveBundle",bundle,obrs)
    yield call(fetchResolveds,' Bundle resolving', bundle.bsn,bundle.version,obrs.toString())
}

// find dependents of a bundle
export function* doFindDependentsBundle(bundle,obrs) {
    console.log("doFindDependentsBundle",bundle,obrs)
    yield call(fetchDependents,' Bundle dependencies', bundle,obrs.toString())
}


/*
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
    yield call(postAssembly, 'Assembly creation', 'cmd/assembly', payload)
    // Returns the { data , error } object
    return yield select(getData, 'assembly');
}
*/
export {
    loadBundles,
    resolveBundle,
    findDepsBundle,
    closeBundlePanel
}
