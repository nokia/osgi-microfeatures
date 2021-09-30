import React, { Component } from "react";
import { bindActionCreators } from "redux";
import { connect } from "react-redux";
import { withRouter } from "react-router-dom";
import { createSelector } from "reselect";

import { functionSelectors, functionOperations } from "../../../state/ducks/function";
import {
  entitiesSelectors,
  entitiesOperations
} from "../../../state/ducks/entities";

import ToolBar from "../../commons/ToolBar";
import FunctionsList from "./FunctionsList";
import QuickSel from "../../commons/QuickSel";
import RcStatusIndicator from "../../commons/RcStatusIndicator";
import Button from "@nokia-csf-uxr/csfWidgets/Button";
import AlertDialogConfirm from "@nokia-csf-uxr/csfWidgets/AlertDialogConfirm";

class Functions extends Component {
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
  openFunctionWizard = () => {
    const { isAdministrator } = this.props.configData;
    const newRoute = (isAdministrator === true)?`/functionwizard`:`/accessDenied`;
    this.props.pollStopFunctions();
    this.props.history.push(newRoute);
  };

  deleteFunction = (payload) => {
    const { isAdministrator } = this.props.configData;
    if (isAdministrator !== true) {
      this.props.pollStopFunctions();
      this.props.history.push(`/accessDenied`);
    } else {
      this.props.deleteFunction(payload);
    }
  }


  render() {
    console.log("<Functions /> props ", this.props);
    const funcTion = this.props.deletePayload;
    const { name } = funcTion;
    const deleteInfo1 = "Name : " + name;
    const deleteInfo2 = '';
    const isDeleting = this.props.deleteStatus === "started";

    return (
      <div style={{ height: this.props.height, position: "relative" }}>
        <ToolBar pageTitle={"Functions"} />
        <RcStatusIndicator
          id={"addFunctionIndicator"}
          clearText={"ADDING FUNCTION..."}
          requestStatus={this.props.requestFunctionStatus}
        />
        {isDeleting === true && (
          <div
            id="undeployoverlay"
            className={"csfWidgets overlay active black"}
          />
        )}
        {this.props.openConfirmDelete && (
          <AlertDialogConfirm
            title={"Do you want to delete this function?"}
            confirmationText1={deleteInfo1}
            confirmationText2={deleteInfo2}
            confirmationButtonLabel={"DELETE"}
            onClose={this.props.cancelDeleteFunction}
            onConfirm={this.props.confirmDeleteFunction}
          />
        )}
        <div id="functions-row1">
        <div>{"Quick selector:"}</div>
        <div style={{ verticalAlign: "middle" }}>
          <QuickSel
            id={"functionQuickSel"}
            quickSelOptions={this.props.quickSelOptions}
            setQuickSelOption={this.props.setQuickSelOption}
            quickSel={this.props.selectedQuick}
          />
        </div>
        <div style={{ verticalAlign: "middle" }}>
          <Button
            id="addFunctionBtn"
            text="ADD FUNCTION"
            isCallToAction
            disabled={!this.props.canAddFunction}
            onClick={this.openFunctionWizard}
          />
        </div>
      </div>
        <div id="functions-row2" style={{ height: this.getListHeight() }}>
          <FunctionsList
            functionsListData={this.props.functionsListData}
            deleteFunction={this.deleteFunction}
            width={this.props.width}
          />
        </div>
      </div>
    );
  }
}

//
// Specifics Selector for functions view
//
const getQuickSelData = createSelector(
  entitiesSelectors.getFunctions,
  functions => {
    const data = [];
    data.push({ id: "NONE", label: "None", value: "NONE" });

    const comparator = (f1, f2) => {
      const a = f1.id.toUpperCase();
      const b = f2.id.toUpperCase();
      return a > b ? 1 : b > a ? -1 : 0;
    };
    const functionchoices = [];
    for (let key in functions) {
      let funcTion = functions[key];
      functionchoices.push({ id: key, label: funcTion.name, value: key });
    }

    return data.concat(functionchoices.sort(comparator));
  }
);

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function(state) {
  return {
    canAddFunction: functionSelectors.canAddFunction(state),
    functionsListData: entitiesSelectors.getAllFunctions(state),
    quickSelOptions: getQuickSelData(state),
    selectedQuick: functionSelectors.getSelectedQuick(state),
    requestFunctionStatus: functionSelectors.getFunctionRequestState(state),
    openConfirmDelete: functionSelectors.doConfirmDeleteFunction(state),
    deletePayload: functionSelectors.getDeletePayload(state),
    deleteStatus: functionSelectors.getDeleteStatus(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      ...functionOperations,
      pollStopFunctions: entitiesOperations.pollStopFunctions
    },
    dispatch
  );
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(Functions)
);
