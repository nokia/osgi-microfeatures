/* eslint-disable no-constant-condition */
import { call, select } from 'redux-saga/effects'
import { api } from '../../../services'
import { requestData, obrs, features, assemblies, snapshots, reloadrepos, selectObr } from './actions'
import { utils } from "../../../utilities"
import { default as selectors } from './selectors'

const { fetchEntity } = utils;

const fetchObrs = fetchEntity.bind(null, obrs, api.fetchObrs)
const fetchFeatures = fetchEntity.bind(null, features, api.fetchFeatures)
const fetchAssemblies = fetchEntity.bind(null, assemblies, api.fetchAssemblies)
const fetchSnapshots = fetchEntity.bind(null, snapshots, api.fetchSnapshots)
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
    yield call(postReloadRepos, 'Repository reload', '/cmd/repos/reload', { obrs:`${obrs}`})
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

// Load assemblies list in according to the obr list
export function* loadAssemblies(obrsList, force) {
    const params = obrsList.toString();
    //    console.log("loadAssembliesList",obrsList,force,params);

    const assemblies = yield select(selectors.getAssemblies)
    if (force || !assemblies || assemblies.length < 1) {
        yield call(fetchAssemblies,'Assemblies loading', params)
    }
    // Returns the assemblies list as map
    return yield select(selectors.getAssemblies);
}

// Load snapshots list in according to the obr list
export function* loadSnapshots(obrsList, force) {
    const params = obrsList.toString();
    //    console.log("loadAssembliesList",obrsList,force,params);

    const snapshots = yield select(selectors.getSnapshots)
    if (force || !snapshots || snapshots.length < 1) {
        yield call(fetchSnapshots,'Snapshots loading', params)
    }
    // Returns the assemblies list as map
    return yield select(selectors.getSnapshots);
}


export {
    requestData,
    selectObr
}
