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
import SelectItem from "@nokia-csf-uxr/csfWidgets/SelectItemNew/SelectItemNew";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class MainConfig extends Component {
  constructor(props) {
    super(props);
    this.errors = {};
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for MainConfig )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("MainConfig componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errors = {};

    this.prepareErrorResourcesForElement(this.props.element);

    // Retrieves warning message from global state.
    // This allows to keep warnings while navigate accross wizard pages.
    this.globalErrors = { ...this.props.storageErrors };
    console.log("componentWillMount globalErrors", this.globalErrors);
  }

  componentDidMount() {
    console.log("MainConfig did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }

    // Update the complete status in according to the reset field
    this.updateElement(this.props.element);
  }

  componentWillUnmount() {
    console.log("MainConfig componentWillUnmount", this.globalErrors);
    // Save only the warnings messages before navigating to other form
    const { routeIdError, ...warnings } = this.globalErrors;
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
      typeError: "",
      pathError: "",
      functionIdError: ""
    };
  };

  validate = () => {
    console.log("<MainConfig validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    // After checking, element will be updated for trimable fields
    const newElem = { ...this.props.element };

    const { name, type, path, functionId } = this.props.element;
    const nameTrimed = name.trim();
    const typeTrimed = type.trim();
    const pathTrimed = path.trim();
    const functionIdTrimed = functionId.trim();

    newElem.name = nameTrimed;
    newElem.type = typeTrimed;
    newElem.path = pathTrimed;
    newElem.functionId = functionIdTrimed;

    this.errors.nameError = "";

    if (!nameTrimed) {
      this.errors.nameError = "Route name is required";
      validation.errors.push(this.errors.nameError);
    } else if (/^[a-z](-?[a-z0-9])*$/.test(nameTrimed) === false) {
      this.errors.nameError =
        "Should start by letter, no space no uppercase. Letter/Number/Hyphen allowed.";
      validation.errors.push(this.errors.nameError);
    }

    this.errors.typeError = "";

    if (!typeTrimed) {
      this.errors.typeError = "Route type is required";
      validation.errors.push(this.errors.typeError);
    } else if (/^[a-zA-Z](-?[a-zA-Z0-9])*$/.test(typeTrimed) === false) {
      this.errors.typeError =
        "Should start by letter, no space. Letter/Number/Hyphen allowed.";
      validation.errors.push(this.errors.typeError);
    }

    this.errors.pathError = "";

    if (!pathTrimed) {
      this.errors.pathError = "Route path is required";
      validation.errors.push(this.errors.pathError);
    } else if (/^\S*$/.test(pathTrimed) === false) {
      this.errors.pathError = "Space not allowed.";
      validation.errors.push(this.errors.pathError);
    }

    this.errors.functionIdError = "";

    if (!functionIdTrimed) {
      this.errors.functionIdError = "Route function name is required";
      validation.errors.push(this.errors.functionIdError);
    } else if (/^[a-z](-?[a-z0-9])*$/.test(functionIdTrimed) === false) {
      this.errors.functionIdError = "Should refers an existing function name.";
      validation.errors.push(this.errors.functionIdError);
    } else {
      if (this.isUnknownFunction(functionIdTrimed) === true) {
        // Warn the user
        this.globalErrors.routeFunctionIdError =
          "Function name not found! Should be created later in the Function view.";
        validation.warnings.push(this.globalErrors.routeFunctionIdError);
      } else {
        this.globalErrors.routeFunctionIdError = "";
      }
    }

    // Check if the route identifier ( name ) is already used
    if (this.errors.nameError === "") {
      this.globalErrors.routeIdError = this.checkRouteid(
        nameTrimed,
        typeTrimed
      );
      if (this.globalErrors.routeIdError !== "")
        validation.errors.push(this.globalErrors.routeIdError);
    }

    // Update this element
    this.updateElement(newElem);

    console.log(
      "<MainConfig validate() return validation",
      this.errors,
      this.globalErrors,
      validation
    );
    return validation;
  };

  isCompleteElement = elem => {
    return (
      elem.name.length > 0 &&
      elem.type.length > 0 &&
      elem.path.length > 0 &&
      elem.functionId.length > 0
    );
  };

  //
  // RENDERING METHODS
  //

  renderElement = elem => {
    const { nameError, typeError, pathError, functionIdError } = this.errors;
    const { name, type, path, functionId } = elem;

    const functionOptions = this.buildFunctionOptions(functionId);

    return (
      <div style={{ paddingTop: "12px" }}>
        <TextInput
          id="name"
          name="name"
          required
          focus
          autoComplete={'off'}
          label="Route name"
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
          id="type"
          required
          autoComplete={'off'}
          label="Route type"
          text={type}
          errorMsg={typeError}
          error={!!typeError}
          onChange={data => {
            const newElem = { ...elem };
            newElem.type = data.value;
            this.updateElement(newElem);
          }}
        />
        <TextInput
          id="path"
          required
          autoComplete={'off'}
          label="Route path"
          text={path}
          errorMsg={pathError}
          error={!!pathError}
          onChange={data => {
            const newElem = { ...elem };
            newElem.path = data.value;
            this.updateElement(newElem);
          }}
        />
        <SelectItem
          id={"functionid"}
          isRequired
          allowCreate
          searchable
          errorMsg={functionIdError}
          error={!!functionIdError}
          options={functionOptions}
          selectedItem={functionId}
          name={"functionid"}
          onChange={data => {
            console.log("renderElement SelectItem data", data);
            const newElem = { ...elem };
            let value = newElem.value;
            if (data.type === "onAdd") value = data.value.value;
            else value = data.value;
            if (value !== null) {
              newElem.functionId = value;
              this.updateElement(newElem);
            }
          }}
          onBlurResetsInput={false}
          label={"Function name"}
          labelHasHelpIcon
          labelHelpIconTooltipProps={{ text: 'Select an exiting function or enter a new function name (no space, no uppercase) to create later.'}}
          isValidNewOption={this.checkNewFunctionOption}
        />
      </div>
    );
  };

  render() {
    console.log("MainConfig render() props", this.props, this.state);
    const { routeFunctionIdError, routeIdError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderElement(this.props.element)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([routeIdError])}
        </div>
        <div className={"colorWarningList"}>
          {generateFormError([routeFunctionIdError])}
        </div>
      </div>
    );
  }

  //
  // TOOLS
  //
  checkRouteid = name => {
    const routeId = name;
    // Check if this id is not alreay used
    const ids = this.props.routeIds;
    console.log("ids", ids);
    return ids.includes(routeId) === true
      ? "This route (" + routeId + ") is already created. Change name."
      : "";
  };

  buildFunctionOptions = id => {
    //check if the function identifier is a new one
    // to add it as a new option among existing functions
    let options = [].concat(this.props.functionOptions);
    if (id !== "" && this.isUnknownFunction(id) === true)
      options = options.concat([{ id: id, label: id, value: id }]);
    return options;
  };

  isUnknownFunction = id => {
    const found = this.props.functionIds.includes(id);
    console.log("isUnknownFunction", id, this.props.functionIds, found);
    return !found;
  };

  checkNewFunctionOption = newoption => {
    return /^[a-z](-?[a-z0-9])*$/.test(newoption);
  };
}

MainConfig.propTypes = {
  onValidate: PropTypes.func,
  element: PropTypes.shape({
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired,
    functionId: PropTypes.string.isRequired
  }).isRequired,
  updateElement: PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired,
  routeIds: PropTypes.arrayOf(PropTypes.string).isRequired,
  functionIds: PropTypes.arrayOf(PropTypes.string).isRequired,
  functionOptions: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired
    })
  ).isRequired
};

MainConfig.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default MainConfig;
