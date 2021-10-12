/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

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
    this.globalErrors = { ...this.props.storageErrors };
    console.log("componentWillMount globalErrors", this.globalErrors);
  }

  componentDidMount() {
    console.log("Identity did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }

    // Update the complete status in according to the reset field
    this.updateElement(this.props.element);
  }

  componentWillUnmount() {
    console.log("Identity componentWillUnmount", this.globalErrors);
    // Save only the warnings messages before navigating to other form
    const { functionIdError, ...warnings } = this.globalErrors;
    console.log("componentWillUnmount", warnings);
    this.props.updateStorageErrors(warnings);
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
  // IDENTITY. DESCRIPTOR MANAGMENT
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
      nameError: ""
    };
  };

  validate = () => {
    console.log("<Identity validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    // After checking, element will be updated for trimable fields
    const newElem = { ...this.props.element };

    const { name } = this.props.element;
    const nameTrimed = name.trim();
    newElem.name = nameTrimed;

    this.errors.nameError = "";

    if (!nameTrimed) {
      this.errors.nameError = "Function name is required";
      validation.errors.push(this.errors.nameError);
    } else if (/^[a-z](-?[a-z0-9])*$/.test(nameTrimed) === false) {
      this.errors.nameError =
        "Should start by letter, no space, no uppercase. Letter/Number/Hyphen allowed.";
      validation.errors.push(this.errors.nameError);
    }

    // Check if the function identifier ( name ) is already used
    if (this.errors.nameError === "") {
      this.globalErrors.functionIdError = this.checkFunctionid(nameTrimed);
      if (this.globalErrors.functionIdError !== "")
        validation.errors.push(this.globalErrors.functionIdError);
    }

    // Update this element
    this.updateElement(newElem);

    console.log(
      "<Identity validate() return validation",
      this.errors,
      this.globalErrors,
      validation
    );
    return validation;
  };

  isCompleteElement = elem => elem.name.length > 0;

  //
  // RENDERING METHODS
  //

  renderElement = elem => {
    const { nameError } = this.errors;
    const { name } = elem;

    return (
      <div style={{ paddingTop: "12px" }}>
        <div className="row">
          <div className="col-sm-4">
            <TextInput
              id="name"
              required
              focus
              autoComplete={'off'}
              label="Function name"
              text={name}
              errorMsg={nameError}
              error={!!nameError}
              onChange={data => {
                const newElem = { ...elem };
                newElem.name = data.value;
                this.updateElement(newElem);
              }}
            />
          </div>
        </div>
      </div>
    );
  };

  render() {
    console.log("Identity render() props", this.props, this.state);
    const { functionIdError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderElement(this.props.element)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([functionIdError])}
        </div>
        {/*
        <div className={"colorWarningList"}>
          {generateFormError([functionFunctionIdError])}
        </div>
*/}
      </div>
    );
  }

  //
  // TOOLS
  //
  checkFunctionid = name => {
    const functionId = name;
    // Check if this id is not alreay used
    const ids = this.props.functionIds;
    console.log("ids", ids);
    return ids.includes(functionId) === true
      ? "This function (" + functionId + ") is already created. Change name."
      : "";
  };
}

Identity.propTypes = {
  onValidate: PropTypes.func,
  element: PropTypes.shape({
    name: PropTypes.string.isRequired
  }).isRequired,
  updateElement: PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired,
  functionIds: PropTypes.arrayOf(PropTypes.string).isRequired
};

Identity.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Identity;
