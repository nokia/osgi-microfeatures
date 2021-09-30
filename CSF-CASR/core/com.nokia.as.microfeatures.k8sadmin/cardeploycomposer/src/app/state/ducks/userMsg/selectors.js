export const getUserMessage = (state) => state.userMessage
export const getErrorMessage = (state) => (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null