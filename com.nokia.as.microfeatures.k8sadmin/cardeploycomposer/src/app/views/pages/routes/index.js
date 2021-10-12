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
import { withRouter } from "react-router-dom";
import { createSelector } from "reselect";

import { routeSelectors, routeOperations } from "../../../state/ducks/route";
import {
  entitiesSelectors,
  entitiesOperations
} from "../../../state/ducks/entities";

import ToolBar from "../../commons/ToolBar";
import RoutesList from "./RoutesList";
import QuickSel from "../../commons/QuickSel";
import RcStatusIndicator from "../../commons/RcStatusIndicator";
import Button from "@nokia-csf-uxr/csfWidgets/Button";
import AlertDialogConfirm from "@nokia-csf-uxr/csfWidgets/AlertDialogConfirm";

class Routes extends Component {
  constructor(props) {
    super(props);
    this.toolbarh = 64;
    this.quickSelh = 67;
  }

  getListHeight = () =>
    this.props.height - (this.toolbarh + this.quickSelh + 20);

  /**
   * Move to the deployment wizard page.
   */
  openRouteWizard = () => {
    const { isAdministrator } = this.props.configData;
    const newRoute = (isAdministrator === true)?`/routewizard`:`/accessDenied`;
    this.props.pollStopRoutes();
    this.props.history.push(newRoute);
  };

  deleteRoute = (payload) => {
    const { isAdministrator } = this.props.configData;
    if (isAdministrator !== true) {
      this.props.pollStopRoutes();
      this.props.history.push(`/accessDenied`);
    } else {
      this.props.deleteRoute(payload);
    }
  }

  render() {
    console.log("<Routes /> props ", this.props);
    const route = this.props.deletePayload;
    const { name, type } = route;
    const deleteInfo1 = "Name : " + name;
    const deleteInfo2 = "Type : " + type;
    const isDeleting = this.props.getDeleteStatus === "started";

    return (
      <div style={{ height: this.props.height, position: "relative" }}>
        <ToolBar pageTitle={"Routes"} />
        <RcStatusIndicator
          id={"addRouteIndicator"}
          clearText={"ADDING ROUTE..."}
          requestStatus={this.props.requestRouteStatus}
        />
        {isDeleting === true && (
          <div
            id="undeployoverlay"
            className={"csfWidgets overlay active black"}
          />
        )}
        {this.props.openConfirmDelete && (
          <AlertDialogConfirm
            title={"Do you want to delete this route?"}
            confirmationText1={deleteInfo1}
            confirmationText2={deleteInfo2}
            confirmationButtonLabel={"DELETE"}
            onClose={this.props.cancelDeleteRoute}
            onConfirm={this.props.confirmDeleteRoute}
          />
        )}
        <div id="routes-row1">
          <div>{"Quick selector:"}</div>
          <div style={{ verticalAlign: "middle" }}>
            <QuickSel
              id={"routeQuickSel"}
              quickSelOptions={this.props.quickSelOptions}
              setQuickSelOption={this.props.setQuickSelOption}
              quickSel={this.props.selectedQuick}
            />
          </div>
          <div style={{ verticalAlign: "middle" }}>
            <Button
              id="addRouteBtn"
              text="ADD ROUTE"
              isCallToAction
              disabled={!this.props.canAddRoute}
              onClick={this.openRouteWizard}
            />
          </div>
        </div>

        <div id="routes-row2" style={{ height: this.getListHeight() }}>
          <RoutesList
            routesListData={this.props.routesListData}
            deleteRoute={this.deleteRoute}
            width={this.props.width}
            functionIds={this.props.functionIds}
            runtimeIds={this.props.runtimeIds}
          />
        </div>
      </div>
    );
  }
}

//
// Specifics Selector for routes view
//
const getQuickSelData = createSelector(
  entitiesSelectors.getRoutes,
  routes => {
    const data = [];
    data.push({ id: "NONE", label: "None", value: "NONE" });

    const comparator = (f1, f2) => {
      const a = f1.id.toUpperCase();
      const b = f2.id.toUpperCase();
      return a > b ? 1 : b > a ? -1 : 0;
    };
    const routechoices = [];
    for (let key in routes) {
      let route = routes[key];
      routechoices.push({ id: key, label: route.name, value: key });
    }

    return data.concat(routechoices.sort(comparator));
  }
);

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function(state) {
  return {
    canAddRoute: routeSelectors.canAddRoute(state),
    routesListData: entitiesSelectors.getAllRoutes(state),
    quickSelOptions: getQuickSelData(state),
    selectedQuick: routeSelectors.getSelectedQuick(state),
    requestRouteStatus: routeSelectors.getRouteRequestState(state),
    openConfirmDelete: routeSelectors.doConfirmDeleteRoute(state),
    deletePayload: routeSelectors.getDeletePayload(state),
    getDeleteStatus: routeSelectors.getDeleteStatus(state),
    functionIds: entitiesSelectors.getFunctionIds(state),
    runtimeIds: entitiesSelectors.getRuntimeIds(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      ...routeOperations,
      pollStopRoutes: entitiesOperations.pollStopRoutes
    },
    dispatch
  );
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(Routes)
);
