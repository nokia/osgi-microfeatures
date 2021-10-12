/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
import { merge, uniq } from 'lodash'
const moduleKey = 'entities'

export const getObrs = (state) => ({ local: state.localobr, remotes: state.obrs })
export const getFeatures = (state) => state.features
export const getFeatureById = (state,id) => state.features[id]
export const getAssemblies = (state) => state.assemblies
export const getAssemblyById = (state,id) => state.assemblies[id]
export const getSnapshots = (state) => state.snapshots
export const getSnapshotById = (state,id) => state.snapshots[id]

export const getSelectedObr = (state) => state.selectedObr

// OBRS
export const getRemoteObrs = createSelector(
    getObrs,
    (allobrs) => ([ ...allobrs.remotes].sort(utils.naturalCompare).reverse() )
)
export const getLocalObrs = createSelector(
    getObrs,
    (allobrs) => ([ allobrs.local])
)
export const getCurrentObrs = createSelector(
    [getLocalObrs,getSelectedObr],
    (locals, selected) => {
        let currents = [...locals];
        if( selected )
        currents = currents.concat([selected])
        console.log("getCurrentObrs",locals,selected,currents)
        return currents
    }
)

//
// Entities
//

const comparator = (f1,f2) => ((f1.fid > f2.fid)?1:((f2.fid > f1.fid) ? -1 : 0))
export const getAllFeatures = createSelector(
    getFeatures,
    (featuresMap) => Object.values(featuresMap).sort(comparator)
)

//
// Assemblies
//
// Returns for a assembly, the list of features ids and runtime id
export const extractAssemblyFeaturesIds = (state,assemblyId) => {
    let extracted = { fids : [], rid : null }
    const fids = getAllAssemblyFeaturesIds(state,assemblyId)
//    console.log("extractAssemblyFeaturesIds",assemblyId,fids)
    for (let i = 0; i < fids.length; i++) {
        let f = getFeatureById(state,fids[i])
        // Feature cannot exist in case where this assembly has been created from another OBR
        if( f && f.categories[0] === 'runtime' )
            extracted.rid = fids[i]
        else
            extracted.fids.push(fids[i]) 
    }
    return extracted;
}

// Gets the list of all features identifier for an assembly identifier
const getAllAssemblyFeaturesIds = (state,assemblyId) => {
    const ids = []
    const assembly = getAssemblyById(state,assemblyId)
    if( assembly === undefined || assembly.asmbfeatures === undefined || assembly.asmbfeatures.length < 1 )
        return ids;
    return utils.getFeatureIdsFromLdapFilters(assembly.asmbfeatures)
}


// Returns an array of sorted features that contains an assembly
export const getAssemblyFeatures = (state,assemblyId) => {
    const { fids , rid } = extractAssemblyFeaturesIds(state,assemblyId);
    let featuresIds = merge([], fids );
    if( rid != null )
    featuresIds = featuresIds.concat([rid]).sort();
    const features = [];
    let feature = {}
    for(let i = 0; i < featuresIds.length ; i++) {
        feature = getFeatureById(state,featuresIds[i])
        // When the feature in not found in the current OBR, we create a fake feature to
        // warn the user !
        if( !feature ) {
            const { name , version } = utils.decodeFeatureId(featuresIds[i])
            feature = { 
                fid:featuresIds[i],
                name : name,
                categories : ["unknown"],
                bsn:'??????????',
                version:version,
                desc:'????',
                url:''
            }
        }
        features.push( Object.assign({},feature) )
    }
    return features;
}

export const getAllAssemblies = createSelector(
    getAssemblies,
    (assembliesMap) => Object.values(assembliesMap).sort(comparator)
)

export const getAllSnapshots = createSelector(
    getSnapshots,
    (snapshotsMap) => Object.values(snapshotsMap).sort(comparator)
)

// Returns the list of features ids which are used by assemblies
export const getUsedFeaturesIds = (state) => createSelector(
    getAssemblies,
    (assembliesMap) => {
        let fIds = []
        const aIds = Object.keys(assembliesMap)
        for(let i = 0; i < aIds.length ; i++ ) {
            const afids = getAllAssemblyFeaturesIds(state,aIds[i])
            fIds = fIds.concat(afids);
        }
        
        fIds = uniq(fIds)
        return fIds;
    }
)(state)

// Returns the nb of feeatures which are used by assemblies but not found in the current OBR
export const getFeaturesNotFound = createSelector(
    getFeatures,getUsedFeaturesIds,
    (featuresMap,useds) => {
        let fIds = Object.keys(featuresMap)
        const founds = useds.filter( id => fIds.indexOf(id) !== -1)
        return useds.length - founds.length;
    }
)


export default utils.globalizeSelectors({
    getObrs,
    getLocalObrs,
    getRemoteObrs,
    getSelectedObr,
    getCurrentObrs,
    getFeatures,
    getFeatureById,
    getAssemblies,
    getAssemblyById,
    getAllFeatures,
    getSnapshots,
    getSnapshotById,
    getAllSnapshots,
    extractAssemblyFeaturesIds,
    getAllAssemblies,
    getAssemblyFeatures,
    getUsedFeaturesIds,
    getFeaturesNotFound
},moduleKey)