import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'function'


export const getSelectedQuick = state => state.selectedQuick;
export const getDeletePayload = state => state.remove.payload;
export const doConfirmDeleteFunction = state => state.remove.confirm;
export const getDeleteStatus = state => state.remove.deleting;
export const getFunctionForm = state => state.form;
export const getFunctionFormErrors = state => state.formErrors;
export const getFunctionRequestState = state => state.form.state;



export const canAddFunction = createSelector(
    getFunctionRequestState,
    ( state ) => (state !== 'started')
)


export default utils.globalizeSelectors({
    canAddFunction,
    getSelectedQuick,
    doConfirmDeleteFunction,
    getDeletePayload,
    getDeleteStatus,
    getFunctionForm,
    getFunctionFormErrors,
    getFunctionRequestState
},moduleKey)
