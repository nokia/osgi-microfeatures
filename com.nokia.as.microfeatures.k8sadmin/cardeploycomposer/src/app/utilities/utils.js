/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/* Utility class */
import React from "react";
import uniqueId from "lodash/uniqueId";
import { get } from "lodash";
import { put, call } from "redux-saga/effects";
import $ from "jquery";

export const getPageSize = () => {
  if (typeof window.innerWidth === "number") {
    return { width: window.innerWidth, height: window.innerHeight };
  }
  if (
    document.documentElement &&
    (document.documentElement.clientWidth ||
      document.documentElement.clientHeight)
  ) {
    return {
      width: document.documentElement.clientWidth,
      height: document.documentElement.clientHeight
    };
  }
  if (
    document.body &&
    (document.body.clientWidth || document.body.clientHeight)
  ) {
    return {
      width: document.body.clientWidth,
      height: document.body.clientHeight
    };
  }
};

export const genReactKey = name => `${name}:${uniqueId()}`;

export const generateId = (s1, s2) => s1 + "@" + s2;
const decodeId = (id, s1 = "name", s2 = "version", tab = id.split("@", 2)) => {
  let x = {};
  x[s1] = tab[0];
  x[s2] = tab[1];
  return x;
};

export const encodeFeatureId = feature =>
  generateId(feature.name, feature.version);
export const decodeFeatureId = id => decodeId(id);

export const hashCode = s => {
  let hash = 0,
    i,
    chr,
    len;
  if (s.length === 0) return hash;
  len = s.length;
  for (i = 0; i < len; i++) {
    chr = s.charCodeAt(i);
    hash = (hash << 5) - hash + chr;
    hash |= 0; // Convert to 32bit integer
  }
  return hash;
};

function URLEncode(c) {
  var o = "";
  var x = 0;
  c = c.toString();
  var r = /(^[a-zA-Z0-9_.]*)/;
  while (x < c.length) {
    var m = r.exec(c.substr(x));
    if (m !== null && m.length > 1 && m[1] !== "") {
      o += m[1];
      x += m[1].length;
    } else {
      if (c[x] === " ") o += "+";
      else {
        var d = c.charCodeAt(x);
        var h = d.toString(16);
        o += "%" + (h.length < 2 ? "0" : "") + h.toUpperCase();
      }
      x++;
    }
  }
  return o;
}

export const msgTypes = {
  REQUEST: "REQUEST",
  SUCCESS: "SUCCESS",
  FAILURE: "FAILURE"
};

/**
 * Allows to create action types such as NAME_REQUEST, NAME_SUCCESS and NAME_FAILURE
 * whose NAME is the base value
 * @param base
 */
function createRequestTypes(base) {
  return [msgTypes.REQUEST, msgTypes.SUCCESS, msgTypes.FAILURE].reduce(
    (acc, type) => {
      acc[type] = `${base}_${type}`;
      return acc;
    },
    {}
  );
}

function action(type, payload = {}, id = {}) {
  console.log("utils action", type, payload, id);
  const result = { type, ...payload, ...id };
  console.log("utils action result", result);
  return result;
}

/***************************** Subroutines ************************************/

// resuable fetch Subroutine
// entity :  obrs | features
// apiFn  : api.fetchObrsList | api.fetchFeature | ...
// id     : login | fullName
// url    : next page url. If not provided will use pass id to apiFn
function* fetchEntity(entity, apiFn, id, url, params) {
  /*    
    console.log("fetchEntity entity =>",entity)
    console.log("fetchEntity apiFn =>",apiFn)
    console.log("fetchEntity id =>",id)
    console.log("fetchEntity url =>",url)
    console.log("fetchEntity params =>",params)
*/
  yield put(entity.request(id, params));
  const { response, error } = yield call(apiFn, url || id, params);
  //    console.log("fetchEntity", response, error, id);
  if (response) yield put(entity.success(id, response, params));
  else yield put(entity.failure(id, error, params));
}

// GLOBALIZE selectors
const fromRoot = (path, selector) => (state, ...args) =>
  selector(get(state, path), ...args);
const globalizeSelectors = (selectors, path) => {
  return Object.keys(selectors).reduce((final, key) => {
    final[key] = fromRoot(path, selectors[key]);
    return final;
  }, {});
};

//
// Bundles
//

const parseBundles = function(xml) {
  const obr = $(xml);
  const obrBundles = {};
  obr.find("resource").each(function() {
    try {
      var identity = $(this)
        .find("capability[namespace='osgi.identity']")
        .first();
      var symbolicname = identity
        .find("attribute[name='osgi.identity']")
        .attr("value");
      var version = identity.find("attribute[name='version']").attr("value");
      /*
                var catsvalue = $identity.find("attribute[name='category']").attr("value");
                var cats = ( !catsvalue )?'No category':catsvalue.replace(/ /g, "");
                var presentationname = $identity.find("attribute[name='presentationname']").attr("value");
                var content = $(this).find("capability[namespace='osgi.content']").first();
                var uri = $content.find("attribute[name='url']").attr("value");*/
      if (symbolicname) {
        const id = generateId(symbolicname, version);
        obrBundles[id] = { bsn: symbolicname, version: version, fid: id };
      }
    } catch (e) {
      console.log("parseBundles failed, reason=" + e);
    }
  });
  return obrBundles;
};

//
// Comparators
//
const naturalCompare = function(a, b) {
  var ax = [],
    bx = [];

  a.replace(/(\d+)|(\D+)/g, function(_, $1, $2) {
    ax.push([$1 || Infinity, $2 || ""]);
  });
  b.replace(/(\d+)|(\D+)/g, function(_, $1, $2) {
    bx.push([$1 || Infinity, $2 || ""]);
  });

  while (ax.length && bx.length) {
    var an = ax.shift();
    var bn = bx.shift();
    var nn = an[0] - bn[0] || an[1].localeCompare(bn[1]);
    if (nn) return nn;
  }

  return ax.length - bx.length;
};

//
// Deploy tools
//
const OBR_VERSION_REGEXP = /.*obr-(.*)\.xml$/;

export const getObrVersion = function(url) {
  OBR_VERSION_REGEXP.lastIndex = 0;
  const match = OBR_VERSION_REGEXP.exec(url);
  if (!match || match.length < 2) {
    console.warn("getUrlLabel no matching", url, match);
    return undefined;
  }
  return match[1];
};

export const getObrRepository = function(url) {
  const splitted = url.split('/');
  if( splitted.length > 4 )
    return splitted[3];
  return undefined;
}

export const generateFormError = function(errorMsgs) {
  if (errorMsgs) {
    return (
      <ul>
        {React.Children.map(errorMsgs, errorMsg => {
          if (errorMsg) {
            return <li>{errorMsg}</li>;
          }
        })}
      </ul>
    );
  }
  return null;
};


export const propertyfileToJson = content => {
  // For each line ends by '\' appends it to the previous to obtain only a single line by property
  const ncontent = content.replace(/\\\n/g,'')
  const lines = ncontent.split("\n");
    // Remove comment lines
  const kvs = lines.filter(line => {
                if(/^(#)+/.test(line) === true)
                  return false;
                return line;
        } );

  const json = {};
  kvs.forEach(line => {
    console.log("LINE", line);
    const key = line.substring(0,line.indexOf('=')).trim();
    const value = line.substring(line.indexOf('=')+1).trim();
    json[key] = value;
  });

  // Create properties as json
  console.log("json",json);
  return json

}

export default {
  generateFormError,
  getObrVersion,
  getPageSize,
  genReactKey,
  generateId,
  encodeFeatureId,
  decodeFeatureId,
  hashCode,
  msgTypes,
  createRequestTypes,
  action,
  fetchEntity,
  globalizeSelectors,
  URLEncode,
  parseBundles,
  naturalCompare,
  propertyfileToJson
};
