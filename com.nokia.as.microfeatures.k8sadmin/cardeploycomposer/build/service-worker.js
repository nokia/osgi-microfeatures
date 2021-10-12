/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

"use strict";var precacheConfig=[["./index.html","86cd441fe013db2ad021604d55efcb19"],["./static/css/main.82eaf7ae.css","67429672d73be83448880518406e0ad5"],["./static/media/NokiaPureHeadline-Light.52ea4690.woff","52ea46908455f3572d4190c592627ed0"],["./static/media/NokiaPureText-Bold.a955b4d3.woff","a955b4d39c04a8c5375d15d5451f9318"],["./static/media/NokiaPureText-Medium.72fd2e6f.woff","72fd2e6fdbc2ab562b746af7ccf85b07"],["./static/media/NokiaPureText-Regular.eb5af0e7.woff","eb5af0e7ce9a201d672fbe0343acc1f6"],["./static/media/Nokia_logo_blue.9e9ddacb.svg","9e9ddacb441db72cc023e9c0e7c84069"],["./static/media/Nokia_logo_white.27e3a263.svg","27e3a263aa70f7d0fda8304653f97c75"],["./static/media/Text_Area_Resize_Gripper.d26eda37.svg","d26eda373f9825aac3592ee8fc5e108c"],["./static/media/ic_Constraints.d3b88c74.svg","d3b88c744364c430c9e3cd4638de24dc"],["./static/media/ic_arrow_back.d95b70ff.svg","d95b70ffa6b66d0696667b8a4811d8e5"],["./static/media/ic_arrow_downward.33ed1b05.svg","33ed1b05038f767e1a8e692dc0fa90d1"],["./static/media/ic_arrow_drop_down.00eb4890.svg","00eb48901cc3a70722e08e9f01e36355"],["./static/media/ic_arrow_upward.4ae80b59.svg","4ae80b59bf5ec5fed6916c23e79a8adf"],["./static/media/ic_check.d74f173b.svg","d74f173bb1a6049490c26a46877945a6"],["./static/media/ic_check_white.a43450f4.svg","a43450f4f15a0e7f249c02fa539ac7da"],["./static/media/ic_chevron_left.cf56314c.svg","cf56314c268d279e8b93e44358d0b67f"],["./static/media/ic_chevron_right.eed433fa.svg","eed433fab6dfafe2dbea294f27366821"],["./static/media/ic_close.9e15cad6.svg","9e15cad6b0db1c4760886ec729dcc2f1"],["./static/media/ic_close_circle.6aea7d8a.svg","6aea7d8ab19477eda037680ffd78e5c1"],["./static/media/ic_dashboard.fceb2a81.svg","fceb2a81c9ba45df52625bf4a82d7662"],["./static/media/ic_delete.7ad18fe0.svg","7ad18fe0854aee43ad0abc3d85750dd9"],["./static/media/ic_drag_handle.debaaf68.svg","debaaf68ef50bdb446d86ab039a3b35b"],["./static/media/ic_edit.eb7173f3.svg","eb7173f349c732299c8563b2a16e4138"],["./static/media/ic_error_alert.f56a095b.svg","f56a095b860ab1556aa49f142533b316"],["./static/media/ic_expand_less.e4240b05.svg","e4240b055ba30d0c2bd646062740aab9"],["./static/media/ic_expand_more.c51cf6dc.svg","c51cf6dce1088fabbc96d98688ce7cfd"],["./static/media/ic_file.62c61350.svg","62c613506a7085c35e38bda1fa0880d3"],["./static/media/ic_find.afd5099e.svg","afd5099ebc2738310576dfaa36080526"],["./static/media/ic_find_in_page.3119eb4e.svg","3119eb4e7ec0ef74381c826bf551acd1"],["./static/media/ic_health.71a3a7e4.svg","71a3a7e47be4c3de8e0905da19fbf846"],["./static/media/ic_help.167530c4.svg","167530c46ede3dd9e7f42b2ec8904352"],["./static/media/ic_history.4f73fbfe.svg","4f73fbfe44f257730af693d886e7d00b"],["./static/media/ic_info_outline.c83bd717.svg","c83bd717fc092b404c211782710a23c7"],["./static/media/ic_maintenance.af809da7.svg","af809da798bae6947b09f788a8e2ead0"],["./static/media/ic_managed_routes.e331f339.svg","e331f3393d05fc4b42d1f83eef33151a"],["./static/media/ic_matrix.4a9b8bc0.svg","4a9b8bc07672cb30f68b4466e3334ace"],["./static/media/ic_menu.5a791823.svg","5a791823a51dcc6a68b4179d4d7b9d22"],["./static/media/ic_more_horizontal.13cc77fa.svg","13cc77fa0179dfe0e5fe625f30fc5183"],["./static/media/ic_rack.a32c65b7.svg","a32c65b73eaa0a4324bebbc6daf64e00"],["./static/media/ic_settings.6ccbf805.svg","6ccbf8052f56e7bbdbca5c69ecf9a8f1"],["./static/media/ic_sort.9105e3e2.svg","9105e3e249c6a75096b88bae4a5cb015"],["./static/media/ic_sort_ascending.0ca6f6de.svg","0ca6f6de4f312f0b39a49a3f15e5e500"],["./static/media/ic_warning_alert.4b5ae5ff.svg","4b5ae5ff2b39f4b045094ec50eb38b06"],["./static/media/login-back-blue.8ffb9ed7.jpg","8ffb9ed752a63812178d455e44d38381"],["./static/media/login_background_blur.5bb5f4e4.png","5bb5f4e4f2c7615e878b55e7144a765b"]],cacheName="sw-precache-v3-sw-precache-webpack-plugin-"+(self.registration?self.registration.scope:""),ignoreUrlParametersMatching=[/^utm_/],addDirectoryIndex=function(e,a){var c=new URL(e);return"/"===c.pathname.slice(-1)&&(c.pathname+=a),c.toString()},cleanResponse=function(a){return a.redirected?("body"in a?Promise.resolve(a.body):a.blob()).then(function(e){return new Response(e,{headers:a.headers,status:a.status,statusText:a.statusText})}):Promise.resolve(a)},createCacheKey=function(e,a,c,t){var i=new URL(e);return t&&i.pathname.match(t)||(i.search+=(i.search?"&":"")+encodeURIComponent(a)+"="+encodeURIComponent(c)),i.toString()},isPathWhitelisted=function(e,a){if(0===e.length)return!0;var c=new URL(a).pathname;return e.some(function(e){return c.match(e)})},stripIgnoredUrlParameters=function(e,c){var a=new URL(e);return a.hash="",a.search=a.search.slice(1).split("&").map(function(e){return e.split("=")}).filter(function(a){return c.every(function(e){return!e.test(a[0])})}).map(function(e){return e.join("=")}).join("&"),a.toString()},hashParamName="_sw-precache",urlsToCacheKeys=new Map(precacheConfig.map(function(e){var a=e[0],c=e[1],t=new URL(a,self.location),i=createCacheKey(t,hashParamName,c,/\.\w{8}\./);return[t.toString(),i]}));function setOfCachedUrls(e){return e.keys().then(function(e){return e.map(function(e){return e.url})}).then(function(e){return new Set(e)})}self.addEventListener("install",function(e){e.waitUntil(caches.open(cacheName).then(function(t){return setOfCachedUrls(t).then(function(c){return Promise.all(Array.from(urlsToCacheKeys.values()).map(function(a){if(!c.has(a)){var e=new Request(a,{credentials:"same-origin"});return fetch(e).then(function(e){if(!e.ok)throw new Error("Request for "+a+" returned a response with status "+e.status);return cleanResponse(e).then(function(e){return t.put(a,e)})})}}))})}).then(function(){return self.skipWaiting()}))}),self.addEventListener("activate",function(e){var c=new Set(urlsToCacheKeys.values());e.waitUntil(caches.open(cacheName).then(function(a){return a.keys().then(function(e){return Promise.all(e.map(function(e){if(!c.has(e.url))return a.delete(e)}))})}).then(function(){return self.clients.claim()}))}),self.addEventListener("fetch",function(a){if("GET"===a.request.method){var e,c=stripIgnoredUrlParameters(a.request.url,ignoreUrlParametersMatching),t="index.html";(e=urlsToCacheKeys.has(c))||(c=addDirectoryIndex(c,t),e=urlsToCacheKeys.has(c));var i="./index.html";!e&&"navigate"===a.request.mode&&isPathWhitelisted(["^(?!\\/__).*"],a.request.url)&&(c=new URL(i,self.location).toString(),e=urlsToCacheKeys.has(c)),e&&a.respondWith(caches.open(cacheName).then(function(e){return e.match(urlsToCacheKeys.get(c)).then(function(e){if(e)return e;throw Error("The cached response that was expected is missing.")})}).catch(function(e){return console.warn('Couldn\'t serve response for "%s" from cache: %O',a.request.url,e),fetch(a.request)}))}});