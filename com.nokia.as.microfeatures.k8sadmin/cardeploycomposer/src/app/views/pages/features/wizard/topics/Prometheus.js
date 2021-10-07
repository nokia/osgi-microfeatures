import React, { Component } from "react";
import PropTypes from "prop-types";
import uniq from "lodash/uniq";

import Button from "@nokia-csf-uxr/csfWidgets/Button";
import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Prometheus extends Component {
  constructor(props) {
    super(props);
    this.errorsByIdx = [];
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for promotheus )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Prometheus componentWillMount props", this.props);
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
    console.log("Prometheus did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("Prometheus componentWillUnmount");
    // Save the warnings before navigating to other pages
    // const { prometheusPortNbrError, ...warnings } = this.globalErrors;
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
  // CONFIG. PROMETHEUS DESCRIPTOR MANAGMENT
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
      port: "",
      path: ""
    };

    // Update the list of prometheus descriptor in the global state
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
   * Create / reset error resources for a promotheus descriptor
   */
  prepareErrorResourcesForItem = id => {
    this.errorsByIdx[id] = {
      portError: "",
      pathError: ""
    };
  };

  /**
   * Called by Wizard when the user is moving to the next step ( Button Continue clicked )
   */
  validate = () => {
    console.log("<Prometheus validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    // Perform validation for prometheus descriptor
    this.props.elements.forEach(item => {
      // Re-initalise Error management for this item
      this.prepareErrorResourcesForItem(item.id);
      // After checking, item will be updated for trimable fields
      const newItem = { ...item };

      const { port, path } = item;

      const portTrimed = port.trim();
      newItem.port = portTrimed;
      if (!portTrimed) {
        this.errorsByIdx[item.id].portError = "Port number is required";
        validation.errors.push(this.errorsByIdx[item.id].portError);
      } else if (/^([0-9])+$/.test(portTrimed) === false) {
        this.errorsByIdx[item.id].portError = "Only digits";
        validation.errors.push(this.errorsByIdx[item.id].portError);
      }

      const pathTrimed = path.trim();
      newItem.path = pathTrimed;
      if (!pathTrimed) {
        this.errorsByIdx[item.id].pathError = "Path is required";
        validation.errors.push(this.errorsByIdx[item.id].pathError);
      } else if (
        /^\/[a-zA-Z0-9]((-|\/)?[a-zA-Z0-9])*$/.test(pathTrimed) === false
      ) {
        this.errorsByIdx[item.id].pathError =
          "Should start by /, no space. Letter/Number/Hyphen allowed";
        validation.errors.push(this.errorsByIdx[item.id].pathError);
      }

      // Update the golbal state for this item
      this.updateItem(newItem);
    });

    // Check port numbers against all descriptors
    const portnumbers = this.props.elements.map(item => item.port.trim());
    const uniqPortNumbers = uniq(portnumbers);
    if (portnumbers.length > uniqPortNumbers.length) {
      this.globalErrors.prometheusPortNbrError =
        "Some port numbers are identicals";
      validation.errors.push(this.globalErrors.prometheusPortNbrError);
    } else {
      this.globalErrors.prometheusPortNbrError = "";
    }

    console.log(
      "<Prometheus validate() return validation",
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
   * Check a promotheus descriptor against its fields.
   * @item The item to check
   * @param True means the item is considered as complete
   */
  isCompleteItem = item => {
    return item.port.length > 0 && item.path.length > 0;
  };

  //
  // RENDERING METHODS
  //
  renderItem = item => {
    const { portError, pathError } = this.errorsByIdx[item.id];
    return (
      <div style={{ paddingTop: "12px" }}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deleteProBtn${item.id}`}
              tooltip={{ text: "Delete this prometheus port descriptor" }}
              icon={"ic_delete_inactive_ports"}
              offset={{ top: "5px" }}
              eventData={{ id: item.id }}
              onClick={this.deleteItem}
            />
          </div>
          <div className="col-sm-2">
            <TextInput
              id={`portPro${item.id}`}
              required
              autoComplete={'off'}
              label="Prometheus Port"
              text={item.port}
              errorMsg={portError}
              error={!!portError}
              onChange={data => {
                const newItem = { ...item };
                newItem.port = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`pathPro${item.id}`}
              required
              autoComplete={'off'}
              label="Prometheus path"
              text={item.path}
              errorMsg={pathError}
              error={!!pathError}
              onChange={data => {
                const newItem = { ...item };
                newItem.path = data.value;
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
    console.log("Prometheus render() props", this.props, this.state);
    const { prometheusPortNbrError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderItemList(this.props.elements, this.renderItem)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([prometheusPortNbrError])}
        </div>
        <div>
          <Button
            id="addProBtn"
            text="SET PROMETHEUS"
            isCallToAction
            disabled={this.props.elements.length > 0}
            onClick={this.addItem}
          />
          <div className={"inlinelabel"}>
            <Label text="Define the Prometheus configuration [optional]" />
          </div>
        </div>
      </div>
    );
  }
}

Prometheus.propTypes = {
  onValidate: PropTypes.func,
  updateElements: PropTypes.func.isRequired,
  elements: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      port: PropTypes.string.isRequired,
      path: PropTypes.string.isRequired
    })
  ).isRequired,
  storageErrors : PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

Prometheus.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Prometheus;
