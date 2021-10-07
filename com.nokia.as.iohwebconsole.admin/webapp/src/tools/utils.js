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
