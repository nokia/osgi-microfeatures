//import { take, put, call, fork, select, all, takeEvery } from 'redux-saga/effects'
import {
  put,
  call,
  select,
  takeEvery,
  all,
  take,
  race
} from "redux-saga/effects";
import { delay } from "redux-saga";

import { decodeFeatureId } from '../utilities/utils'

import { userTypes, userOperations } from '../state/ducks/user'

import { viewsConst, viewsTypes, viewsSelectors } from "../state/ducks/views";
import {
  entitiesTypes,
  entitiesOperations,
  entitiesSelectors
} from "../state/ducks/entities";
import { userMsgSelectors, userMsgActions } from "../state/ducks/userMsg";
import { indicatorsActions } from "../state/ducks/indicators";
import {
  runtimeTypes,
  runtimeOperations,
  runtimeSelectors
} from "../state/ducks/runtime";
import { terminalTypes, terminalActions } from "../state/ducks/terminal";

import {
  routeTypes,
  routeOperations,
  routeSelectors
} from "../state/ducks/route";

import {  functionTypes, functionOperations, functionSelectors } from '../state/ducks/function'

import {
  bundlesTypes,
  bundlesOperations,
  bundlesSelectors
} from "../state/ducks/bundles";

import Cookies from "universal-cookie";
const cookies = new Cookies();

//  ************ WATCHERS ***************
//
//  Fetch data to get :
// 1- the list of all available urls of obrs
// 2-
//
//
export function* loadData() {
  const { startLoading, stopLoading } = indicatorsActions;
  console.log("SAGA REQ_DATA");
  // Display the loading icon
  yield put(startLoading("Fetching user data..."));
  // Retrieve the user info data
  const userdata = yield call(userOperations.loadUserInfoData);
  console.log("userdata",userdata);
  let error = yield select(userMsgSelectors.getErrorMessage);
  if (error != null) {
    yield put(stopLoading());
    return;
  }

  yield put(startLoading("Fetching OBR list..."));
  // Retrieves the list of all available obrs ( local and remotes )
  const obrs = yield call(entitiesOperations.loadObrs);
  console.log("loadObrsList", obrs);

  error = yield select(userMsgSelectors.getErrorMessage);
  if (error != null) {
    yield put(stopLoading());
    return;
  }
  // Should have two obrs at least ( one local and one remote)
  console.log("loadData obrs", obrs);
  if (
    !obrs ||
    !obrs.local ||
    obrs.local.length < 1 ||
    !obrs.remotes ||
    obrs.remotes.length < 1
  ) {
    yield put(
      userMsgActions.setWarnMessage(
        "No available local/remote Obr at time!",
        "Please retry later!"
      )
    );
    yield put(stopLoading());
    return;
  }
  // Is there a selected obr? If yes, there is two obrs ( local & remote)
  let currentObrs = yield select(entitiesSelectors.getCurrentObrs);
  if (currentObrs.length < 2) {
    // No selection has been done yet.
    // Here, we can perform an automatic obr selection in case there is only one remote available obr
    const remoteObrs = yield select(entitiesSelectors.getRemoteObrs);
    if (obrs.length === 2) {
      yield put(entitiesOperations.selectObr(remoteObrs[0]));
    } else {
      // More than one remote obrs. See last use
      const lastObrVersion = remoteObrs[0];
      var prefObr = cookies.get("preferedObr");
      if (remoteObrs.indexOf(prefObr) === -1) {
        // Prefered obr not found => select the last version
        yield put(entitiesOperations.selectObr(lastObrVersion));
      } else {
        // Warns the user in case an old obr version is used.
        if (prefObr !== lastObrVersion) {
          yield put(
            userMsgActions.setInfoMessage(
              "Dashboard : Current OBR selection",
              "A more recent obr version is available."
            )
          );
        }
        yield put(entitiesOperations.selectObr(prefObr));
      }
    }
    return;
  }
  // Load others entities in according to the selected remote OBR
  loadObrEntities();
}

// Load others entities for a selected remote OBR
export function* loadObrEntities() {
  const { startLoading, stopLoading } = indicatorsActions;
  const currentObrs = yield select(entitiesSelectors.getCurrentObrs);
  console.log("loadObrEntities currentObrs", currentObrs);

  yield put(startLoading("Fetching entities..."));

  // Reload repositores held by the server
  yield call(entitiesOperations.reloadRepos, currentObrs);

  // Load the list of features.
  yield call(entitiesOperations.loadFeatures, currentObrs, true);

  let error = yield select(userMsgSelectors.getErrorMessage);
  if (error != null) {
    yield put(stopLoading());
    return;
  }

  // Update the runtime deploy requirements in according to the loaded features
  const requirements = yield select(runtimeSelectors.getDeployRequirements);
  const { fnames } = requirements;
  const featuresIds = yield select(entitiesSelectors.getFeaturesIdsByNames,fnames );
  const found = (fnames.length === featuresIds.length)?true:false;
  const payload = {featuresIds: featuresIds, found: found};
  yield put(runtimeOperations.setDeployRequirements(payload));

  // Load list of runtimes
  yield call(entitiesOperations.loadRuntimes, true);

  // Load list of routes
  yield call(entitiesOperations.loadRoutes, true);

  // Load list of functions
  yield call(entitiesOperations.loadFunctions, true);

  yield put(stopLoading());
}

/**
 * Worker to poll runtime data
 */
function* pollRuntimes() {
  while (true) {
    yield call(entitiesOperations.loadRuntimes, true);
    yield call(delay, 4000);
  }
}

function* watchPollRuntimes() {
  while (true) {
    yield take(entitiesTypes.POLL_START_RUNTIMES);
    yield race([call(pollRuntimes), take(entitiesTypes.POLL_STOP_RUNTIMES)]);
  }
}

//
// Applies the selected option of the Quick selector in features view
//
function* applySelectedQuickOption() {
  console.log("applySelectedQuickOption");
  yield put(runtimeOperations.resetDeploy());

  const quickOption = yield select(runtimeSelectors.getSelectedQuick);
  let featuresIds = [];
  let runtimeFeatureId = null;
  switch (quickOption) {
    case "NONE":
      break;

    case "ALL":
      const allfeatures = yield select(entitiesSelectors.getAllFeatures);
      console.log("applySelectedQuickOption features", allfeatures);
      // Get all features ids that will be automaticcaly added on deployment
      const reqFIds = yield select(runtimeSelectors.getRequiredFeatureIds);

      // Returns the list of features ids which are not runtimes and not added automaticcaly on deploy
      featuresIds = allfeatures.filter(f => f.categories[0] !== 'runtime' && reqFIds.includes(f.fid) !== true).map(f => f.fid);
      break;

    default:
      // its value is a runtime identifier
      const runtime = yield select(
        entitiesSelectors.getRuntimeById,
        quickOption
      );
      // Remove useless data
      const { status, url, fid, state, features, ...rest } = runtime;
      console.log("applySelectedQuickOption runtime", runtime);
      const runtimeForm = rest;

      const { fids, rid } = yield select(
        entitiesSelectors.extractRuntimeFeaturesIds,
        quickOption
      );
      // Remove features considered as requirements
      const requireds = yield select(runtimeSelectors.getRequiredFeatureNames);
      featuresIds = fids.filter(fid => requireds.includes(decodeFeatureId(fid).name ) === false );

      runtimeFeatureId = rid;

      // Preset fields used to deploy a runtime in according to the selected quick option
      console.log("PRESET RuntimeForm", runtimeForm);
      yield put(runtimeOperations.presetDeploy(runtimeForm));
  }

  // Update the list of features identifiers
  console.log("applySelectedQuickOption set featuresIds/runtimeFeatureId", featuresIds,runtimeFeatureId);
  yield put(
    runtimeOperations.setSelectedFeatures(featuresIds, runtimeFeatureId)
  );
}


/**
 * Worker to poll route data
 */
function* pollRoutes() {
  while (true) {
    yield call(entitiesOperations.loadRoutes, true);
    yield call(delay, 4000);
  }
}

function* watchPollRoutes() {
  while (true) {
    yield take(entitiesTypes.POLL_START_ROUTES);
    yield race([call(pollRoutes), take(entitiesTypes.POLL_STOP_ROUTES)]);
  }
}


//
// Applies the selected option of the Quick selector in routes view
//
function* applySelectedQuickRouteOption() {
  console.log("applySelectedQuickRouteOption");
  yield put(routeOperations.resetRoute());

  const quickOption = yield select(routeSelectors.getSelectedQuick);
  switch (quickOption) {
    case "NONE":
      break;


    default:
      // its value is a route identifier
      const route = yield select(
        entitiesSelectors.getRouteById,
        quickOption
      );
      // Remove useless data
      const { status, fid, state, ...rest } = route;
      console.log("applySelectedQuickOption route", route);
      const routeForm = rest;

      // Preset fields used to add a route in according to the selected quick option
      console.log("PRESET routeForm", routeForm);
      yield put(routeOperations.presetRoute(routeForm));
  }
}

/**
 * Worker to poll function data
 */
function* pollFunctions() {
  while (true) {
    yield call(entitiesOperations.loadFunctions, true);
    yield call(delay, 4000);
  }
}

function* watchPollFunctions() {
  while (true) {
    yield take(entitiesTypes.POLL_START_FUNCTIONS);
    yield race([call(pollFunctions), take(entitiesTypes.POLL_STOP_FUNCTIONS)]);
  }
}


//
// Applies the selected option of the Quick selector in functions view
//
function* applySelectedQuickFunctionOption() {
  console.log("applySelectedQuickFunctionOption");
  yield put(functionOperations.resetFunction());

  const quickOption = yield select(functionSelectors.getSelectedQuick);
  switch (quickOption) {
    case "NONE":
      break;


    default:
      // its value is a function identifier
      const funcTion = yield select(
        entitiesSelectors.getFunctionById,
        quickOption
      );
      // Remove useless data
      const { status, fid, state, ...rest } = funcTion;
      console.log("applySelectedQuickOption function", funcTion);
      const functionForm = rest;

      // Preset fields used to add a function in according to the selected quick option
      console.log("PRESET functionForm", functionForm);
      yield put(functionOperations.presetFunction(functionForm));
  }
}


function* loadBundles() {
  const { startLoading, stopLoading } = indicatorsActions
  console.log("loadBundles LOAD_BUNDLES");
  yield put(startLoading("Loading Current OBR..."))
  // Load current bundles ( i.e reads the current obr )
  const currentUrl = yield select(entitiesSelectors.getSelectedObr);
  console.log("loadBundles currentUrl", currentUrl);
  yield call(bundlesOperations.doLoadBundles, currentUrl, false);
  yield put(stopLoading());
}

function* resolveBundle() {
  console.log("resolveBundle RESOLVE_BUNDLE");
  yield put(
    userMsgActions.setInfoMessage(
      "Bundle resolution in progress",
      "Please wait..."
    )
  );
  const bundle = yield select(bundlesSelectors.getBundleForOp);
  const obrs = yield select(entitiesSelectors.getCurrentObrs);
  console.log("resolveBundle bundle", bundle, obrs);
  yield call(bundlesOperations.doResolveBundle, bundle, obrs);
  let error = yield select(userMsgSelectors.getErrorMessage);
  if (error === null) yield put(userMsgActions.resetUserMessage());
}

function* findDependents() {
  console.log("findDependents FIND_DEPS_BUNDLE");
  yield put(
    userMsgActions.setInfoMessage(
      "Bundle dependencies calculation in progress",
      "Please wait..."
    )
  );
  const bundle = yield select(bundlesSelectors.getBundleForOp);
  const obrs = yield select(entitiesSelectors.getCurrentObrs);
  console.log("findDependents bundle", bundle, obrs);
  yield call(bundlesOperations.doFindDependentsBundle, bundle, obrs);
  let error = yield select(userMsgSelectors.getErrorMessage);
  if (error === null) yield put(userMsgActions.resetUserMessage());
}

// Load entities in according to the selected view
function* loadViewData() {
  const selectedView = yield select(viewsSelectors.getActiveView);

  // stop runtime polling
  const ispolling = yield select(entitiesSelectors.isPollingRuntime);
  if (ispolling === true) yield put(entitiesOperations.pollStopRuntimes());

  // stop route polling
  const ispollingroutes = yield select(entitiesSelectors.isPollingRoute);
  if (ispollingroutes === true) yield put(entitiesOperations.pollStopRoutes());

  // stop function polling
  const ispollingfunctions = yield select(entitiesSelectors.isPollingFunction);
  if (ispollingfunctions === true) yield put(entitiesOperations.pollStopFunctions());


  if (selectedView === viewsConst.views.RUNTIMES) {
    yield put(entitiesOperations.pollStartRuntimes());
    return;
  }

  if (selectedView === viewsConst.views.ROUTES) {
    yield put(entitiesOperations.pollStartRoutes());
    return;
  }

  if (selectedView === viewsConst.views.FUNCTIONS) {
    yield put(entitiesOperations.pollStartFunctions());
    return;
  }


  if (selectedView === viewsConst.views.OBRS) {
    yield put(bundlesOperations.loadBundles());
  }


}

//
// Deploy runtime : Post data
//
function* deployRuntime() {
  console.log("watchDeployRuntime");
  // build the whole configuration to deploy the runtime
  const form = yield select(runtimeSelectors.getDeployForm);
  const featuresIds = yield select(runtimeSelectors.getFeaturesIds);
  const runtimeFeatureId = yield select(runtimeSelectors.getRuntimeFeatureId);
  const reqFeatureIds = yield select(runtimeSelectors.getRequiredFeatureIds);

  // Remove useless properties
  const { open, state, completes, form1complete, form2complete, form3complete, form4complete, ...fconfig } = form;
  // Concat all runtimes features
  const features = [].concat([runtimeFeatureId]).concat(reqFeatureIds).concat(featuresIds);

  
  const config = { ...fconfig, features: features };

  console.log("watchDeployRuntime ", config);
  // Post the request to deploy features
  let result = yield call(runtimeOperations.doDeployRuntime, config);
  console.log("watchDeployRuntime result", result);
  if (result.error === null) {
    yield put(runtimeOperations.fullResetDeploy());
    // Refresh runtimes list
    yield call(entitiesOperations.loadRuntimes, true);
    return;
  }
}


//
// Undeploy Runtime : DELETE data
//
function* undeployRuntime() {
  const payload = yield select(runtimeSelectors.getUndeployPayload);
  console.log("undeploy runtime payload", payload);
  yield call(runtimeOperations.doUndeployRuntime, payload);
  yield call(entitiesOperations.loadRuntimes, true);
}

//
// Start Terminal for a deployed runtime
//
function* doStartTerminal() {
  yield put(terminalActions.openTerminal());
}

//
// Add route : Post data
//
function* addRoute() {
  console.log("watchAddRoute");
  // build the whole configuration to deploy the route
  const form = yield select(routeSelectors.getRouteForm);

  // Remove useless properties
  const { open, state, completes, ...fconfig } = form;  
  const config = { ...fconfig };

  console.log("watchAddRoute ", config);
  // Post the request to deploy features
  let result = yield call(routeOperations.doAddRoute, config);
  console.log("watchAddRoute result", result);
  if (result.error === null) {
    yield put(routeOperations.fullResetRoute());
    // Refresh runtimes list
    yield call(entitiesOperations.loadRoutes, true);
    return;
  }
}

//
// Delete Route : DELETE data
//
function* deleteRoute() {
  const payload = yield select(routeSelectors.getDeletePayload);
  console.log("delete route payload", payload);
  yield call(routeOperations.doDeleteRoute, payload);
  yield call(entitiesOperations.loadRoutes, true);
}

//
// Add function : Post data
//
function* addFunction() {
  console.log("watchAddFunction");
  // build the whole configuration to deploy the function
  const form = yield select(functionSelectors.getFunctionForm);

  // Remove useless properties
  const { open, state, completes, ...fconfig } = form;  
  const config = { ...fconfig };

  console.log("watchAddFunction ", config);
  // Post the request to deploy features
  let result = yield call(functionOperations.doAddFunction, config);
  console.log("watchAddFunction result", result);
  if (result.error === null) {
    yield put(functionOperations.fullResetFunction());
    // Refresh runtimes list
    yield call(entitiesOperations.loadFunctions, true);
    return;
  }
}

//
// Delete Function : DELETE data
//
function* deleteFunction() {
  const payload = yield select(functionSelectors.getDeletePayload);
  console.log("delete function payload", payload);
  yield call(functionOperations.doDeleteFunction, payload);
  yield call(entitiesOperations.loadFunctions, true);
}




export default function* root() {
  yield all([
    takeEvery(userTypes.LOGOUT, userOperations.doDisconnect),
    takeEvery(entitiesTypes.REQ_DATA, loadData),
    takeEvery(entitiesTypes.SELECT_OBR, loadObrEntities),
    takeEvery(runtimeTypes.SET_SELECTED_QUICK, applySelectedQuickOption),
    takeEvery(terminalTypes.START_TERMINAL, doStartTerminal),
    takeEvery(viewsTypes.SET_VIEW, loadViewData),
    takeEvery(bundlesTypes.LOAD_BUNDLES, loadBundles),
    takeEvery(bundlesTypes.RESOLVE_BUNDLE, resolveBundle),
    takeEvery(bundlesTypes.FIND_DEPS_BUNDLE, findDependents),
    takeEvery(runtimeTypes.DEPLOY_RUNTIME, deployRuntime),
    takeEvery(runtimeTypes.CONFIRM_UNDEPLOY_RUNTIME, undeployRuntime),
    takeEvery(routeTypes.SET_SELECTED_QUICKROUTE, applySelectedQuickRouteOption),
    takeEvery(routeTypes.ADD_ROUTE, addRoute ),
    takeEvery(routeTypes.CONFIRM_DELETE_ROUTE, deleteRoute ),
    takeEvery(functionTypes.SET_SELECTED_QUICKFUNCTION, applySelectedQuickFunctionOption),
    takeEvery(functionTypes.ADD_FUNCTION, addFunction ),
    takeEvery(functionTypes.CONFIRM_DELETE_FUNCTION, deleteFunction ),
    call(watchPollRuntimes),
    call(watchPollRoutes),
    call(watchPollFunctions)
  ]);
  /*
        yield all([
            fork(watchLoadData)
        ])
    */
}
