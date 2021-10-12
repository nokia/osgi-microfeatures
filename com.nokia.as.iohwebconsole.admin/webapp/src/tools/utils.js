/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/* Utility class */

function action(type, payload = {}, id = {}) {
    console.log("utils action", type, payload, id)
    const result = { type, ...payload, ...id }
    console.log("utils action result", result)
    return result
}

export default {
    action
}
