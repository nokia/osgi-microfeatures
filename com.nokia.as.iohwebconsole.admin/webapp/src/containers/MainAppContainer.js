/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import $ from "jquery"
import { connect } from 'react-redux';
import { start2dActivity, stop2dActivity, updateView, updateFlux } from '../actions';
import { getUserMessage } from '../reducers/userMsgReducer';
import { resetUserMessage, setErrorMessage } from '../actions/index';

import AppBanner from '@nokia-csf-uxr/csfWidgets/AppBanner';
import ToolBar from '../components/commons/ToolBar';
import UserDialog from '../components/commons/UserDialog';
import Viewer2d from '../components/Viewer2d';
import * as Labels from '../labels';
import admin from '../managers/main';
import '../App.css';


class MainAppContainer extends Component {
  constructor(props) {
    super(props);
    this._isMounted = false;
    this._2dmanager = false;
  }

  componentDidMount() {
    console.info("<MainAppContainer/> -MOUNTED- ")

    this._isMounted = true;
    // Remove default right part of AppBanner that display user info
    const rightPartNode = $('.user-account-summary-button');
    rightPartNode.remove();
    // Initialize the main manager used to fetch data and uses the vis Network api
    // to display the nodes/edges network in a dom container.
    try {
      // Provides some redux callbacks to the main manager.
      const { start, stop, setErrorMessage } = this.props;
      admin.init( start, stop, setErrorMessage);
      // Keep a reference to the 2D manager for later.
      this._2dmanager = admin.get2DManager();
    } catch (e) {
      console.trace("Admin init failed", e);
    }
  }

  componentWillUnmount() {
    this._isMounted = false;
    this._2dmanager = null;
    console.info("<MainAppContainer/> UNMOUNTED ")
  }

  _handleClickStartStop = (event) => {
    console.info("_handleClickStartStop",event,this.props.config);
    if (event.data.value === 'start') {
      this._2dmanager.changeConfig(this.props.config);
      this._2dmanager.open();
    } else {
      this._2dmanager.close();
    }
  }

  getToolBarProps = () => {
    let props = {
      id: 'myTb',
      pageTitle: Labels.LABEL_TOOLBAR_TITLE
    }
    if (this.props.isStarted2d === true) {
      props["iconButtons"] = [{
        icon: 'ic_power_off',
        tooltip: { text: Labels.LABEL_STOP_ACTIVITY },
        eventData: { value: 'stop' },
        onClick: this._handleClickStartStop
      }];
    } else {
      props["iconButtons"] = [{
        icon: 'ic_power',
        tooltip: { text: Labels.LABEL_START_ACTIVITY },
        eventData: { value: 'start' },
        onClick: this._handleClickStartStop
      }];
    }

    return props;
  }

  handleDismissClick = () => {
    this.props.resetUserMessage()
  }

  renderUserMessage = () => {
    const { userMessage } = this.props;
    if (!userMessage) {
      return null
    }
    return (
      <UserDialog {...userMessage} onClose={this.handleDismissClick} />
    )
  }

  renderViewer = () => {
    if (this.props.isStarted2d === true) {
      return (<Viewer2d {...this.props} />)
    }
    return (<div className="viewer2d off"></div>)
  }


  render() {
    console.log("<MainAppContainer props>", this.props);
    return (
      <AppBanner
        userAccountSummaryUsername={''}
        productFamily={Labels.LABEL_PRODUCT_FAMILY}
        productName={Labels.LABEL_PRODUCT_NAME}

      >
        <ToolBar {...this.getToolBarProps()} />
        { this.renderViewer() }
        {this.renderUserMessage()}

      </AppBanner>
    );
  }
}
const mapStateToProps = function (state) {
  console.log("MainAppContainer state=", state);
  return {
    isStarted2d: state.isStarted2d,
    config: state.config,
    userMessage: getUserMessage(state)
  }
}

const mapDispatchToProps = (dispatch) => ({
  start: () => dispatch(start2dActivity()),
  stop: () => dispatch(stop2dActivity()),
  updateView: (view) => dispatch(updateView(view)),
  updateFlux: (flux) => dispatch(updateFlux(flux)),
  resetUserMessage: () => dispatch(resetUserMessage()),
  setErrorMessage: (title, error, details) => dispatch(setErrorMessage(title, error, details))
})

export default connect(mapStateToProps, mapDispatchToProps)(MainAppContainer);
