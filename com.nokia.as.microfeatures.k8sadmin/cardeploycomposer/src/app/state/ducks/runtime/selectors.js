import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'runtime'


export const getRuntimeFeatureId = state => state.runtimeFeatureId;
export const getFeaturesIds = state => state.featuresIds;
export const getSelectedQuick = state => state.selectedQuick;
export const getUndeployPayload = state => state.undeploy.payload;
export const doConfirmUndeployRuntime = state => state.undeploy.confirm;
export const getUndeployStatus = state => state.undeploy.undeploying;
export const getDeployForm = state => state.form;
export const getDeployFormErrors = state => state.formErrors;
export const getDeployRequestState = state => state.form.state;

export const isDeployMetRequirements = state => state.requirements.found;
export const getDeployRequirements = state => state.requirements;
export const getRequiredFeatureNames = state => state.requirements.fnames;
export const getRequiredFeatureIds = state => state.requirements.featuresIds;

export const getRuntimeIdForDetails = state => state.details;

export const canDeployRuntime = createSelector(
    isDeployMetRequirements,
    getDeployRequestState,
    getFeaturesIds,
    getRuntimeFeatureId,
    ( met, state , selecteds , runtime ) => (met !== false && state !== 'started' && selecteds.length > 0 && runtime !== null )
)

export const hasExternalPort = createSelector(
    state => state.form.portslist,
    (ports) =>( ports.filter(p => p.external === true ).length > 0)
)


export default utils.globalizeSelectors({
    getRuntimeFeatureId,
    getFeaturesIds,
    canDeployRuntime,
    getSelectedQuick,
    doConfirmUndeployRuntime,
    getUndeployPayload,
    getUndeployStatus,
    getDeployForm,
    getDeployFormErrors,
    getDeployRequestState,
    getDeployRequirements,
    isDeployMetRequirements,
    getRequiredFeatureIds,
    getRequiredFeatureNames,
    getRuntimeIdForDetails,
    hasExternalPort
},moduleKey)
