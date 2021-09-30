import React, { Component } from "react";
import PropTypes from "prop-types";

import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Identity extends Component {
  constructor(props) {
    super(props);
    this.errors = {};
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for Identity )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Identity componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errors = {};

    this.prepareErrorResourcesForElement(this.props.element);

    // Retrieves warning message from global state.
    // This allows to keep warnings while navigate accross wizard pages.
    this.globalErrors = {}; // { ...this.props.storageErrors };
    console.log("componentWillMount globalErrors", this.globalErrors);

    // Update the complete status in according to the reset field
    this.updateElement(this.props.element);
  }

  componentDidMount() {
    console.log("Identity did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("Identity componentWillUnmount", this.globalErrors);
    // Save only the warnings messages before navigating to other form
    //const { routeIdError, ...warnings } = this.globalErrors;
    //console.log("componentWillUnmount", warnings);
    //this.props.updateStorageErrors(warnings);
    // Clean
    this.errors = {};
  }

  //
  // Save the Main Config list in Redux and its complete status
  //
  saveData = (element, complete) => {
    this.props.updateElement(element, complete);
  };

  //
  // MAINCONFIG. DESCRIPTOR MANAGMENT
  //
  updateElement = element => {
    // Check the complete status of the form and update it
    const complete = this.isCompleteElement(element);
    this.saveData(element, complete);
  };

  //
  // VALIDATION - ERROR MANAGMENT
  //

  /**
   * Create / reset error resources for an element descriptor
   */
  prepareErrorResourcesForElement = element => {
    this.errors = {
      nameError: "",
      namespaceError: "",
      ridError: ""
    };
  };

  validate = () => {
    console.log("<Identity validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    const { name, namespace } = this.props.element;
    const nameTrimed = name.trim();
    const namespaceTrimed = namespace.trim();

    this.errors.nameError = "";

    if (!nameTrimed) {
      this.errors.nameError = "Route name is required";
      validation.errors.push(this.errors.nameError);
    } else if (/^[a-z](-?[a-z0-9])*$/.test(nameTrimed) === false) {
      this.errors.nameError =
        "Should start by letter, no space, no uppercase. Letter/Number/Hyphen allowed.";
      validation.errors.push(this.errors.nameError);
    }

    this.errors.namespaceError = "";

    if (!namespaceTrimed) {
      this.errors.namespaceError = "Route namespace is required";
      validation.errors.push(this.errors.namespaceError);
    } else if (/^[a-z](-?[a-z0-9])*$/.test(namespaceTrimed) === false) {
      this.errors.namespaceError = "Should start by letter, no space, no uppercase. Letter/Number/Hyphen allowed.";
      validation.errors.push(this.errors.namespaceError);
    }

    // Check if the runtime identifier ( name@namespace ) is already used
    this.errors.ridError = "";
    if (this.errors.nameError === "" && this.errors.namespaceError === "") {
      this.errors.ridError = this.checkRid(nameTrimed, namespaceTrimed);
      if (this.errors.ridError !== "")
        validation.errors.push(this.errors.ridError);
    }

    console.log(
      "<Identity validate() return validation",
      this.errors,
      this.globalErrors,
      validation
    );
    return validation;
  };

  isCompleteElement = elem => elem.name.length > 0 && elem.namespace.length > 0;

  //
  // RENDERING METHODS
  //

  renderElement = elem => {
    const { nameError, namespaceError } = this.errors;
    const { name, namespace } = elem;

    return (
      <div style={{ paddingTop: "12px" }}>
        <TextInput
          id="rutname"
          required
          focus
          autoComplete={'off'}
          label="Runtime name"
          text={name}
          errorMsg={nameError}
          error={!!nameError}
          onChange={data => {
            const newElem = { ...elem };
            newElem.name = data.value;
            this.updateElement(newElem);
          }}
        />
        <TextInput
          id="namespace"
          required
          disabled
          label="Runtime namespace (Future use)"
          text={namespace}
          errorMsg={namespaceError}
          error={!!namespaceError}
          onChange={data => {
            const newElem = { ...elem };
            newElem.namespace = data.value;
            this.updateElement(newElem);
          }}
        />
      </div>
    );
  };

  render() {
    console.log("Identity render() props", this.props, this.state);
    const { ridError } = this.errors;

    return (
      <div>
        <div>{this.renderElement(this.props.element)}</div>
        <div className={"colorErrorList"}>{generateFormError([ridError])}</div>
        {/*
        <div className={"colorWarningList"}>
          {generateFormError([])}
        </div>
*/}
      </div>
    );
  }

  //
  // TOOLS
  //
  checkRid = (name, namespace) => {
    const deployId = name + "@" + namespace;
    // Check if this id is not alreay used
    const ids = this.props.runtimeIds;
    console.log("ids", ids);
    return ids.includes(deployId) === true
      ? "This runtime (" +
          deployId +
          ") is already deployed. Change name."
      : "";
  };
}

Identity.propTypes = {
  onValidate: PropTypes.func,
  element: PropTypes.shape({
    name: PropTypes.string.isRequired,
    namespace: PropTypes.string.isRequired
  }).isRequired,
  updateElement: PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired,
  runtimeIds: PropTypes.arrayOf(PropTypes.string).isRequired
};

Identity.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Identity;
