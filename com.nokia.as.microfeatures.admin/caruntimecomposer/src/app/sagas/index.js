
//import { take, put, call, fork, select, all, takeEvery } from 'redux-saga/effects'
import { put, call, select, takeEvery, all } from 'redux-saga/effects'

import { viewsConst, viewsTypes, viewsSelectors}  from '../state/ducks/views'
import { entitiesTypes, entitiesOperations, entitiesSelectors } from '../state/ducks/entities'
import { userMsgSelectors, userMsgActions } from '../state/ducks/userMsg'
import { indicatorsActions } from '../state/ducks/indicators'
import { assemblyTypes, assemblyOperations, assemblySelectors } from '../state/ducks/assembly'
import { snapshotTypes, snapshotOperations, snapshotSelectors } from '../state/ducks/snapshot'
import { bundlesTypes , bundlesOperations, bundlesSelectors } from '../state/ducks/bundles'

import Cookies from 'universal-cookie';
const cookies = new Cookies();

//  ************ WATCHERS ***************
// 
//  Fetch data to get :
// 1- the list of all available urls of obrs
// 2- 
// 
//
export function* loadData() {
    const { startLoading, stopLoading } = indicatorsActions
    console.log("SAGA REQ_DATA");
    // Display the loading icon
    yield put(startLoading("Fetching OBR list..."))
    // Retrieves the list of all available obrs ( local and remotes )
    const obrs = yield call(entitiesOperations.loadObrs)
    console.log("loadObrsList", obrs)

    let error = yield select(userMsgSelectors.getErrorMessage)
    if (error != null) {
        yield put(stopLoading())
        return;
    }
    // Should have two obrs at least ( one local and one remote)
    console.log("loadData obrs",obrs )
    if( !obrs || !obrs.local || obrs.local.length < 1 || !obrs.remotes || obrs.remotes.length < 1) {
        yield put( userMsgActions.setWarnMessage("No available local/remote Obr at time!","Please retry later!"))
        yield put(stopLoading())
        return;
    }
    // Is there a selected obr? If yes, there is two obrs ( local & remote)
    let currentObrs = yield select(entitiesSelectors.getCurrentObrs)
    if( currentObrs.length < 2 ) {
        // No selection has been done yet.
        // Here, we can perform an automatic obr selection in case there is only one remote available obr
        const remoteObrs = yield select(entitiesSelectors.getRemoteObrs)
        if( obrs.length === 2 ) {          
            yield put( entitiesOperations.selectObr(remoteObrs[0] ) )
        } else {
            // More than one remote obrs. See last use
            const lastObrVersion = remoteObrs[0]
            var prefObr = cookies.get('preferedObr');
            if( remoteObrs.indexOf(prefObr) === -1) {
                // Prefered obr not found => select the last version
                yield put( entitiesOperations.selectObr( lastObrVersion ) )
            } else {
                // Warns the user in case an old obr version is used.
                if( prefObr !== lastObrVersion) {
                    yield put( userMsgActions.setInfoMessage("Dashboard : Current OBR selection","A more recent obr version is available."))
                }
                yield put( entitiesOperations.selectObr(prefObr) )
            }
        }       
        return;
    } 
    // Load others entities in according to the selected remote OBR
    loadObrEntities()
}

// Load others entities for a selected remote OBR
export function* loadObrEntities() {
    const { startLoading, stopLoading, hideWelcome } = indicatorsActions
    const currentObrs = yield select(entitiesSelectors.getCurrentObrs)
    console.log("loadObrEntities currentObrs",currentObrs)

    yield put(startLoading("Fetching entities..."))

    // Reload repositores held by the server
    yield call(entitiesOperations.reloadRepos,currentObrs)

    // Load the list of features.
    yield call(entitiesOperations.loadFeatures, currentObrs, true)

    let error = yield select(userMsgSelectors.getErrorMessage)
    if (error != null) {
        yield put(stopLoading())
        return;
    }

    // load list of assemblies
    yield call(entitiesOperations.loadAssemblies, currentObrs, true)

    // load list od snapshots
    yield call(entitiesOperations.loadSnapshots, currentObrs, true)

    yield put(stopLoading())
    yield put(hideWelcome())
    
}

//
// Applies the selected option of the Quick selector in features view
//
function* applySelectedQuickOption() {
    console.log("applySelectedQuickOption");
    const quickOption = yield select(assemblySelectors.getSelectedQuick)
    const features = yield select(entitiesSelectors.getFeatures)
    let assemblyForm = { name: "", version: "", desc: "", doc:""}
    let featuresIds = []
    let runtimeId = null
    switch (quickOption) {
        case 'NONE':
            break;

        case 'ALL':
            console.log("applySelectedQuickOption features", features)
            featuresIds = Object.keys(features);
            break;

        default:
            // its value is an assembly identifier
            const assembly = yield select(entitiesSelectors.getAssemblyById,quickOption)
            assemblyForm = { name: assembly.bsn, version: assembly.version, desc: assembly.name, doc:assembly.url }

            const { fids, rid } = yield select(entitiesSelectors.extractAssemblyFeaturesIds, quickOption)
            featuresIds = fids
            runtimeId = rid
            console.log("applySelectedQuickOption ", quickOption, fids, rid)
    }
    // Preset fields used to create assembly in according to the selected quick option
    yield put(assemblyOperations.presetAssembly(assemblyForm))

    // Update the list of features identifiers
    yield put(assemblyOperations.setSelectedFeatures(featuresIds, runtimeId))   
}

//
// Create Assembly : Post data
//
function* createAssembly() {
    console.log("watchCreateAssembly");
    const payload = yield select(assemblySelectors.getForm)
    console.log("watchCreateAssembly ", payload)
    // Post the request to create the assembly
    let result = yield call(assemblyOperations.doCreateAssembly, payload)
    console.log("watchCreateAssembly result", result)
    if (result.error === null) {
        yield put(assemblyOperations.resetAssembly())
        // Refresh assemblies list
        const obrs = yield select(entitiesSelectors.getCurrentObrs)
        yield call(entitiesOperations.loadAssemblies, obrs, true)
        return;
    }
}

//
// Delete Assembly : DELETE data
//
function* deleteAssembly() {
    const payload = yield select(assemblySelectors.getDeletePayload)
    console.log("deleteAssembly payload", payload)
    yield call(assemblyOperations.doDeleteAssembly, payload)
    const obrs = yield select(entitiesSelectors.getCurrentObrs)
    yield call(entitiesOperations.loadAssemblies, obrs, true)

}

//
// Create snapshot
//
function* createSnapshot() {
    console.log("watchCreateSnapshot");
    const payload = yield select(snapshotSelectors.getPayload)
    console.log("watchCreateSnapshot ", payload)
    // Post the request to create the snapshot
    let result = yield call(snapshotOperations.doCreateSnapshot, payload)
    console.log("watchCreateSnapshot result", result)

    yield put(snapshotOperations.reset())
    // Refresh snapshots list
    const obrs = yield select(entitiesSelectors.getCurrentObrs)
    yield call(entitiesOperations.loadSnapshots, obrs, true)

}

//
// Delete Snapshot : DELETE data
//
function* deleteSnapshot() {
    const payload = yield select(snapshotSelectors.getPayload)
    console.log("deleteSnapshot payload", payload)
    yield call(snapshotOperations.doDeleteSnapshot, payload)
    // Refresh snapshots list
    const obrs = yield select(entitiesSelectors.getCurrentObrs)
    yield call(entitiesOperations.loadSnapshots, obrs, true)

}

function* loadBundles() {
    const { startLoading, stopLoading } = indicatorsActions
    console.log("loadBundles LOAD_BUNDLES")
    // First, load local bundles ( i.e reads the local obr)
    yield put(startLoading("Loading local OBR..."))
    const localUrls = yield select(entitiesSelectors.getLocalObrs)
    console.log("loadBundles localUrls",localUrls[0])
    yield call(bundlesOperations.doLoadBundles, localUrls[0],true)

    // Second, load current bundles ( i.e reads the current obr )
    yield put(startLoading("Loading Current OBR..."))
    const currentUrl = yield select(entitiesSelectors.getSelectedObr)
    console.log("loadBundles currentUrl",currentUrl)
    yield call(bundlesOperations.doLoadBundles, currentUrl,false)
    yield put(stopLoading());
}

function* resolveBundle() {
    console.log("resolveBundle RESOLVE_BUNDLE")
    yield put( userMsgActions.setInfoMessage("Bundle resolution in progress","Please wait..."))
    const bundle = yield select(bundlesSelectors.getBundleForOp)
    const obrs = yield select(entitiesSelectors.getCurrentObrs)
    console.log("resolveBundle bundle", bundle,obrs)
    yield call(bundlesOperations.doResolveBundle,bundle,obrs)
    let error = yield select(userMsgSelectors.getErrorMessage)
    if( error === null)
        yield put( userMsgActions.resetUserMessage() )
}

function* findDependents() {
    console.log("findDependents FIND_DEPS_BUNDLE")
    yield put( userMsgActions.setInfoMessage("Bundle dependencies calculation in progress","Please wait..."))
    const bundle = yield select(bundlesSelectors.getBundleForOp)
    const obrs = yield select(entitiesSelectors.getCurrentObrs)
    console.log("findDependents bundle", bundle,obrs)
    yield call(bundlesOperations.doFindDependentsBundle,bundle,obrs)
    let error = yield select(userMsgSelectors.getErrorMessage)
    if( error === null)
        yield put( userMsgActions.resetUserMessage() )
}

// Load entities in according to the selected view
function* loadViewData() {
    const selectedView = yield select(viewsSelectors.getActiveView)
    if( selectedView === viewsConst.views.SNAPSHOTS ) {
        const obrs = yield select(entitiesSelectors.getCurrentObrs)
        yield call(entitiesOperations.loadSnapshots, obrs, true)
        return;
    }
    if( selectedView === viewsConst.views.OBRS ) {
        yield put(bundlesOperations.loadBundles())
    }
}

export default function* root() {
    yield all([
        takeEvery(entitiesTypes.REQ_DATA, loadData),
        takeEvery(entitiesTypes.SELECT_OBR,loadObrEntities),
        takeEvery(assemblyTypes.SET_SELECTED_QUICK, applySelectedQuickOption),
        takeEvery(assemblyTypes.CREATE_ASSEMBLY, createAssembly),
        takeEvery(assemblyTypes.CONFIRM_DELETE_ASSEMBLY, deleteAssembly),
        takeEvery(snapshotTypes.CREATE_SNAPSHOT, createSnapshot),
        takeEvery(snapshotTypes.CONFIRM_DELETE_SNAPSHOT, deleteSnapshot),
 //       takeEvery(runtimeTypes.CREATE_RUNTIME, createRuntime),
        takeEvery(viewsTypes.SET_VIEW,loadViewData),
        takeEvery(bundlesTypes.LOAD_BUNDLES, loadBundles),
        takeEvery(bundlesTypes.RESOLVE_BUNDLE,resolveBundle),
        takeEvery(bundlesTypes.FIND_DEPS_BUNDLE,findDependents)
    ])
    /*
        yield all([
            fork(watchLoadData),
            fork(watchCreateAssembly)
        ])
    */
}