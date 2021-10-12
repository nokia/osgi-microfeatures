/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

function OAuthLogoutPlugin() {

    var lastAuthUrl = null;
    var pay = null;

    return {
        statePlugins: {
            auth: {
                wrapActions: {

                    authorizeOauth2: (originalAction, system) => (payload) => {
                        originalAction(payload);

                        var auth = payload.auth;
                        lastAccessToken = payload.token.access_token;
                        lastTokenUrl = payload.auth.schema.get('tokenUrl');
                        pay = payload;
                        if (auth) {
                            lastAuthUrl = auth.schema.get('authorizationUrl');
                        }
                    },

                    logout: (originalAction, system) => (payload) => {
                        originalAction(payload);
                        var protocol = location.protocol;
                        var slashes = protocol.concat("//");
                        var host = slashes.concat(window.location.host);
                        var path = window.location.pathname
                        var full_url = host.concat(path.substring(0, path.lastIndexOf("/")))
                        var redirectUrl = full_url + "/oauth2-logout.html";
                        var client_id = pay.auth.clientId;
                        var refresh_token = pay.token.refresh_token;

                        if (lastAuthUrl) {
                            var logoutUrl = lastAuthUrl.substring(0, lastAuthUrl.lastIndexOf('/')) + '/logout';

                            var form = document.createElement("form");
                            form.setAttribute("method", "post");
                            form.setAttribute("action", logoutUrl);
                            form.setAttribute("target", "view");

                            var hiddenField = document.createElement("input"); 
                            hiddenField.setAttribute("type", "hidden");
                            hiddenField.setAttribute("name", "redirect_uri");
                            hiddenField.setAttribute("value", encodeURIComponent(redirectUrl));
                            form.appendChild(hiddenField);

                            var hiddenField2 = document.createElement("input"); 
                            hiddenField2.setAttribute("type", "hidden");
                            hiddenField2.setAttribute("name", "client_id");
                            hiddenField2.setAttribute("value", client_id);
                            form.appendChild(hiddenField2);

                            var hiddenField3 = document.createElement("input"); 
                            hiddenField3.setAttribute("type", "hidden");
                            hiddenField3.setAttribute("name", "refresh_token");
                            hiddenField3.setAttribute("value", refresh_token);
                            form.appendChild(hiddenField3);

                            document.body.appendChild(form);
                            window.open('', 'view');

                            form.submit();
                        } else {
                            var logoutUrl = lastTokenUrl.substring(0, lastTokenUrl.lastIndexOf('/')) + '/logout';
                            logoutUrl += "?id_token_hint=" + lastAccessToken;
                            logoutUrl += "&redirect_uri=" + encodeURIComponent(redirectUrl);

                            window.open(logoutUrl);
                        }
                    }
                }
            }
        }
    }
}
