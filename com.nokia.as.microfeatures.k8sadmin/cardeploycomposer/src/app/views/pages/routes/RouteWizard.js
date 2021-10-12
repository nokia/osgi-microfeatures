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
import { routeSelectors, routeOperations } from "../../../state/ducks/route";
import { viewsConst, viewsActions } from "../../../state/ducks/views";
import { entitiesSelectors } from "../../../state/ducks/entities";

import VerticalStepWizard from "@nokia-csf-uxr/csfWidgets/VerticalStepWizard";

import TopicsForm from "../../commons/wizard/TopicsForm";
import Summary from './wizard/Summary'
import {
  MainConfig,
  Params,
  FunctionParams,
  Runtimes,
  ExecutionFields
} from "./wizard/topics";

class RouteWizard extends Component {
  constructor(props) {
    super(props);
    console.log("<RouteWizard /> props", props);
  }

  componentWillMount() {
    console.log("componentWillMount");
  }

  componentDidMount() {
    console.log("componentDidMount ");
  }

  // - Props Changes -
  componentWillReceiveProps(nextProps) {
    console.log("componentWillReceiveProps", nextProps);
  }

  componentWillUnmount() {
    console.log("RouteWizard UNMOUNTING...");
  }

  handler = data => {
    console.log("ROUTE WIZARD HANDLER FORCE UPDATE!!!! ");
    this.forceUpdate();
  };

  onExit = () => {
    console.log("onExit");
    this.props.fullResetRoute();
    this.props.history.goBack();
    // Set the route view to restart polling
    this.props.setView(viewsConst.views.ROUTES);
  };

  onFinish = () => {
    console.log("onFinish");
    // Add new route
    this.props.addRoute();
    this.props.history.goBack();
    // Set the route view to restart polling
    this.props.setView(viewsConst.views.ROUTES);
  };

  //
  // Update methods for all topics
  //
  updateMainConfig = (elem, complete) => {
    this.props.updateRouteForm({
      ...elem,
      completes: { mainConfig: complete }
    });
  };

  updateParams = (list, complete) => {
    this.props.updateRouteForm({
      paramsList: list,
      completes: { params: complete }
    });
  };

  updateFunctionParams = (list, complete) => {
    this.props.updateRouteForm({
      functionParamsList: list,
      completes: { functionparams: complete }
    });
  };

  updateRuntimes = (list, complete) => {
    this.props.updateRouteForm({
      runtimesList: list,
      completes: { runtimes: complete }
    });
  };

  updateExecutionFields = (elem, complete) => {
    this.props.updateRouteForm({
      ...elem,
      completes: { execution: complete }
    });
  };

  isNewFunctionName = name => {
    if (!!!name || name === "") return false;
    const found = this.props.functionIds.includes(name);
    console.log("isNewFunctionName", name, this.props.functionIds, found);
    return !found;
  };

  /**
   * Instantiate all topics which will be added to forms
   * A topic is a part of the whole configuration to deploy a route.
   * Forms define wizard steps.
   */
  buildTopics = () => {
    const { name, type, path, functionId, ttl } = this.props.routeForm;
    this.mainConfig = (
      <MainConfig
        updateElement={this.updateMainConfig}
        element={{ name, type, path, functionId }}
        routeIds={this.props.routeIds}
        functionIds={this.props.functionIds}
        functionOptions={this.props.functionOptions}
      />
    );

    this.params = (
      <Params
        updateElements={this.updateParams}
        elements={this.props.routeForm.paramsList}
      />
    );

    this.functionParams = (
      <FunctionParams
        updateElements={this.updateFunctionParams}
        elements={this.props.routeForm.functionParamsList}
      />
    );

    this.executionFields = (
      <ExecutionFields
        updateElement={this.updateExecutionFields}
        element={{ ttl }}
      />
    );

    this.runtimes = (
      <Runtimes
        updateElements={this.updateRuntimes}
        elements={this.props.routeForm.runtimesList}
        runtimeIds={this.props.runtimeIds}
        runtimeOptions={this.props.runtimeOptions}
      />
    );
  };

  render() {
    console.log("<RouteWizard /> props", this.props, this.state);
    // Update topics
    this.buildTopics();

    const { completes } = this.props.routeForm;

    const form1complete = completes.mainConfig;
    this.form1 = (
      <TopicsForm
        key={"form1"}
        id={"form1"}
        title="Identity"
        description=" Route definition"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.mainConfig]}
      />
    );

    const form2complete = completes.runtimes && completes.execution;
    this.form2 = (
      <TopicsForm
        key={"form2"}
        id={"form2"}
        title="Execution"
        description=" [Optional] Execution Parameters"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.runtimes, this.executionFields]}
      />
    );

    const form3complete = completes.params && completes.functionparams;
    this.form3 = (
      <TopicsForm
        key={"form3"}
        id={"form3"}
        title="Parameters"
        description=" [Optional] Route/function Parameters"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.params, this.functionParams]}
      />
    );

    return (
      <div id="deployStepWizard">
        <VerticalStepWizard
          id="basicWizard"
          title={"Route creation"}
          continueBtnText={"CONTINUE"}
          backBtnText={"BACK"}
          finishBtnText={"ADD ROUTE"}
          onFinish={this.onFinish}
          exitTitle={"Are you sure you want to exit the step form?"}
          exitMsg={
            "All current information will be lost if you exit before finishing."
          }
          onExit={this.onExit}
          exitExitBtnText={"EXIT"}
          exitCancelBtnText={"CANCEL"}
          children={[this.form1, this.form2, this.form3]}
          continueBtnDisabledArray={[
            !form1complete,
            !form2complete,
            !form3complete
          ]}
          handler={this.handler}
          summary={<Summary {...this.props} />}
        />
      </div>
    );
  }
}

const getRuntimeOptions = createSelector(
  entitiesSelectors.getRuntimeIds,
  ids => {
    const choices = [];
    for (let i = 0; i < ids.length; i++) {
      let id = ids[i];
      choices.push({ id: id, label: id, value: id });
    }
    return choices;
  }
);

const getFunctionOptions = createSelector(
  entitiesSelectors.getFunctionIds,
  ids => {
    const choices = [];
    for (let i = 0; i < ids.length; i++) {
      let id = ids[i];
      choices.push({ id: id, label: id, value: id });
    }
    return choices;
    //     return [{ id: 'fake', label: 'fake', value: 'fake' }].concat(choices);
  }
);

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function(state) {
  return {
    routeForm: routeSelectors.getRouteForm(state),
    storageErrors: routeSelectors.getRouteFormErrors(state),
    routeIds: entitiesSelectors.getRouteIds(state),
    runtimeIds: entitiesSelectors.getRuntimeIds(state),
    runtimeOptions: getRuntimeOptions(state),
    functionOptions: getFunctionOptions(state),
    functionIds: entitiesSelectors.getFunctionIds(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      fullResetRoute: routeOperations.fullResetRoute,
      updateStorageErrors: routeOperations.setFormErrors,
      updateRouteForm: routeOperations.presetRoute,
      addRoute: routeOperations.addRoute,
      ...viewsActions
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RouteWizard);

