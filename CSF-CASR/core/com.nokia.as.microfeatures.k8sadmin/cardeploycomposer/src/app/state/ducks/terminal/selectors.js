import { utils } from "../../../utilities"
const moduleKey = 'terminal'

export const isTerminalOpen = state => state.open
export const terminalUrl = state => state.url
export const terminalId = state => state.id
export const terminalPod = state => state.pod

export default utils.globalizeSelectors({
    isTerminalOpen,
    terminalUrl,
    terminalId,
    terminalPod
}, moduleKey)
