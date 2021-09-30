import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'runtime'

export const getCreateStatus = state => state.create
export const getLayout = state => state.layout
export const isOpenLegacyPanel = state => state.legacyPanel
export const getPgci = state => state.pgci

export const getPayload = createSelector(
    state => state.name,
    state => state.bsn,
    state => state.version,
    state => state.pgci,
    ( name , bsn , version , pgci ) => {
        if( pgci === null)
            return ({ name:name, bsn:bsn, version:version})
        // Legacy payload   
        return ({ name:name, bsn:bsn, version:version, p:pgci.p, g:pgci.g, c:pgci.c, i:pgci.i})        
    }
)

export default utils.globalizeSelectors({
    getPayload,
    getCreateStatus,
    getLayout,
    isOpenLegacyPanel,
    getPgci
},moduleKey)