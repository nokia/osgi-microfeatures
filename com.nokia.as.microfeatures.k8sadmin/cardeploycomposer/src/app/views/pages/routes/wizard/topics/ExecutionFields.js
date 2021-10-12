/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import PropTypes from "prop-types";

import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";
import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class ExecutionFields extends Component {
  constructor(props) {
    super(props);
    this.errors = {};
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for ExecutionFields )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("ExecutionFields componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errors = {};

    this.prepareErrorResourcesForElement(this.props.element);

    // Retrieves warning message from global state.
    // This allows to keep warnings while navigate accross wizard pages.
    //  this.globalErrors = { ...this.props.storageErrors };
    console.log("componentWillMount globalErrors", this.globalErrors);
  }

  componentDidMount() {
    console.log("ExecutionFields did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }

    // Update the complete status in according to the reset field
    //    this.updateElement(this.props.element);
  }

  componentWillUnmount() {
    console.log("ExecutionFields componentWillUnmount", this.globalErrors);
    // Save only the warnings messages before navigating to other form
    //   const { functionIdError, ...warnings } = this.globalErrors;
    //    console.log("componentWillUnmount", warnings);
    //    this.props.updateStorageErrors(warnings);
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
  // HADLING. DESCRIPTOR MANAGMENT
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
    this.errors = { timeoutError: "" };
  };

  validate = () => {
    console.log("<ExecutionFields validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    // After checking, element will be updated for trimable fields
    const newElem = { ...this.props.element };

    const { ttl } = this.props.element;
    const timeoutTrimed = ttl.trim();

    newElem.ttl = timeoutTrimed;

    this.errors.timeoutError = "";

    if (!timeoutTrimed) {
      this.errors.timeoutError = "Execution timeout is required";
      validation.errors.push(this.errors.timeoutError);
    } else if (/^([0-9])+$/.test(timeoutTrimed) === false) {
      this.errors.timeoutError = "Only digits";
      validation.errors.push(this.errors.timeoutError);
    } else {
      // Remove all first zero
      newElem.ttl = Number(timeoutTrimed).toString();
    }

    // Update this element
    this.updateElement(newElem);

    console.log(
      "<ExecutionFields validate() return validation",
      this.errors,
      this.globalErrors,
      validation
    );
    return validation;
  };

  isCompleteElement = elem => elem.ttl.length > 0;

  //
  // RENDERING METHODS
  //

  renderElement = elem => {
    const { timeoutError } = this.errors;
    const { ttl } = elem;

    return (
      <div style={{ paddingTop: "12px" }}>
        <div style={{ display: "inline-block" }}>
          <TextInput
            id={"ttlinput"}
            required
            autoComplete={'off'}
            label="Execution timeout"
            text={ttl}
            errorMsg={timeoutError}
            error={!!timeoutError}
            onChange={data => {
              const newElem = { ...elem };
              newElem.ttl = data.value.trim();
              this.updateElement(newElem);
            }}
          />
        </div>
        <div className={"inlinelabel"}>
          <Label text="Timeout in milliseconds [optional]" />
        </div>
      </div>
    );
  };

  render() {
    console.log("ExecutionFields render() props", this.props, this.state);
    //    const { functionIdError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderElement(this.props.element)}</div>
        <div className={"colorErrorList"}>{generateFormError([])}</div>
        {/*
        <div className={"colorWarningList"}>
          {generateFormError([functionFunctionIdError])}
        </div>
*/}
      </div>
    );
  }
}

ExecutionFields.propTypes = {
  onValidate: PropTypes.func,
  element: PropTypes.shape({
    ttl: PropTypes.string.isRequired
  }).isRequired,
  updateElement: PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

ExecutionFields.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default ExecutionFields;
