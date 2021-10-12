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
    requestData,
    obrs,
    features,
    runtimes,
    reloadrepos,
    selectObr,
    pollStartRuntimes,
    pollStopRuntimes,
    routes,
    pollStartRoutes,
    pollStopRoutes,
    functions,
    pollStartFunctions,
    pollStopFunctions

} from './actions'
import { utils } from "../../../utilities"
import { default as selectors } from './selectors'

const { fetchEntity } = utils;

const fetchObrs = fetchEntity.bind(null, obrs, api.fetchObrs)
const fetchFeatures = fetchEntity.bind(null, features, api.fetchFeatures)
const fetchRuntimes = fetchEntity.bind(null, runtimes, api.fetchRuntimes)
const fetchRoutes = fetchEntity.bind(null, routes, api.fetchRoutes)
const fetchFunctions = fetchEntity.bind(null, functions, api.fetchFunctions)

const postReloadRepos = fetchEntity.bind(null, reloadrepos, api.postRequest)

//
// GENERATORS
//

// load obrs unless it is cached
export function* loadObrs() {
    const obrs = yield select(selectors.getObrs)
    console.log("loadObrs",obrs);
    if (!obrs || !obrs.local || obrs.local.length < 1 || !obrs.remotes || obrs.remotes.length < 1) {
        console.log("loadObrs calling fetchObrsList");
        yield call(fetchObrs, 'OBR list load')
    }
    // Returns the obrs list array
    return yield select(selectors.getObrs);
}

// Ask the server to reload repos
export function* reloadRepos(obrs) {
    yield call(postReloadRepos, 'Repository reload', '../cmd/repos/reload', { obrs:`${obrs}`})
}

// load the features in according to the obrs
export function* loadFeatures(obrs, force) {
    const params = obrs.toString();
    //    console.log("loadFeaturesList",obrsList,force,params);

    const features = yield select(selectors.getFeatures)
    if (force || !features || features.length < 1) {
        yield call(fetchFeatures,'Features loading', params)
    }
    // Returns the features list as map
    return yield select(selectors.getFeatures);
}

// load runtimes list
export function* loadRuntimes(force) {
    const runtimes = yield select(selectors.getRuntimes)
    if (force || !runtimes || runtimes.length < 1) {
        yield call(fetchRuntimes,'Runtimes loading')
    }
    // Returns the runtime list as map
    return yield select(selectors.getRuntimes);    
}

// load routes list
export function* loadRoutes(force) {
    const routes = yield select(selectors.getRoutes)
    if (force || !routes || routes.length < 1) {
        yield call(fetchRoutes,'Routes loading')
    }
    // Returns the route list as map
    return yield select(selectors.getRoutes);    
}

// load functions list
export function* loadFunctions(force) {
    const functions = yield select(selectors.getFunctions)
    if (force || !functions || functions.length < 1) {
        yield call(fetchFunctions,'Functions loading')
    }
    // Returns the runtime list as map
    return yield select(selectors.getFunctions);    
}


export {
    requestData,
    selectObr,
    pollStartRuntimes,
    pollStopRuntimes,
    pollStartRoutes,
    pollStopRoutes,
    pollStartFunctions,
    pollStopFunctions
}
