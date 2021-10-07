import axios from 'axios'; //We use axios for making a http request to the server
import { schema, normalize } from 'normalizr'
import { utils } from "../utilities"
//import querystring from 'querystring'

const HTTP_METHOD = {
    GET : 'get',
    DELETE : 'delete',
    HEAD : 'head',
    POST : 'post',
    PUT : 'put',
    PATCH : 'patch'
}
//export const API_ROOT = 'http://localhost:9090/'
/**
 * METHOD USED FOR JSON RESPONSE
 * @param {*} endpoint Mandatory url
 * @param {*} method Mandatory http method ( 'get' , 'post' , 'delete' )
 * @param {*} schema Optional schema for normalization
 * @param {*} payload Optional payload data
 */
function callApi(endpoint, method, schema, payload) {
    const buildError = (data) => {
        let msg = 'Unknown error'
        if( data.status )
            msg = 'Error '+ data.status+ ': ';
        if( data.statusText)
            msg += data.statusText+ '. ';
        if( data.data && data.data.error) {
            msg = msg + '\n' + data.data.error;
        }
        console.warn("callApi buildError",msg)
        return msg;
    }
 
//    console.log('callApi', endpoint);
//    const fullUrl = (!endpoint.startsWith(API_ROOT)) ? API_ROOT + endpoint : endpoint
    const fullUrl = endpoint;
    console.log('fullUrl', fullUrl);

    let config = {
        url: fullUrl,
        timeout: 40000,
        method: method,
        responseType: 'json'
    }
    if (payload) {
        Object.assign(config, { data: payload });
    }
    console.log("axios.config", config);
    return axios(config)
        .then(function (response) {
            const json = response.data
            console.log("AXIOS RESPONSE JSON",json)
            if (!schema) {
                // Without schema, we have to wrap json under 'entities'
                // to be compliant
                return Object.assign({}, { entities: json });
            }
            const camelizedJson = json; // camelizeKeys(json)
            console.log("axios before normalize", camelizedJson);
            const ret = Object.assign({},
                normalize(camelizedJson, schema)
            )
            console.log("callApi fetch json", json, ret);
            return ret;
        })
        .then(
        //      response => ({response}),
        response => {
                       console.log("callApi A response", response, { response });
            return { response };
        },
        error => {
            console.log("callApi B error", error);
            let errordata = "";
            if (error.response) {
                console.log("error.response=",error.response);
                errordata = buildError(error.response);
            } else if (error.request) {
                console.log("error.request=",error.request);
                errordata = buildError(error.request);
            } else {
                console.log("error.message=",error.message);
                errordata = error.message;
            }
          console.log("response error", errordata);
            return ({ error: errordata || 'Something bad happened' });
        })
}

// We use this Normalizr schemas to transform API responses from a nested form
// to a flat form where obrs,features and asemblies are placed in `entities`, and nested
// JSON objects are replaced with their IDs. This is very convenient for
// consumption by reducers, because we can easily build a normalized tree
// and keep it updated as we fetch more data.

// Read more about Normalizr: https://github.com/gaearon/normalizr

// Schemas for features list
// The following code allows to transform the array of features to a map where keys are generated
// during the normalization process.

// Allows to build for each feature item ( item mutation ):
// -an identifier : fid based on item name and version
// -a flag : selected to false
const generateExtraFeatureData = value => {
    if (!Object.prototype.hasOwnProperty.call(value, 'fid')) {
        value.fid = value.name + '@' + value.version;
        value.selected = false;
    }
    return {...value};
}
const featuresEntity = new schema.Entity('features', undefined, {
    processStrategy: generateExtraFeatureData,
    idAttribute: 'fid'
})

// Schemas for runtimes list
// The following code allows to transform the array of runtimes to a map where keys are generated
// during the normalization process.

// Allows to build for each runtime item ( item mutation ):
// -an identifier : fid based on item name and version
const generateExtraruntimeData = value => {
    if (!Object.prototype.hasOwnProperty.call(value, 'fid')) {
        value.fid = value.name + '@' + value.namespace;
    }
    return {...value};
}

const runtimesEntity = new schema.Entity('runtimes', undefined, {
    processStrategy: generateExtraruntimeData,
    idAttribute: 'fid'
})

// Schemas for routes list
// The following code allows to transform the array of routes to a map where keys are generated
// during the normalization process.

// Allows to build for each route item ( item mutation ):
// -an identifier : fid based on item name
const generateExtrarouteData = value => {
    if (!Object.prototype.hasOwnProperty.call(value, 'fid')) {
        value.fid = value.name;
    }
    return {...value};
}

const routesEntity = new schema.Entity('routes', undefined, {
    processStrategy: generateExtrarouteData,
    idAttribute: 'fid'
})

// Schemas for functions list
// The following code allows to transform the array of functions to a map where keys are generated
// during the normalization process.

// Allows to build for each function item ( item mutation ):
// -an identifier : fid based on item name
const generateExtrafunctionData = value => {
    if (!Object.prototype.hasOwnProperty.call(value, 'fid')) {
        value.fid = value.name;
    }
    return {...value};
}

const functionsEntity = new schema.Entity('functions', undefined, {
    processStrategy: generateExtrafunctionData,
    idAttribute: 'fid'
})

// api services
export const fetchUserInfoData = () => callApi('../cmd/sessioninfo', HTTP_METHOD.GET)
export const fetchDisconnectData = () => callApi('../cmd/disconnect', HTTP_METHOD.GET)
export const fetchObrs = () => callApi('../cmd/default/obr/list', HTTP_METHOD.GET)
export const fetchFeatures = params => callApi(`../cmd/list/feature?obrs=${params}`, HTTP_METHOD.GET, { features: [featuresEntity]})
export const fetchRuntimes = params => callApi(`../cmd/deployeds`, HTTP_METHOD.GET, { runtimes: [runtimesEntity]})
export const fetchRoutes = params => callApi(`../cmd/routes`, HTTP_METHOD.GET, { routes: [routesEntity]})
export const fetchFunctions = params => callApi(`../cmd/functions`, HTTP_METHOD.GET, { functions: [functionsEntity]})

export const postRequest = (url, payload) => callApi(url,HTTP_METHOD.POST, false, payload)
export const deleteRequest = (url, payload) => callApi(url,HTTP_METHOD.DELETE, false, payload)

export const fetchResolveds = (bsn,version,obrs) => callApi(`../cmd/bundle/${bsn}/${version}/resolution?all=1&obrs=${obrs}`,HTTP_METHOD.GET)
export const fetchDependents = (bundle,obrs) => callApi(`../cmd/bundle/${bundle.bsn}/${bundle.version}/dependent?all=1&obrs=${obrs}`,HTTP_METHOD.GET)

/**
 * METHOD USED FOR TEXT RESPONSE
 * @param {*} endpoint Mandatory url
 * @param {*} method Mandatory http method ( 'get' , 'post' , 'delete' )
 * @param {*} payload Optional payload data
 */
function callApiText(endpoint, method, payload) {
    const buildError = (data) => {
        let msg = 'Unknown error'
        if( data.status )
            msg = 'Error '+ data.status+ ': ';
        if( data.statusText)
            msg += data.statusText+ '. ';
        if( data.data && data.data.error) {
            msg = msg + '\n' + data.data.error;
        }
        console.warn("callApi buildError",msg)
        return msg;
    }

    const fullUrl = endpoint;
    console.log('fullUrl', fullUrl);

    let config = {
        url: fullUrl,
        timeout: 40000,
        method: method,
        responseType: 'text'
    }
    if (payload) {
        Object.assign(config, { data: payload });
    }
    console.log("axios.config", config);
    return axios(config)
        .then(function (response) {
            const text = response.data
//            console.log("AXIOS RESPONSE TEXT",response,text)
            return text;
        })
        .then(
        //      response => ({response}),
        response => {
//                       console.log("callApiText A response", response, { response });
            return { response };
        },
        error => {
            console.log("callApi B error", error);
            let errordata = "";
            if( error.code === 408 || error.code === 'ECONNABORTED') {
                errordata = `A timeout happend on url ${error.config.url}`;
            } else if (error.response) {
                console.log("error.response=",error.response);
                errordata = buildError(error.response);
            } else if (error.request) {
                console.log("error.request=",error.request);
                errordata = buildError(error.request);
            } else {
                console.log("error.message=",error.message);
                errordata = error.message;
            }
          console.log("response error", errordata);
            return ({ error: errordata || 'Something bad happened' });
        })
}

export const fetchBundles = (url) => callApiText(`../cmd/obr/${utils.URLEncode(url)}/bundles`, HTTP_METHOD.GET)



/*
    Keep the initial implementation using the 'fetch' browser api
    replaced now by the axios api.
    This implementation come from the 'REAL-WORLD' project of
    redux-saga exemples 
 */
/*
        return fetch(fullUrl)
            .then(response =>
                response.json().then(json => ({ json, response }))
            ).then(({ json, response }) => {
                if (!response.ok) {
                    return Promise.reject(json)
                }
    
                if( !schema ) {
                    // Whithout schema, we have to wrap json under 'entities'
                    // to be complient
                   return Object.assign({}, { entities: json});
                }
                const camelizedJson = json; // camelizeKeys(json)
                //     console.log("callApi fetch json",json);
                const ret = Object.assign({},
                    normalize(camelizedJson, schema)
                )
                console.log("callApi fetch json", json, ret);
                return ret;
            })
            .then(
            //      response => ({response}),
            function (response) {
                console.log("callApi A response", response, { response });
                return { response };
            },
            //      error => ({error: error.message || 'Something bad happened'})
            function (error) {
                console.log("callApi B error", { error: error.message });
                return ({ error: error.message || 'Something bad happened' });
            }
            )
 */