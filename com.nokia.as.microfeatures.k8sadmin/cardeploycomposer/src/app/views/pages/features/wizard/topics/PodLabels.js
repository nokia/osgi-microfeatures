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
import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class PodLabels extends Component {
  constructor(props) {
    super(props);
    this.errorsByIdx = [];
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for Pod Label )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("PodLabels componentWillMount props", this.props);
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
    console.log("PodLabels did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("PodLabels componentWillUnmount");
    // Save the warnings before navigating to other pages
    // const { labelNamesError, ...warnings } = this.globalErrors;
    // this.props.updateStorageErrors(warnings);
    // Clean
    this.errorsByIdx = [];
  }

  //
  // Save the PodLabels list in Redux and its complete status
  //
  saveData = (list, complete) => {
    this.props.updateElements(list, complete);
  };

  //
  // POD LABELS DESCRIPTOR MANAGMENT
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
      name: "",
      value: ""
    };

    // Update the list of pod label descriptor in the global state
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
   * Create / reset error resources for a pod label descriptor
   */
  prepareErrorResourcesForItem = id => {
    this.errorsByIdx[id] = {
      nameError: "",
      valueError: ""
    };
  };

  /**
   * Called by Wizard when the user is moving to the next step ( Button Continue clicked )
   */
  validate = () => {
    console.log("<PodLabels validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    //
    // Perform a validation on each pod label descriptor
    //
    this.props.elements.forEach(item => {
      // Re-initalise Error management for this item
      this.prepareErrorResourcesForItem(item.id);
      // After checking, item will be updated for trimable fields
      const newItem = { ...item };

      const { name, value } = item;

      const nameTrimed = name.trim();
      newItem.name = nameTrimed;
      if (!nameTrimed) {
        this.errorsByIdx[item.id].nameError = "Label name is required";
        validation.errors.push(this.errorsByIdx[item.id].nameError);
      } else if (/^[a-zA-Z](-?[a-zA-Z0-9])*$/.test(nameTrimed) === false) {
        this.errorsByIdx[item.id].nameError =
          "Should start by letter, no space. Letter/Number/Hyphen allowed.";
        validation.errors.push(this.errorsByIdx[item.id].nameError);
      }

      const valueTrimed = value.trim();
      newItem.value = valueTrimed;
      if (!valueTrimed) {
        this.errorsByIdx[item.id].valueError = "Label value is required";
        validation.errors.push(this.errorsByIdx[item.id].valueError);
      } else if (/^\S*$/.test(valueTrimed) === false) {
        this.errorsByIdx[item.id].valueError = "No space allowed.";
        validation.errors.push(this.errorsByIdx[item.id].valueError);
      }

      // Update the golbal state for this item
      this.updateItem(newItem);
    });

    // Check pod label name against all descriptors
    const labelNames = this.props.elements.map(item => item.name.trim());
    const uniqLabelNames = uniq(labelNames);
    if (labelNames.length > uniqLabelNames.length) {
      this.globalErrors.labelNamesError = "Some pod label names are identicals";
      validation.errors.push(this.globalErrors.labelNamesError);
    } else {
      this.globalErrors.labelNamesError = "";
    }

    console.log(
      "<PodLabels validate() return validation",
      this.errorsByIdx,
      this.globalErrors,
      validation
    );
    return validation;
  };

  /**
   * Check the complete status for an item list
   */
  isCompleteItems = items => {
    for (let index = 0; index < items.length; index++) {
      if (this.isCompleteItem(items[index]) === false) return false;
    }
    return true;
  };

  /**
   * Check a port descriptor against its fields.
   * @item The item to check
   * @param True means the item is considered as complete
   */
  isCompleteItem = item => {
    return item.name.length > 0 && item.value.length > 0;
  };

  //
  // RENDERING METHODS
  //

  renderItem = item => {
    const { nameError, valueError } = this.errorsByIdx[item.id];

    return (
      <div style={{ paddingTop: "12px" }}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deletePlBtn${item.id}`}
              tooltip={{ text: "Delete this pod label descriptor" }}
              icon={"ic_delete"}
              offset={{ top: "5px" }}
              eventData={{ id: item.id }}
              onClick={this.deleteItem}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`plname${item.id}`}
              required
              focus
              autoComplete={'off'}
              label="Label name"
              text={item.name}
              errorMsg={nameError}
              error={!!nameError}
              onChange={data => {
                const newItem = { ...item };
                newItem.name = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`plvalue${item.id}`}
              required
              autoComplete={'off'}
              label="Label value"
              text={item.value}
              errorMsg={valueError}
              error={!!valueError}
              onChange={data => {
                const newItem = { ...item };
                newItem.value = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
        </div>
      </div>
    );
  };

  renderItemList(list, renderfun) {
    const listItems = list.map(item => (
      <li key={item.id} style={{ borderBottom: "1px solid #ccc" }}>
        {renderfun(item)}
      </li>
    ));
    return (
      <ul style={{ listStyleType: "none", marginTop: "0px" }}>{listItems}</ul>
    );
  }

  render() {
    console.log("PodLabels render() props", this.props, this.state);
    const { labelNamesError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderItemList(this.props.elements, this.renderItem)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([labelNamesError])}
        </div>
        <div>
          <Button
            id="addPlBtn"
            text="ADD POD LABELS"
            isCallToAction
            disabled={false}
            onClick={this.addItem}
          />
          <div className={"inlinelabel"}>
            <Label text="Labels will be added to the pod definition [optional]" />
          </div>
        </div>
      </div>
    );
  }
}

PodLabels.propTypes = {
  onValidate: PropTypes.func,
  updateElements: PropTypes.func.isRequired,
  elements: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired
    })
  ).isRequired,
  storageErrors : PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

PodLabels.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default PodLabels;
