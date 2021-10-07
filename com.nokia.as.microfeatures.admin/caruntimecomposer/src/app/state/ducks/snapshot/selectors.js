import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'snapshot'

export const getCreateStatus = state => state.create
export const getDeleteStatus = state => state.remove
export const shouldConfirmDelete = state => state.confirm

//export const getPayload = state => state.payload

export const getPayload = createSelector(
    state => state.name,
    state => state.bsn,
    state => state.version,
    ( name , bsn , version ) => ({ name:name, bsn:bsn, version:version})
)

export default utils.globalizeSelectors({
    getPayload,
    getCreateStatus,
    getDeleteStatus,
    shouldConfirmDelete
},moduleKey)