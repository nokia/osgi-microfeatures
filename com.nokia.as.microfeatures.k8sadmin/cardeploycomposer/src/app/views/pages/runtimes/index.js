/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import { bindActionCreators } from "redux";
import { connect } from "react-redux";
import { createSelector } from "reselect";
import { merge } from "lodash";
import { entitiesSelectors, entitiesOperations } from "../../../state/ducks/entities";
import {
  runtimeSelectors,
  runtimeOperations
} from "../../../state/ducks/runtime";
import {
  terminalSelectors,
  terminalActions
} from "../../../state/ducks/terminal";

import AlertDialogConfirm from "@nokia-csf-uxr/csfWidgets/AlertDialogConfirm";

import ToolBar from "../../commons/ToolBar";
import DetailsPanel from "./DetailsPanel";
import Terminal from "./Terminal";

import RuntimesList from "./RuntimesList";

class Runtimes extends Component {
  constructor(props) {
    super(props);
    this.toolbarh = 69;
  }

  getListHeight = () => this.props.height - (this.toolbarh + 40);

  componentWillReceiveProps(nextProps) {
    // When a runtime has been undeloyed, we have to close its terminal
    if (
      nextProps.isTerminalOpen === true &&
      nextProps.terminalId &&
      nextProps.terminalId !== "" &&
      !nextProps.runtimeIds.includes(nextProps.terminalId)
    ) {
      // Close the terminal
      console.log(
        "componentWillReceiveProps FORCE TO CLOSE TERMINAL",
        this.props,
        nextProps
      );
      this.props.closeTerminal();
    }
  }

  componentWillUnmount() {
    // Close Terminal if open yet.
    this.props.closeTerminal();
  }

  undeployRuntime = (payload) => {
    const { isAdministrator } = this.props.configData;
    if (isAdministrator !== true) {
      this.props.pollStopRuntimes();
      this.props.history.push(`/accessDenied`);
    } else {
      this.props.undeployRuntime(payload);
    }
  }

  startTerminal = (fid, name, url) => {
    const { isAdministrator } = this.props.configData;
    if (isAdministrator !== true) {
      this.props.pollStopRuntimes();
      this.props.history.push(`/accessDenied`);
    } else {
      this.props.startTerminal(fid, name, url);
    }
  }

  render() {
    console.log("<Runtimes /> props ", this.props);
    const runtime = this.props.undeployPayload;
    const { name, namespace } = runtime;
    const deleteInfo1 = "Name : " + name;
    const deleteInfo2 = "Namespace : " + namespace;
    const isUndeploying = this.props.getUndeployStatus === "started";

    return (
      <div id={'runtimes-page'} style={{ height: this.props.height, position: "relative" }}>
        <ToolBar pageTitle={"Runtimes"} />
        {isUndeploying === true && (
          <div
            id="undeployoverlay"
            className={"csfWidgets overlay active black"}
          />
        )}
        {this.props.runtimeDetailsId !== null && (
          <DetailsPanel
            runtime={this.props.detailRuntime}
            onCancel={this.props.closeRuntimeDetails}
            startPolling={this.props.pollStartRuntimes}
            stopPolling={this.props.pollStopRuntimes}
          />
        )}
        {this.props.openConfirmUndeploy && (
          <AlertDialogConfirm
            title={"Do you want to undeploy this runtime?"}
            confirmationText1={deleteInfo1}
            confirmationText2={deleteInfo2}
            confirmationButtonLabel={"UNDEPLOY"}
            onClose={this.props.cancelUndeployRuntime}
            onConfirm={this.props.confirmUndeployRuntime}
          />
        )}
        <div
          id="runtimes-rows"
          style={{ height: this.getListHeight(), position: "relative" }}
        >
          <RuntimesList
            runtimesListData={this.props.runtimesListData}
            undeployRuntime={this.undeployRuntime}
            openDetails={this.props.openRuntimeDetails}
            startTerminal={this.startTerminal}
            width={this.props.width}
          />
        </div>
        {this.props.isTerminalOpen && (
          <Terminal
            id={this.props.terminalId}
            pod={this.props.terminalPod}
            url={this.props.terminalUrl}
            close={this.props.closeTerminal}
            width={this.props.width}
          />
        )}
      </div>
    );
  }
}

const getRuntimesList = state =>
  createSelector(
    entitiesSelectors.getAllRuntimes,
    runtimes => {
      const runs = merge([], runtimes).map(runtime => {
        let features = entitiesSelectors.getRuntimeFeatures(state, runtime.fid);

        let newRuntime = Object.assign({}, runtime, { features: features });
        return newRuntime;
      });

      console.log("getRuntimesList has worked!");
      return runs;
    }
  )(state);

const getDetailRuntime = state =>
  createSelector(
    runtimeSelectors.getRuntimeIdForDetails,
    (id) => {
      const runtime = entitiesSelectors.getRuntimeById(state,id)
      console.log("getDetailRuntime", runtime)
      return runtime;
    }
    )(state);

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function(state) {
  return {
    runtimesListData: getRuntimesList(state),
    runtimeIds: entitiesSelectors.getRuntimeIds(state),
    openConfirmUndeploy: runtimeSelectors.doConfirmUndeployRuntime(state),
    undeployPayload: runtimeSelectors.getUndeployPayload(state),
    getUndeployStatus: runtimeSelectors.getUndeployStatus(state),
    runtimeDetailsId: runtimeSelectors.getRuntimeIdForDetails(state),
    detailRuntime: getDetailRuntime(state),
    isTerminalOpen: terminalSelectors.isTerminalOpen(state),
    terminalId: terminalSelectors.terminalId(state),
    terminalPod: terminalSelectors.terminalPod(state),
    terminalUrl: terminalSelectors.terminalUrl(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      ...runtimeOperations,
      ...terminalActions,
      pollStopRuntimes: entitiesOperations.pollStopRuntimes,
      pollStartRuntimes: entitiesOperations.pollStartRuntimes

    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Runtimes);
