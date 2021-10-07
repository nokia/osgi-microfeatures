import React, { Component } from "react";
import { bindActionCreators } from "redux";
import { connect } from "react-redux";
import {
  functionSelectors,
  functionOperations
} from "../../../state/ducks/function";
import { viewsConst, viewsActions } from "../../../state/ducks/views";
import { entitiesSelectors } from "../../../state/ducks/entities";

import VerticalStepWizard from "@nokia-csf-uxr/csfWidgets/VerticalStepWizard";

import TopicsForm from "../../commons/wizard/TopicsForm";
import Summary from './wizard/Summary'
import { Identity, Handling, Params, Locations } from "./wizard/topics";

class FunctionWizard extends Component {
  constructor(props) {
    super(props);
    console.log("<FunctionWizard /> props", props);
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
    console.log("FunctionWizard UNMOUNTING...");
  }

  handler = data => {
    this.forceUpdate();
  };

  onExit = () => {
    this.props.fullResetFunction();
    this.props.history.goBack();
    // Set the function view to restart polling
    this.props.setView(viewsConst.views.FUNCTIONS);
  };

  onFinish = () => {
    // Add a new Function
    this.props.addFunction();
    this.props.history.goBack();
    // Set the function view to restart polling
    this.props.setView(viewsConst.views.FUNCTIONS);
  };

  //
  // Update methods for all topics
  //
  updateIdentity = (elem, complete) => {
    this.props.updateFunctionForm({
      ...elem,
      completes: { identity: complete }
    });
  };

  updateHandling = (elem, complete) => {
    this.props.updateFunctionForm({
      ...elem,
      completes: { handling: complete }
    });
  };

  updateLocation = (list, complete) => {
    this.props.updateFunctionForm({
      locationsList: list,
      completes: { locations: complete }
    });
  };

  updateParams = (list, complete) => {
    this.props.updateFunctionForm({
      paramsList: list,
      completes: { params: complete }
    });
  };

  /**
   * Instantiate all topics which will be added to forms
   * A topic is a part of the whole configuration to deploy a function.
   * Forms define wizard steps.
   */
  buildTopics = () => {
    const { name, lazy, timeout } = this.props.functionForm;
    this.identity = (
      <Identity
        updateElement={this.updateIdentity}
        element={{ name }}
        functionIds={this.props.functionIds}
      />
    );

    this.handling = (
      <Handling
        updateElement={this.updateHandling}
        element={{ lazy, timeout }}
      />
    );

    this.locations = (
      <Locations
        updateElements={this.updateLocation}
        elements={this.props.functionForm.locationsList}
      />
    );

    this.params = (
      <Params
        updateElements={this.updateParams}
        elements={this.props.functionForm.paramsList}
      />
    );
  };

  render() {
    console.log("<FunctionWizard /> props", this.props, this.state);
    // Update topics
    this.buildTopics();

    const { completes } = this.props.functionForm;

    const form1complete = completes.identity && completes.locations;
    this.form1 = (
      <TopicsForm
        key={"form1"}
        id={"form1"}
        title="Identity"
        description=" Function definition"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.identity, this.locations]}
      />
    );

    const form2complete = completes.handling;
    this.form2 = (
      <TopicsForm
        key={"form2"}
        id={"form2"}
        title="Handling"
        description=" [Optional] Handling Parameters"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.handling]}
      />
    );

    const form3complete = completes.params;
    this.form3 = (
      <TopicsForm
        key={"form3"}
        id={"form3"}
        title="Parameters"
        description=" [Optional] Function Parameters"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.params]}
      />
    );

    return (
      <div id="deployStepWizard">
        <VerticalStepWizard
          id="basicWizard"
          title={"Function creation"}
          continueBtnText={"CONTINUE"}
          backBtnText={"BACK"}
          finishBtnText={"ADD FUNCTION"}
          onFinish={this.onFinish}
          exitTitle={"Are you sure you want to exit the step form?"}
          exitMsg={
            "All current information will be lost if you exit before finishing."
          }
          onExit={this.onExit}
          exitExitBtnText={"EXIT"}
          exitCancelBtnText={"CANCEL"}
          children={[this.form1, this.form2, this.form3]}
          continueBtnDisabledArray={[!form1complete, !form2complete, !form3complete]}
          handler={this.handler}
          summary={<Summary {...this.props} />}
        />
      </div>
    );
  }
}

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function(state) {
  return {
    functionForm: functionSelectors.getFunctionForm(state),
    storageErrors: functionSelectors.getFunctionFormErrors(state),
    functionIds: entitiesSelectors.getFunctionIds(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      fullResetFunction: functionOperations.fullResetFunction,
      updateStorageErrors: functionOperations.setFormErrors,
      updateFunctionForm: functionOperations.presetFunction,
      addFunction: functionOperations.addFunction,
      ...viewsActions
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(FunctionWizard);