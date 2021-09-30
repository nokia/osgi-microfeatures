import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'route'


export const getSelectedQuick = state => state.selectedQuick;
export const getDeletePayload = state => state.remove.payload;
export const doConfirmDeleteRoute = state => state.remove.confirm;
export const getDeleteStatus = state => state.remove.deleting;
export const getRouteForm = state => state.form;
export const getRouteFormErrors = state => state.formErrors;
export const getRouteRequestState = state => state.form.state;



export const canAddRoute = createSelector(
    getRouteRequestState,
    ( state ) => (state !== 'started')
)


export default utils.globalizeSelectors({
    canAddRoute,
    getSelectedQuick,
    doConfirmDeleteRoute,
    getDeletePayload,
    getDeleteStatus,
    getRouteForm,
    getRouteFormErrors,
    getRouteRequestState
},moduleKey)
