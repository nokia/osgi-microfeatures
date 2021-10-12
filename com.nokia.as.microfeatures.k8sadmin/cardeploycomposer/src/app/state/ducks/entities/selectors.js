/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { createSelector } from "reselect";
import { utils } from "../../../utilities";
import { merge, uniq } from "lodash";
const moduleKey = "entities";

export const getObrs = state => ({
  local: state.localobr,
  remotes: state.obrs
});
export const getFeatures = state => state.features;
export const getFeatureById = (state, id) => state.features[id];
export const getRuntimes = state => state.runtimes;
export const getRuntimeById = (state, id) => state.runtimes[id];
export const getRoutes = state => state.routes;
export const getRouteById = (state, id) => state.routes[id];
export const getFunctions = state => state.functions;
export const getFunctionById = (state, id) => state.functions[id];

export const getSelectedObr = state => state.selectedObr;

export const isPollingRuntime = state => state.pollingRuntimes;
export const isPollingRoute = state => state.pollingRoutes;
export const isPollingFunction = state => state.pollingFunctions;

// OBRS
export const getRemoteObrs = createSelector(
  getObrs,
  allobrs => [...allobrs.remotes].sort(utils.naturalCompare).reverse()
);
export const getLocalObrs = createSelector(
  getObrs,
  allobrs => [allobrs.local]
);
export const getCurrentObrs = createSelector(
  [getLocalObrs, getSelectedObr],
  (locals, selected) => {
    let currents = [...locals];
    if (selected) currents = currents.concat([selected]);
    console.log("getCurrentObrs", locals, selected, currents);
    return currents;
  }
);

//
// Entities
//

const comparator = (f1, f2) => (f1.fid > f2.fid ? 1 : f2.fid > f1.fid ? -1 : 0);
export const getAllFeatures = createSelector(
  getFeatures,
  featuresMap => Object.values(featuresMap).sort(comparator)
);

//
// Runtimes
//
export const getAllRuntimes = createSelector(
  getRuntimes,
  runtimesMap => Object.values(runtimesMap).sort(comparator)
);

export const getRuntimeIds = createSelector(
  getRuntimes,
  runtimesMap => Object.keys(runtimesMap).sort()
);

// Returns an array of sorted features that contains a runtime
export const getRuntimeFeatures = (state, runtimeId) => {
  const { fids, rid } = extractRuntimeFeaturesIds(state, runtimeId);
  let featuresIds = merge([], fids);
  if (rid != null) featuresIds = featuresIds.concat([rid]).sort();
  const features = [];
  let feature = {};
  for (let i = 0; i < featuresIds.length; i++) {
    feature = getFeatureById(state, featuresIds[i]);
    // When the feature in not found in the current OBR, we create a fake feature to
    // warn the user !
    if (!feature) {
      const { name, version } = utils.decodeFeatureId(featuresIds[i]);
      feature = {
        fid: featuresIds[i],
        name: name,
        categories: ["unknown"],
        bsn: "??????????",
        version: version,
        desc: "????",
        url: ""
      };
    }
    features.push(Object.assign({}, feature));
  }
  return features;
};

// Returns for a runtime, the list of features ids and runtime id
export const extractRuntimeFeaturesIds = (state, runtimeId) => {
  let extracted = { fids: [], rid: null };
  const fids = getAllRuntimeFeaturesIds(state, runtimeId);
  //    console.log("extractRuntimeFeaturesIds",runtimeId,fids)
  for (let i = 0; i < fids.length; i++) {
    let f = getFeatureById(state, fids[i]);
    // Feature cannot exist in case where this assembly has been created from another OBR
    if (f && f.categories[0] === "runtime") extracted.rid = fids[i];
    else extracted.fids.push(fids[i]);
  }
  return extracted;
};

// Gets the list of all features identifier for an runtime identifier
const getAllRuntimeFeaturesIds = (state, runtimeId) => {
  const ids = [];
  const runtime = getRuntimeById(state, runtimeId);
  if (
    runtime === undefined ||
    runtime.features === undefined ||
    runtime.features.length < 1
  )
    return ids;
  return runtime.features;
};

// Gets features ids by names
export const getFeaturesIdsByNames = (state, names) => {
  const allfids = Object.keys(getFeatures(state));
  let fids = [];
  names.forEach(name => {
    fids = fids.concat(getFeatureIdByName(allfids, name));
  });
  console.log("getFeaturesIdsByNames return", fids);
  return fids;
};

const getFeatureIdByName = (featuresIds, name) => {
  const ids = featuresIds.filter(fid => {
    const idName = fid.match(/(.*)@/);
    //        console.log("getFeatureIdByName",idName[1]);
    return idName[1] === name ? fid : false;
  });
  if (ids.length > 1) {
    console.error(
      `Mandatory feature ${name} have more than one corresponding versions in the same obr!`
    );
  }
  console.log("getFeatureIdByName return", ids);
  return ids;
};

//
// Routes
//
export const getAllRoutes = createSelector(
  getRoutes,
  routesMap => Object.values(routesMap).sort(comparator)
);

export const getRouteIds = createSelector(
  getRoutes,
  routesMap => Object.keys(routesMap)
);

//
// Functions
//
export const getAllFunctions = createSelector(
  getFunctions,
  functionsMap => Object.values(functionsMap).sort(comparator)
);

export const getFunctionIds = createSelector(
  getFunctions,
  functionsMap => Object.keys(functionsMap).sort()
);

//
// Selectors for dashboard statistics against route consistency
//

/*
 * Returns an object with the following attributes:
 * brokenRoutes : Nb of routes with broken reference ( function / runtime )
 * unknownFunctions: Nb of non existing function found in routes
 * unknownRuntimes: Nb of non existing runtime found in routes
 * runtimesWithBrokenRoute: Nb of runtimes injected with inconsistent routes 
 */
export const getDataConsistency = createSelector(
  [getAllRoutes, getFunctionIds, getRuntimeIds],
  (routes, functionIds, runtimeIds) => {
    const consistency = {
      brokenRoutes: 0,
      unknownFunctions: 0,
      unknownRuntimes: 0,
      runtimesWithBrokenRoute : 0
    };

    let missFuncs = [];
    let missRuntime = [];

    routes.forEach(route => {
      const { functionId, runtimesList } = route;
      let broken = false;
      if (functionIds.includes(functionId) !== true) {
        missFuncs.push(functionIds);
        broken = true;
      }
      if (runtimesList.length > 0) {
        const runtimeId = runtimesList[0].name;
        if (runtimeIds.includes(runtimeId) !== true) {
          // The route points on an unexisting runtime
          missRuntime.push(runtimeId);
          broken = true;
        } else {
            // Route will be injects on this runtime, check if the route is consistent
            if( broken === true )
                consistency.runtimesWithBrokenRoute += 1;
        }
      }
      if (broken === true) consistency.brokenRoutes += 1;
    });

    consistency.unknownFunctions = uniq(missFuncs).length;
    consistency.unknownRuntimes = uniq(missRuntime).length;

    return consistency;
  }
);

export default utils.globalizeSelectors(
  {
    getObrs,
    getLocalObrs,
    getRemoteObrs,
    getSelectedObr,
    getCurrentObrs,
    getFeatures,
    getAllFeatures,
    getFeatureById,
    getRuntimes,
    getAllRuntimes,
    getRuntimeIds,
    extractRuntimeFeaturesIds,
    getRuntimeById,
    getRuntimeFeatures,
    getRoutes,
    getAllRoutes,
    getRouteById,
    getRouteIds,
    getFunctions,
    getFunctionById,
    getAllFunctions,
    getFunctionIds,
    isPollingRuntime,
    isPollingRoute,
    isPollingFunction,
    getFeaturesIdsByNames,
    getDataConsistency
  },
  moduleKey
);
