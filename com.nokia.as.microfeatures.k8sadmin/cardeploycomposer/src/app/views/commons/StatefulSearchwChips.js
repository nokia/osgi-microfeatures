import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import SearchwChips from '@nokia-csf-uxr/csfWidgets/SearchwChips';

export default class StatefulSearchwChips extends PureComponent {
  static propTypes = {
    onUpdate: PropTypes.func.isRequired
  }
  constructor(props) {
    super(props);
    this.state = {
      chips: []
    };
  }

  handleUpdate = (event) => {
    const chips = event.data;
    this.setState({ chips });
    this.props.onUpdate(chips.map(chipsItem => ({
      queryTerm: chipsItem.text
    })));
  }

  render() {
    const {
      onUpdate,
      ...props
    } = this.props;
    const {
      chips,
    } = this.state;
    return (<SearchwChips
      {...props}
      chips={chips}
      onUpdate={this.handleUpdate}
    />);
  }
}