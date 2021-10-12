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
import {
  runtimeSelectors,
  runtimeOperations
} from "../../../state/ducks/runtime";
import { entitiesSelectors } from "../../../state/ducks/entities";
import { getAllSelectedFeatures } from "./index";

import VerticalStepWizard from "@nokia-csf-uxr/csfWidgets/VerticalStepWizard";
import { getObrVersion, getObrRepository } from "../../../utilities/utils";

 import TopicsForm from "../../commons/wizard/TopicsForm";
 import Summary from './wizard/Summary';
import { Identity, Replicat, Configs, Envs, Files, Overrides, PodLabels, Ports, Prometheus, Secrets } from "./wizard/topics";

class DeployWizard extends Component {
  constructor(props) {
    super(props);
    console.log("<DeployWizard /> props", props);
  }

  componentWillMount() {
    console.log("componentWillMount");
    // Set the runtime state with the current obr version
    this.props.updateDeployForm({ repository: getObrRepository(this.props.obr), version: getObrVersion(this.props.obr) });
  }

  componentDidMount() {
    console.log("componentDidMount ");
  }

  // - Props Changes -
  componentWillReceiveProps(nextProps) {
    console.log("componentWillReceiveProps", nextProps);
  }

  componentWillUnmount() {
    console.log("DeployWizard UNMOUNTING...");
  }

  /**
   * Runtime deploy callback
   */
  deployConfiguredRuntime = () => {
    console.log("deployConfiguredRuntime");
    this.props.deployRuntime();
  };

  handler = data => {
    this.forceUpdate();
  };

  onExit = () => {
    this.props.fullResetDeploy();
    this.props.history.goBack();
  };

  onFinish = () => {
    this.deployConfiguredRuntime();
    this.props.history.goBack();
  };

  //
  // Update methods for all topics
  //
  updateIdentity = (elem, complete) => {
    this.props.updateDeployForm({
      ...elem,
      completes: { identity: complete }
    });
  }

  updateReplicat = (elem, complete) => {
    this.props.updateDeployForm({
      ...elem,
      completes: { replicat: complete }
    });
  }

  updateConfigs = (list, complete) => {
    this.props.updateDeployForm({
      configMap: list,
      completes: { configs: complete }
    });
  };

  updateEnvs = (list, complete) => {
    this.props.updateDeployForm({
      envsList: list,
      completes: { envs: complete }
    });
  };

  updateFiles = (list, complete) => {
    this.props.updateDeployForm({
      filesList: list,
      completes: { files: complete }
    });
  };

  updateOverrides = (list, complete) => {
    this.props.updateDeployForm({
      overridesList: list,
      completes: { overrides: complete }
    });
  };

  updatePodLabels = (list, complete) => {
    this.props.updateDeployForm({
      podLabelsList: list,
      completes: { podLabels: complete }
    });
  };

  updatePorts = (list, complete) => {
    this.props.updateDeployForm({
      portslist: list,
      completes: { ports: complete }
    });
  };

  updatePrometheus = (list, complete) => {
    this.props.updateDeployForm({
      prometheusList: list,
      completes: { prometheus: complete }
    });
  };

  updateSecrets = (list, complete) => {
    this.props.updateDeployForm({
      secretsList: list,
      completes: { secrets: complete }
    });
  };

  /**
   * Instantiate all topics which will be added to forms
   * A topic is a part of the whole configuration to deploy a runtime.
   * Forms define wizard steps.
   */
  buildTopics = () => {
    const { name, namespace, replicas } = this.props.deployForm;
  this.identity = (
      <Identity
        updateElement={this.updateIdentity}
        element={{ name, namespace }}
        runtimeIds={this.props.runtimeIds}
      />
    );

    this.replicat = (
      <Replicat
        updateElement={this.updateReplicat}
        element={{ replicas }}
      />
    );

    this.configs = (
      <Configs
        updateElements={this.updateConfigs}
        elements={this.props.deployForm.configMap}
      />
    );

    this.envs = (
      <Envs
        updateElements={this.updateEnvs}
        elements={this.props.deployForm.envsList}
      />
    );

    this.files = (
      <Files
        updateElements={this.updateFiles}
        elements={this.props.deployForm.filesList}
      />
    );

    this.overrides = (
      <Overrides
        updateElements={this.updateOverrides}
        elements={this.props.deployForm.overridesList}
      />
    );

    this.podLabels = (
      <PodLabels
        updateElements={this.updatePodLabels}
        elements={this.props.deployForm.podLabelsList}
      />
    );

    this.ports = (
      <Ports
        updateElements={this.updatePorts}
        elements={this.props.deployForm.portslist}
        hasExternalPort={this.props.hasExternalPort }
        replicas={replicas}
      />
    );

    this.prometheus = (
      <Prometheus
        updateElements={this.updatePrometheus}
        elements={this.props.deployForm.prometheusList}
      />
    );

    this.secrets = (
      <Secrets
        updateElements={this.updateSecrets}
        elements={this.props.deployForm.secretsList}
      />
    );
  };

  render() {
    console.log("<DeployWizard /> props", this.props, this.state);
    // Update topics
    this.buildTopics();

    const { completes } = this.props.deployForm;

    const form1complete = completes.identity;
    this.form1 = (
      <TopicsForm key={"form1"}
        id={"form1"}
        title="Identity"
        description=" Runtime identifier"
        storageErrors={this.props.storageErrors}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.identity]}
      />
    );

    const form2complete = completes.replicat && completes.podLabels && completes.envs;
    this.form2 = (
      <TopicsForm key={"form2"}
        id={"form2"}
        title="Pod configuration"
        description=" [Optional] Replicas/Labels/Environment variables"
        storageErrors={this.props.storageErrors}
        updateDeployForm={this.props.updateDeployForm}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.replicat, this.podLabels, this.envs]}
        completes={completes}
      />
    );

    const form3complete = completes.ports;
    this.form3 = (
      <TopicsForm key={"form3"}
        id={"form3"}
        title="Ports configuration"
        description=" [Optional] Runtime ports definition"
        storageErrors={this.props.storageErrors}
        updateDeployForm={this.props.updateDeployForm}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.ports]}
        completes={completes}
      />
    );

    const form4complete = completes.overrides && completes.files && completes.configs;
    this.form4 = (
      <TopicsForm key={"form4"}
        id={"form4"}
        title="CASR configuration"
        description=" [Optional] Override Props/Files/Map"
        storageErrors={this.props.storageErrors}
        updateDeployForm={this.props.updateDeployForm}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.overrides,this.files,this.configs]}
        completes={completes}
      />
    );

    const form5complete = completes.files && completes.configs && completes.secrets && completes.prometheus;
    this.form5 = (
      <TopicsForm key={"form5"}
        id={"form5"}
        title="Platform integration"
        description=" [Optional] TlsSecret/Prometheus"
        storageErrors={this.props.storageErrors}
        updateDeployForm={this.props.updateDeployForm}
        updateStorageErrors={this.props.updateStorageErrors}
        children={[this.secrets, this.prometheus]}
        completes={completes}
      />
    );

    return (
      <div id="deployStepWizard">
        <VerticalStepWizard
          id="basicWizard"
          title={"Runtime deployment"}
          continueBtnText={"CONTINUE"}
          backBtnText={"BACK"}
          finishBtnText={"DEPLOY"}
          onFinish={this.onFinish}
          exitTitle={"Are you sure you want to exit the step form?"}
          exitMsg={
            "All current information will be lost if you exit before finishing."
          }
          onExit={this.onExit}
          exitExitBtnText={"EXIT"}
          exitCancelBtnText={"CANCEL"}
          children={[
            this.form1,
            this.form2,
            this.form3,
            this.form4,
            this.form5
          ]}
          continueBtnDisabledArray={[
            !form1complete,
            !form2complete,
            !form3complete,
            !form4complete,
            !form5complete
          ]}
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
    deployForm: runtimeSelectors.getDeployForm(state),
    hasExternalPort : runtimeSelectors.hasExternalPort(state),
    storageErrors: runtimeSelectors.getDeployFormErrors(state),
    obr: entitiesSelectors.getSelectedObr(state),
    runtimeIds: entitiesSelectors.getRuntimeIds(state),
    allSelectedFeatures: getAllSelectedFeatures(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      fullResetDeploy: runtimeOperations.fullResetDeploy,
      updateStorageErrors: runtimeOperations.setFormErrors,
      updateDeployForm: runtimeOperations.presetDeploy,
      deployRuntime: runtimeOperations.deployRuntime
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeployWizard);

