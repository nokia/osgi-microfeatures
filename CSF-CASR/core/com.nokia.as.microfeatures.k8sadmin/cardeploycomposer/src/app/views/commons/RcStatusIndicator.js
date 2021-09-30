import React, { Component } from 'react';
import StatusIndicator from '@nokia-csf-uxr/csfWidgets/StatusIndicator';
import PropTypes from 'prop-types';

class RcStatusIndicator extends Component {

    static defaultProps = {
        id: 'statusIndicator',
        clearText : 'OPERATION DONE',
        statusText: 'Operation pending...',
        requestStatus : null
    }
    static propTypes = {
        id : PropTypes.string.isRequired,
        clearText: PropTypes.string.isRequired,
        requestStatus: PropTypes.oneOf(['started', 'ok','error',null])
    }

    constructor(props) {
        super(props)
        this.state = { status: 'closed' }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.requestStatus && nextProps.requestStatus === 'ok')
            this.setState({ status: 'cleared' })
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.props.requestStatus &&
            this.props.requestStatus === 'started' &&
            nextProps.requestStatus &&
            nextProps.requestStatus === 'ok'
        )
            return true
        return false
    }

    render() {
 //       console.log('render StatusIndicator!', this.state.status)
        return (
            <StatusIndicator
                id={this.props.id}
                clearText={this.props.clearText}
                status={this.state.status}
                statusText={this.props.statusText}
            />
        )
    }

}
export default RcStatusIndicator