import React, { Component } from "react";
import PropTypes from "prop-types";
import uniq from "lodash/uniq";

import Button from "@nokia-csf-uxr/csfWidgets/Button";
import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Locations extends Component {
  constructor(props) {
    super(props);
    this.errorsByIdx = [];
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for locations )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Locations componentWillMount props", this.props);
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
    console.log("Locations did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
    // Add a mandatory location if not present
    if (this.props.elements.length < 1) {
      this.addItem();
    } else {
      // Force to update the complete status
      this.updateItem()

    }
  }

  componentWillUnmount() {
    console.log("Locations componentWillUnmount");
    // Save the warnings before navigating to other pages
    // const { locationValuesError, ...warnings } = this.globalErrors;
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
  // TLS LOCATION DESCRIPTOR MANAGMENT
  //
  addItem = doNotSave => {
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
      value: ""
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
    const newList = Object.assign([], this.props.elements);
    if( item ) {
      const index = this.props.elements.findIndex(elem => elem.id === item.id);
      newList[index] = item;
    }
    // Check the complete status of the form and update it
    const complete = this.isCompleteItems(newList);
    this.saveData([...newList], complete);
  };

  //
  // VALIDATION - ERROR MANAGMENT
  //

  /**
   * Create / reset error resources for a location descriptor
   */
  prepareErrorResourcesForItem = id => {
    this.errorsByIdx[id] = {
      valueError: ""
    };
  };

  /**
   * Called by Wizard when the user is moving to the next step ( Button Continue clicked )
   */
  validate = () => {
    console.log("<Locations validate()");
    const validation = {
      errors: [],
      warnings: []
    };
    //
    // Perform a validation on each location descriptor
    //
    this.props.elements.forEach(item => {
      // Re-initalise Error management for this item
      this.prepareErrorResourcesForItem(item.id);
      // After checking, item will be updated for trimable fields
      const newItem = { ...item };

      const { value } = item;

      const valueTrimed = value.trim();
      newItem.value = valueTrimed;
      if (!valueTrimed) {
        this.errorsByIdx[item.id].valueError = "Location value is required";
        validation.errors.push(this.errorsByIdx[item.id].valueError);
      } else if (/^\S*$/.test(valueTrimed) === false) {
        this.errorsByIdx[item.id].valueError = "No space allowed.";
        validation.errors.push(this.errorsByIdx[item.id].valueError);
      }

      // Update the golbal state for this item
      this.updateItem(newItem);
    });

    // Check file value against all descriptors
    const locationValues = this.props.elements.map(item => item.value.trim());
    const uniqLocationValues = uniq(locationValues);
    if (locationValues.length > uniqLocationValues.length) {
      this.globalErrors.locationValuesError = "Some locations are identicals";
      validation.errors.push(this.globalErrors.locationValuesError);
    } else {
      this.globalErrors.locationValuesError = "";
    }

    console.log(
      "<Locations validate() return validation",
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
   * Check a location descriptor against its fields.
   * @item The item to check
   * @param True means the item is considered as complete
   */
  isCompleteItem = item => {
    return item.value.length > 0;
  };

  //
  // RENDERING METHODS
  //
  renderItem = item => {
    const { valueError } = this.errorsByIdx[item.id];

    const disabledDeleteBtn = this.props.elements.length < 2;

    return (
      <div style={{ paddingTop: "12px" }}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deleteLocationBtn${item.id}`}
              tooltip={{ text: "Delete this location" }}
              icon={"ic_delete"}
              offset={{ top: "5px" }}
              eventData={{ id: item.id }}
              onClick={this.deleteItem}
              disabled={disabledDeleteBtn}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`locationvalue${item.id}`}
              required
              autoComplete={'off'}
              label="Location"
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
    console.log("Locations render() props", this.props, this.state);
    const { locationValuesError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderItemList(this.props.elements, this.renderItem)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([locationValuesError])}
        </div>
        <div>
          <Button
            id="addLocBtn"
            text="ADD LOCATION"
            isCallToAction
            disabled={false}
            onClick={this.addItem}
          />
          <div className={"inlinelabel"}>
            <Label text="Paths to the function binaries." />
          </div>
        </div>
      </div>
    );
  }
}

Locations.propTypes = {
  onValidate: PropTypes.func,
  updateElements: PropTypes.func.isRequired,
  elements: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired
    })
  ).isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

Locations.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Locations;
