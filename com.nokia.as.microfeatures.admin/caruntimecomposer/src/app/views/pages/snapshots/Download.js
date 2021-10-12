/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import PropTypes from 'prop-types';
import $ from "jquery"
import Cookies from 'universal-cookie';
import ProgressBarDeterminate from '@nokia-csf-uxr/csfWidgets/ProgressBarDeterminate';

class Download extends Component {
    static propTypes = {
        /** Unique id for dialog. For accessibility requirements, there can be no
         * two elements with the same ID on a given page. */
        id: PropTypes.string.isRequired,
        /** download command  */
        location: PropTypes.string.isRequired,
        /** Relative Url to load the donwload page */
        iframesrc: PropTypes.string.isRequired,
        /** Text displayed in error dialog */
        textOperation: PropTypes.string,
        /** Text displayed in progress bar */
        progressText: PropTypes.string,
        /** Function to be invoked when the download success */
        onSuccess: PropTypes.func.isRequired,
        /** Function to be invoked when the download fails */
        onError: PropTypes.func.isRequired,
    }

    static defaultProps = {
        id: 'frame_dowload',
        location: '/cmd/runtime',
        iframesrc: "download.html",
        textOperation: 'Create runtime',
        progressText : 'Creating runtime...'
    }

    constructor(props) {
        super(props);
        this.cid = this.generateCid();
        this.location = "";
        this.maxWait = 180; //seconds
        this.checkSteps = 200; //200ms
        this.maxTries = this.maxWait * 1000 / this.checkSteps;
        this.state = { tries: 0 };
        this.cookies = new Cookies();
    }

    componentWillUnmount() {
        console.log("Download componentWillUnmount", this.iframe)
        this.iframe = null;

    }

    generateCid = () => {
        console.log("generateCid...")
        function genRD() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return genRD() + genRD() + '-' + genRD() + '-' + genRD() + '-' +
            genRD() + '-' + genRD() + genRD() + genRD();
    }

    onLoadIframe = () => {
        this.cid = this.generateCid();
        console.log("IFRAME LOADED!!!", this.cid)
        const location = this.props.location + '?cache=' + new Date().getTime();
        const obrs = this.props.obrs.toString();

        const $iframe = $(this.iframe);
        $iframe.contents().find('form')
            .attr("action", location)
            .empty()
            .append($("<input>").attr("name", "json").attr('value', JSON.stringify(this.props.payload)))
            .append($("<input>").attr("name", "cid").attr('value', this.cid)).append(
            $("<input>").attr("name", "obrs").attr('value', obrs ? obrs : ""))
            .submit();

        const self = this;
        this.setState({ tries: 0 })
        setTimeout(function () { self.checkFileDownloadComplete.call(self); }, self.checkSteps);
    }

    checkFileDownloadComplete = () => {
        try {
            if (this.state.tries >= this.maxTries) {
                this.error("Timeout while loading file!");
                return;
            }
            this.setState({ tries: this.state.tries + 1 })
            var self = this;
            var cookieVal = this.cookies.get(this.cid);

            var iframeDoc = this.iframe.contentWindow || this.iframe.contentDocument;
            if (iframeDoc && iframeDoc.document) {
                iframeDoc = iframeDoc.document;
            }

            if (cookieVal == null || cookieVal === 'undefined') {

                if (iframeDoc) {
                    if (iframeDoc && iframeDoc.body !== null && iframeDoc.body.innerHTML.length) {
                        if (iframeDoc.body.innerHTML.indexOf("dowloadf") === -1) {
                            setTimeout(function () {
                                try {
                                    var iframeDocText = iframeDoc.body.textContent || iframeDoc.body.innerText;
                                    var json = JSON.parse(iframeDocText);
                                    if (json && json.error) {
                                        self.error.call(self, json.error);
                                        return;
                                    }
                                } catch (e) {
                                    console.log("checkFileDownloadComplete: error parsing json error: " + e);
                                }
                                self.error("Unable to download file!");
                            }, 500);
                            return;
                        }
                    }
                }
            } else {
                //success, remove cookie
                this.cookies.set(this.cid, null, { path: '/' });
                this.success();
                return;
            }
            setTimeout(function () { self.checkFileDownloadComplete.call(self); }, self.checkSteps);
        } catch (e) {
            console.log("Exception", e)
            this.error(e);
        }

    }


    error = (message) => {
        console.log("ERROR msg", message);
        this.props.onError(this.props.textOperation, message.toString())
    };

    success = () => {
        // dispatch action status = 'ok'
        console.log("SUCCESS");
        this.props.onSuccess()

    };


    render() {
//        console.log('<Download /> props', this.props)
        return (
            <div id='downloadoverlay' className={'csfWidgets overlay active black'} >

                <div className={'downloadprogress'} >
                    <ProgressBarDeterminate
                        progress={ this.state.tries * 4 }
                        total={this.maxTries}
                        text={{ content: this.props.progressText, vertical: 'above' }}
                        docked={false}
                        showProgress={false}
                        easeProgress={false}
                    />
                </div>
                <div style={{ display: 'none' }}>
                    {
                        <iframe ref={(iframe) => { this.iframe = iframe; }}
                            id={this.props.id}
                            title={'downloadIframe'}
                            src={this.props.iframesrc}
                            onLoad={this.onLoadIframe}
                        />
                    }
                </div>
            </div >
        )
    }


}

export default Download;