/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import PropTypes from "prop-types";
import uniq from "lodash/uniq";

import Button from "@nokia-csf-uxr/csfWidgets/Button";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";

import RcSelectItemNew from "../../../../commons/RcSelectItemNew";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Runtimes extends Component {
  constructor(props) {
    super(props);
    this.errorsByIdx = [];
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for runtimes )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Runtimes componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errorsByIdx = [];

    this.props.elements.forEach(item => {
      this.prepareErrorResourcesForItem(item.id);
    });

    // Retrieve form errors from global state.
    // This allows to keep warnings while navigate accross wizard forms.
    this.globalErrors = {}; // { ...this.props.storageErrors };
  }

  componentDidMount() {
    console.log("Runtimes did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("Runtimes componentWillUnmount");
    // Save the warnings before navigating to other pages
    // const { runtimeNamesError, ...warnings } = this.globalErrors;
    // this.props.updateStorageErrors(warnings);
    // Clean
    this.errorsByIdx = [];
  }

  //
  // Save the Prometheus list in Redux and its complete status
  //
  saveData = (list, complete) => {
    this.props.updateElements(list, complete);
  };

  //
  // TLS RUNTIME DESCRIPTOR MANAGMENT
  //
  addItem = () => {
    // Generate an unic identifier for this new item
    const id =
      Date.now() +
      Math.random()
        .toString()
        .slice(2);

    // Add error resources for errors management
    this.prepareErrorResourcesForItem(id);

    const item = {
      id: id,
      name: ""
    };

    // Update the list of file descriptor in the global state
    const newList = this.props.elements.concat(item);
    this.saveData([...newList], false);
  };

  deleteItem = e => {
    const { id } = e.data;
    const pruneds = this.props.elements.filter(item => item.id !== id);

    // Check the complete status of the form and update it
    const complete = this.isCompleteItems(pruneds);
    this.saveData([...pruneds], complete);
  };

  updateItem = item => {
    const index = this.props.elements.findIndex(elem => elem.id === item.id);
    const newList = Object.assign([], this.props.elements);
    newList[index] = item;
    // Check the complete status of the form and update it
    const complete = this.isCompleteItems(newList);
    this.saveData([...newList], complete);
  };

  //
  // VALIDATION - ERROR MANAGMENT
  //

  /**
   * Create / reset error resources for a runtime descriptor
   */
  prepareErrorResourcesForItem = id => {
    this.errorsByIdx[id] = {
      nameError: ""
    };
  };

  /**
   * Called by Wizard when the user is moving to the next step ( Button Continue clicked )
   */
  validate = () => {
    console.log("<Runtimes validate()");
    const validation = {
      errors: [],
      warnings: []
    };
    //
    // Perform a validation on each runtime descriptor
    //
    this.props.elements.forEach(item => {
      // Re-initalise Error management for this item
      this.prepareErrorResourcesForItem(item.id);
      // After checking, item will be updated for trimable fields
      const newItem = { ...item };

      const { name } = item;

      const nameTrimed = name.trim();
      newItem.name = nameTrimed;
      if (!nameTrimed) {
        this.errorsByIdx[item.id].nameError = "Runtime identifier is required";
        validation.errors.push(this.errorsByIdx[item.id].nameError);
      } else if (
        /^[a-z](-?[a-z0-9])*@[a-z](-?[a-z0-9])*$/.test(
          nameTrimed
        ) === false
      ) {
        this.errorsByIdx[item.id].nameError =
          "Should refers a runtime identifier ( i.e {name}@namespace ).";
        validation.errors.push(this.errorsByIdx[item.id].nameError);
      }

      // Update the global state for this item
      this.updateItem(newItem);
    });

    // Check runtime identifier against all descriptors
    const runtimeNames = this.props.elements.map(item => item.name.trim());
    const uniqRuntimeNames = uniq(runtimeNames);
    if (runtimeNames.length > uniqRuntimeNames.length) {
      this.globalErrors.runtimeNamesError =
        "Some runtime indentifiers are identicals";
      validation.errors.push(this.globalErrors.runtimeNamesError);
    } else {
      this.globalErrors.runtimeNamesError = "";
    }

    console.log(
      "<Runtimes validate() return validation",
      this.errorsByIdx,
      this.globalErrors,
      validation
    );
    return validation;
  };

  isCompleteItems = items => {
    for (let index = 0; index < items.length; index++) {
      if (this.isCompleteItem(items[index]) === false) return false;
    }
    return true;
  };

  /**
   * Check a runtime descriptor against its fields.
   * @item The item to check
   * @param True means the item is considered as complete
   */
  isCompleteItem = item => {
    return item.name.length > 0;
  };

  //
  // RENDERING METHODS
  //
  renderItem = item => {
    const { nameError } = this.errorsByIdx[item.id];
    const runtimeOptions = this.buildRuntimeOptions(item.name);
    console.log("RUNTIME =>>>>>>>>> renderItem", item, runtimeOptions);
    return (
      <div style={{ paddingTop: "12px" }}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deleteRuntimeBtn${item.id}`}
              tooltip={{ text: "Delete this runtime" }}
              icon={"ic_delete"}
              offset={{ top: "5px" }}
              eventData={{ id: item.id }}
              onClick={this.deleteItem}
            />
          </div>
          <div className="col-sm-4">
            <RcSelectItemNew
              id={`runtimeChoice${item.id}`}
              autoFocus
              allowCreate
              searchable
              errorMsg={nameError}
              error={!!nameError}
              options={runtimeOptions}
              selectedItem={item.name}
              name={`runtimeid${item.id}`}
              onChange={data => {
                const newItem = { ...item };
                let value = newItem.value;
                if (data.type === "onAdd") value = data.value.value;
                else value = data.value;
                if (value !== null) {
                  if (value.indexOf("@") === -1) value += "@namespace";
                  newItem.name = value;
                  this.updateItem(newItem);
                }
              }}
              label={"Runtime identifier ({name}@namespace)"}
              labelHasHelpIcon
              labelHelpIconTooltipProps={{ text: 'Select a targeted runtime or enter a new runtime name (without namespace and uppercase) to create later.'}}
              isValidNewOption={this.checkNewRuntimeOption}
            />
          </div>
        </div>
      </div>
    );
  };

  renderItemList(list, renderfun) {
    const listItems = list.map((item, index) => (
      <li key={item.id} style={{ borderBottom: "1px solid #ccc" }}>
        {renderfun(item)}
      </li>
    ));
    return (
      <ul style={{ listStyleType: "none", marginTop: "0px" }}>{listItems}</ul>
    );
  }

  render() {
    console.log("Runtimes render() props", this.props, this.state);
    const { runtimeNamesError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderItemList(this.props.elements, this.renderItem)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([runtimeNamesError])}
        </div>
        <div>
          <Button
            id="addRuntBtn"
            text="SET RUNTIME"
            isCallToAction
            disabled={this.props.elements.length > 0}
            onClick={this.addItem}
          />
          <div className={"inlinelabel"}>
            <Label id="leg" text="Target runtime  [optional]" />
          </div>
        </div>
      </div>
    );
  }

  //
  // TOOLS
  //
  buildRuntimeOptions = id => {
    //check if the function identifier is a new one
    // to add it as a new option among existing functions
    let options = [].concat(this.props.runtimeOptions);
    if (id !== "" && this.isUnknownRuntime(id) === true)
      options = options.concat([{ id: id, label: id, value: id }]);
    return options;
  };

  isUnknownRuntime = id => {
    const found = this.props.runtimeIds.includes(id);
    console.log("isUnknownFunction", id, this.props.runtimeIds, found);
    return !found;
  };

  checkNewRuntimeOption = newoption => {
    return /^[a-z](-?[a-z0-9])*(@namespace)?$/.test(newoption);
  };
}

Runtimes.propTypes = {
  onValidate: PropTypes.func,
  elements: PropTypes.array,
  updateElements: PropTypes.func,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired,
  runtimeIds: PropTypes.arrayOf(PropTypes.string).isRequired,
  runtimeOptions: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired
    })
  ).isRequired
};

Runtimes.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Runtimes;
