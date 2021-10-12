/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export const getUserMessage = (state) => state.userMessage
export const getErrorMessage = (state) => (state.userMessage && state.userMessage.type === 'error')?state.userMessage:null