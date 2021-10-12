/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import ReactDOM from 'react-dom';

import PropTypes from 'prop-types';
import _ from 'lodash';

import Button from '@nokia-csf-uxr/csfWidgets/Button';

class RcOverlayPanel extends Component {

    constructor(props) {
        super(props);
        this.state = {
            buttonState: 'closed-panel',
            styleObj: { height: '83%' },
            tabIndex: '-1'
        };
        this.thePanelOffset = 0;
        this.wasRecalculated = true; // find location of open panel after it has been closed

    }

    onOpen = (event) => {
        console.log('onOpen event', event)
        event.nativeEvent.preventDefault();

        let currentPanelState = "closed";

        if (this.state.buttonState === 'closed-panel') {
            currentPanelState = "open";
            this.setState({
                buttonState: 'opened-panel'
            })
        }
        else {
            this.setState({
                buttonState: 'closed-panel'
            })
        }

        if (this.props.onPanelAction) {
            this.props.onPanelAction({ event: event, state: currentPanelState });
        }
    }

    onClose = (event) => {
        //call onClick event for toggle button from button.js then call close panel fn
        if (this.state.buttonState === 'opened-panel') {
            console.log('onClose ', this.toggleButton)
            this.toggleButton.props.onClick(event);
            this.closePanel();
        }
    }

    closePanel = () => {
        this.setState({
            buttonState: 'closed-panel'
        })
    }

    onClick = (event) => {
        event.nativeEvent.preventDefault()
        //        console.log("Button clicked")
        if (this.props.onClick) {
            this.props.onClick({
                type: 'onClick',
                nativeEvent: event.nativeEvent,
                data: this.props.eventData
            });
        }
    }

    getToogleButton(context) {
        return (
            <Button
                ref={
                    (toggleButton) => {
                        context.toggleButton = toggleButton
                    }
                }
                id={context.props.id + "-openPanel"}
                onClick={context.onOpen}
                //                className={"csfWidgets open-panel"}
                //                iconUrl={context.props.overlayIconUrl}
                data-open-panel={context.state.buttonState === 'opened-panel'}
                icon={context.props.overlayIcon ? null : 'ic_info'}
                iconColor={context.state.buttonState === 'opened-panel' ? 'rgba(18,65,145,1)' : 'rgba(0,0,1,.54)'}
            //                css={{ icon: true, noText: true }}
            />
        );
    }

    getPanelButton() {
        if (this.props.buttonType === 'action') {
            return (
                <Button
                    id={this.props.id + "-panel-btn"}
                    text={this.props.buttonText}
                    onClick={this.onClick}
                    //                   className={"csfWidgets panel-action-btn"}
                    //                    iconClassName={"panel-action-btn"}
                    //                    css={{ callToAction: true }}
                    isCallToAction
                    disabled={this.props.disablePanelBtn}
                    tabIndex={this.state.tabIndex}
                    data-panel-action={true}
                />
            );
        }
        else if (this.props.buttonType === 'standard') {
            return (
                <Button
                    id={"panel-btn"}
                    text={this.props.buttonText}
                    onClick={this.onClick}
                    //                    className={"csfWidgets panel-standard-btn"}
                    disabled={this.props.disablePanelBtn}
                />
            );
        }
        else {
            return '';
        }
    }

    getContent() {

        if (this.state.buttonState === 'opened-panel') {
            return (
                <div className='panel-body'>
                    {this.props.content}
                </div>
            );
        }
        else {
            return (
                <div className='panel-body'>
                </div>
            );
        }

    }


    render() {
        let newMinWidth = this.state.styleObj.minWidth;
        return (
            <div className={"csfWidgets panel-wrapper"}>
                <div id={this.props.id} className="overlay-wrapper" style={{ minWidth: newMinWidth, top: this.props.wrapperOffset }}>
                    {this.props.getToogleButton(this)}
                    <div
                        id={this.props.id + '-panel'}
                        ref={
                            (overlayPanelRef) => {
                                this.overlayPanelRef = overlayPanelRef
                            }
                        }
                        className={this.props.className + ' container ' + this.state.buttonState + (this.props.hideFooter ? ' hide-footer ' : '') + (this.props.fullWidth ? ' fullWidth ' : '')}
                        style={this.state.styleObj}
                    >
                        <div className='panel-header'>
                            <div className='overlay-title'>{this.props.title}</div>
                            <Button
                                id={this.props.id + "-closePanel"}
                                onClick={this.onClose}
                                data-close-panel={true}
                                icon="ic_close"
                                //                                className={"csfWidgets close-panel"}
                                //                                iconClassName={"close-btn"}
                                //                                css={{ icon: true, noText: true }}
                                tabIndex={this.state.tabIndex}
                            />
                        </div>
                        {this.getContent()}
                        {!this.props.hideFooter && (
                            <div className='panel-footer' >
                                {this.getPanelButton()}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    componentDidMount() {
        let panelOffset = this.getPanelOffset();
        this.thePanelOffset = panelOffset;
        this.setSize();
        window.addEventListener('resize', this.setSize);

    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.setSize);
    }

    componentWillUpdate(nextProps, nextState) {
        if (this.state.buttonState !== nextState.buttonState) {
            this.wasRecalculated = false;
            if (nextState === 'opened-panel') {
                // means last state was closed and non-existant, need the calculation redone
                this.wasRecalculated = true;
            }

            // panel opened or closed
            if (this.props.onPanelOpenClose) {
                this.props.onPanelOpenClose();
            }
        }
    }

    componentDidUpdate() {
        if (!this.wasRecalculated) {
            this.wasRecalculated = true;
            this.setSize();
        }
    }

    //returns the distance, in pixels, of the top of the OverlayPanle to the top of the viewport
    getPanelOffset = () => {
        let myDom = ReactDOM.findDOMNode(this);
        let overlayPanelELemH = myDom.querySelector(".overlay-panel");
        let overlayPanelParent = overlayPanelELemH.firstChild;
        let distanceToTop = overlayPanelParent.getBoundingClientRect().top + 48
        return distanceToTop;
    }

    setSize = () => {
        // Needed to get overlay panel to bottom of window
        //       let panel = document.getElementById(this.props.id).parentElement;
        //        let height = 100 - 100 * (panel.offsetTop / window.innerHeight);

        //calculate aggregate width of tabs and close button and set min-width on overlay-wrapper class
        let totalMinWidth = 52;
        //        let minWidth = totalMinWidth + 'px;';
        //        height = height + '%';

        totalMinWidth = 320; // 320 ( old 252)

//        console.log("setSize() window.innerHeight",window.innerHeight)
        let offsetHeight = (window.innerHeight - this.thePanelOffset) + 'px';
        let styleObj = {
            minWidth: totalMinWidth,
            height: offsetHeight,
            top: this.props.panelOffset
        }
 //               console.log("setSize", styleObj);

        this.setState({
            styleObj: styleObj
        });

    }


}

RcOverlayPanel.defaultProps = {
    id: 'MakeUnique ',
    className: 'overlay-panel',
    title: '',
    content: null,
    buttonType: 'standard',
    buttonText: 'OK',
    onClick: _.noop,
    hideFooter: false,
    onPanelAction: _.noop,
    getToogleButton: RcOverlayPanel.prototype.getToogleButton,
    disablePanelBtn: false,
    wrapperOffset: 0,
    panelOffset: 0,
}

RcOverlayPanel.propTypes = {
    id: PropTypes.string.isRequired,
    className: PropTypes.string.isRequired,
    title: PropTypes.string,
    content: PropTypes.node,
    buttonType: PropTypes.string,
    buttonText: PropTypes.string,
    onClick: PropTypes.func,
    selectedIndex: PropTypes.number,
    hideFooter: PropTypes.bool,
    onPanelAction: PropTypes.func,
    getToogleButton: PropTypes.func,
    wrapperOffset: PropTypes.number,
    panelOffset: PropTypes.number,

};

export default RcOverlayPanel