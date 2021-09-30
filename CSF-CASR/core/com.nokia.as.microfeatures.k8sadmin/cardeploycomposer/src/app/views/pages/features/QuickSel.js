import React, { Component } from 'react';
import SelectItemNew from '@nokia-csf-uxr/csfWidgets/SelectItemNew';
import PropTypes from 'prop-types'

class QuickSel extends Component {

    componentWillMount() {
        if( this.props.quickSel === null && this.props.quickSelOptions.length > 0)
            this.props.setQuickSelOption(this.props.quickSelOptions[0].value)
    }

    handleChange = (event) => {
        console.log('handleChange', event)
        const selected = event.value; // 17.1.0
        this.props.setQuickSelOption(selected);
    }

    render() {
        console.log('<QuickSel /> props',this.props)
        return (
            <SelectItemNew
                id="featuresQuickPulldown"
                selectedItem={this.props.quickSel}
                options={this.props.quickSelOptions}
                autofocus={false}
                onChange={this.handleChange}
                searchable={true}
            />
        );
    }
}

QuickSel.propTypes = {
    quickSel : PropTypes.string.isRequired,
    setQuickSelOption : PropTypes.func.isRequired,
    quickSelOptions: PropTypes.arrayOf(PropTypes.shape({
        label: PropTypes.string.isRequired,
        value: PropTypes.string.isRequired,
        id: PropTypes.string.isRequired
    })).isRequired
}




export default QuickSel;