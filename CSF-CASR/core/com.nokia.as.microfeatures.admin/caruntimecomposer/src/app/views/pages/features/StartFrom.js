import React, { Component } from 'react';
import SelectItem from '@nokia-csf-uxr/csfWidgets/SelectItem';

class StartFrom extends Component {

    componentWillMount() {
        if( this.props.runtimeId === null && this.props.startFromData.length === 0)
            this.props.setRuntime(this.props.startFromData[0].value)
    }

    handleChange = (event) => {
        console.log('handleChange', event)
        const selected = event.value; // 17.1.0
        this.props.setRuntime(selected);
    }

    render() {
        console.log('<StartFrom /> props',this.props)
        return (
            <SelectItem
                id="rencompPulldownNormal"
                selectedItem={this.props.runtimeId}
                data={this.props.startFromData}
                autofocus={false}
                onChange={this.handleChange}
                searchable={true}
            />
        );
    }
}

export default StartFrom;