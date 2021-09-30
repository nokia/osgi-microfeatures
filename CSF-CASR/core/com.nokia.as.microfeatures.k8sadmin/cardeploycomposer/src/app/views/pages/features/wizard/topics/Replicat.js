import React, { Component } from "react";
import PropTypes from "prop-types";

import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";
import Spinner from "@nokia-csf-uxr/csfWidgets/spinner/Spinner";

//import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Replicat extends Component {
  constructor(props) {
    super(props);
    this.errors = {};
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for Replicat )
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Replicat componentWillMount props", this.props);
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
    console.log("Replicat did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("Replicat componentWillUnmount", this.globalErrors);
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
  // REPLICAT. DESCRIPTOR MANAGMENT
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
    this.errors = {};
  };

  validate = () => {
    console.log("<Replicat validate()");
    const validation = {
      errors: [],
      warnings: []
    };
    return validation;
  };

  isCompleteElement = elem => true;

  //
  // RENDERING METHODS
  //

  renderElement = elem => {
    const { replicas } = elem;

    return (
      <div style={{ paddingTop: "12px" }}>
        <div>
          <div style={{display: 'inline-block'}}>
            <Spinner
              id="replicas"
              label={"Number of replicas"}
              min={0}
              max={100}
              readOnly
              step={1}
              value={replicas}
              onChange={data => {
                const newElem = { ...elem };
                newElem.replicas = data.value;
                this.updateElement(newElem);
              }}
              labelHasHelpIcon
              labelHelpIconTooltipProps={{ text: "If 0, then a port should be defined as 'external'."}}
            />
          </div>
          <div className={"inlinelabel"}>
            <Label text="Replicas : Number of pods to create." />
          </div>
        </div>
      </div>
    );
  };

  render() {
    console.log("Replicat render() props", this.props, this.state);

    return (
      <div>
        <div>{this.renderElement(this.props.element)}</div>
{ /*
        <div className={"colorErrorList"}>{generateFormError([])}</div>
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
}

Replicat.propTypes = {
  onValidate: PropTypes.func,
  element: PropTypes.shape({
    replicas: PropTypes.number.isRequired
  }).isRequired,
  updateElement: PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired,
};

Replicat.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Replicat;
