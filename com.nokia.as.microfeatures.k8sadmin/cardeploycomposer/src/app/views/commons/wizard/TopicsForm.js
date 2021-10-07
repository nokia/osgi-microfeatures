import React, { Component } from "react";
import PropTypes from "prop-types";

import FormLayout from "@nokia-csf-uxr/csfWidgets/FormLayout";

import _ from "lodash";

class TopicsForm extends Component {

  constructor(props) {
    super(props);
    this.topics = [];
  }

  componentWillMount() {
    console.log(`${this.props.id} componentWillMount`, this.props);
    React.Children.forEach(this.props.children, (child, i) => {
      this.topics[i] = {
        topic: child
      };
    });
  }

  componentDidMount() {
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUpdate(nextProps, nextState) {
    React.Children.forEach(nextProps.children, (child, i) => {
      this.topics[i].topic = child;
    });
  }

  componentWillUnmount() {
    console.log(`${this.props.id} componentWillUnmount`);
  }

  validate = () => {
    const validation = {
      errors: [],
      warnings: []
    };

    for (let i=0; i < this.topics.length; i++) {
      const step = this.topics[i];
      console.log(`Calling validate step ${i}`, step);
      const { errors, warnings } = step.validate();
      console.log(`validate step ${i} done`, errors, warnings);

      validation.errors = validation.errors.concat(errors);
      validation.warnings = validation.warnings.concat(warnings);
    }

    return validation;
  };

  setOnValidate(i, callback) {
    this.topics[i].validate = callback;
  }

  renderTopics = () => {
    const listTopics = [];
    const { id, storageErrors, updateStorageErrors } = this.props;
    for( let i=0; i < this.topics.length; i++ ) {
      let Topic = React.cloneElement(this.topics[i].topic, {
        onValidate: this.setOnValidate.bind(this, i),
        key: `${id}_topic${i}`,
        updateStorageErrors: updateStorageErrors,
        storageErrors: storageErrors
      });

      listTopics.push(Topic);
      // Add separator between topics
      if( i + 1 < this.topics.length )
        listTopics.push(<div key={`${id}_topicsep${i}` } className={"button-separator"} />)
    };

    return listTopics;
  };

  render() {
    console.log("TopicsForm render() props", this.props, this.state);

    return <FormLayout>{this.renderTopics()}</FormLayout>;
  }
}

TopicsForm.propTypes = {
  id : PropTypes.string.isRequired,
  children : PropTypes.arrayOf(PropTypes.node).isRequired,
  onValidate: PropTypes.func,
  onBack: PropTypes.func,
//  storage : PropTypes.object.isRequired,
//  updateStorage : PropTypes.func.isRequired,
  storageErrors: PropTypes.object.isRequired,
  updateStorageErrors : PropTypes.func.isRequired
};

TopicsForm.defaultProps = {
  id: 'TopicsForm',
  onValidate: _.noop,
  onBack: _.noop
};

export default TopicsForm;
