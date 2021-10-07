import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'assembly'


export const getRuntimeId = state => state.runtimeId;
export const getFeaturesIds = state => state.featuresIds;
export const getForm = state => state.form;
export const isPanelAssemblyOpen = state => state.form.open;
export const getRequestState = state => state.form.state;
export const getSelectedQuick = state => state.selectedQuick;
export const getDeletePayload = state => state.deletion.payload;
export const doConfirmDeleteAssembly = state => state.deletion.confirm;
export const getDeleteStatus = state => state.deletion.remove;

export const canCreateAssembly = createSelector(
    getRequestState,
    getFeaturesIds,
    getRuntimeId,
    ( state , selecteds , runtime ) => (state !== 'started' && selecteds.length > 0 && runtime !== null )
)


export default utils.globalizeSelectors({
    getForm,
    getRuntimeId,
    getFeaturesIds,
    canCreateAssembly,
    isPanelAssemblyOpen,
    getRequestState,
    getSelectedQuick,
    getDeletePayload,
    doConfirmDeleteAssembly,
    getDeleteStatus

},moduleKey)
