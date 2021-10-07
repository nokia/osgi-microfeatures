import { utils } from "../../../utilities"
const moduleKey = 'indicators'

export const isLoading = (state) => state.loading.isLoading
export const loadingMsg = (state) => state.loading.msg
export const isWelcome = (state) => state.loading.welcome

export default utils.globalizeSelectors({
    isLoading,
    isWelcome,
    loadingMsg
},moduleKey)