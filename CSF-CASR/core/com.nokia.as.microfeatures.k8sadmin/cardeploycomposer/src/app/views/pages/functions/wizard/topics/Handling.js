import React, { Component } from "react";
import PropTypes from "prop-types";

import CheckBox from "@nokia-csf-uxr/csfWidgets/CheckBox/CheckBox";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";
import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";
//import Spinner from "@nokia-csf-uxr/csfWidgets/spinner/Spinner";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Handling extends Component {
  constructor(props) {
    super(props);
    this.errors = {};
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for Handling )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Handling componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errors = {};

    this.prepareErrorResourcesForElement(this.props.element);

    // Retrieves warning message from global state.
    // This allows to keep warnings while navigate accross wizard pages.
    //  this.globalErrors = { ...this.props.storageErrors };
    console.log("componentWillMount globalErrors", this.globalErrors);
  }

  componentDidMount() {
    console.log("Handling did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }

    // Update the complete status in according to the reset field
    //    this.updateElement(this.props.element);
  }

  componentWillUnmount() {
    console.log("Handling componentWillUnmount", this.globalErrors);
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
    console.log("<Handling validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    // After checking, element will be updated for trimable fields
    const newElem = { ...this.props.element };

    const { timeout } = this.props.element;
    const timeoutTrimed = timeout.trim();
    newElem.timeout = timeoutTrimed;

    this.errors.timeoutError = "";

    if (!timeoutTrimed) {
      this.errors.timeoutError = "Function timeout is required";
      validation.errors.push(this.errors.timeoutError);
    } else if (/^([0-9])+$/.test(timeoutTrimed) === false) {
      this.errors.timeoutError = "Only digits";
      validation.errors.push(this.errors.timeoutError);
    } else {
      // Remove all first zero
      newElem.timeout = Number(timeoutTrimed).toString();
    }

    // Update this element
    this.updateElement(newElem);

    console.log(
      "<Handling validate() return validation",
      this.errors,
      this.globalErrors,
      validation
    );
    return validation;
  };

  isCompleteElement = elem => elem.timeout.length > 0;

  //
  // RENDERING METHODS
  //

  renderElement = elem => {
    const { timeoutError } = this.errors;
    const { lazy, timeout } = elem;

    return (
      <div style={{ paddingTop: "12px" }}>
        <CheckBox
          id={"handlazy"}
          value={lazy}
          label="Lazy (Function Loading Strategy)"
          onChange={data => {
            const newElem = { ...elem };
            newElem.lazy = data.value;
            if (newElem.lazy === false) newElem.timeout = '0';
            this.updateElement(newElem);
          }}
        />
        {lazy === true && (
          <div>
            <ul>
              <div style={{ display: "inline-block" }}>
                <TextInput
                  id={"lazytimeout"}
                  required
                  label="Inactivity timeout"
                  autoComplete={'off'}
                  text={timeout}
                  errorMsg={timeoutError}
                  error={!!timeoutError}
                  onChange={data => {
                    const newElem = { ...elem };
                    newElem.timeout = data.value;
                    this.updateElement(newElem);
                  }}
                />
              </div>
              <div className={"inlinelabel"}>
                <Label text="Timeout in seconds [optional]" />
              </div>
            </ul>
          </div>
        )}
      </div>
    );
  };

  render() {
    console.log("Handling render() props", this.props, this.state);
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

Handling.propTypes = {
  onValidate: PropTypes.func,
  element: PropTypes.shape({
    lazy: PropTypes.bool.isRequired,
    timeout: PropTypes.string.isRequired
  }).isRequired,
  updateElement: PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

Handling.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Handling;
