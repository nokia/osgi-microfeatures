import { utils } from "../../../utilities"
const moduleKey = 'indicators'

export const isLoading = (state) => state.loading.isLoading
export const loadingMsg = (state) => state.loading.msg

export default utils.globalizeSelectors({
    isLoading,
    loadingMsg
},moduleKey)