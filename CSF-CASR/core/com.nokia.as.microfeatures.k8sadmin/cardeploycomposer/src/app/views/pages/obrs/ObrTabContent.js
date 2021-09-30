import React, { Component } from 'react';
import PropTypes from 'prop-types'
import FormLayout from '@nokia-csf-uxr/csfWidgets/FormLayout';
import Label from '@nokia-csf-uxr/csfWidgets/Label';
import BundlesList from './BundlesList'

export default class ObrTabContent extends Component {
    static propTypes = {
        local : PropTypes.bool.isRequired,
        height: PropTypes.number.isRequired,
        url: PropTypes.string.isRequired,
        //        children: PropTypes.node.isRequired
    }

    getListHeight = () => this.props.height - (65 + 20);

    componentDidMount() {
        console.log("<ObrTabContent/> componentDidMount");
    }

    componentWillUnmount() {
        console.log("<ObrTabContent/> componentWillUnmount");
    }

    render() {
        console.log("<ObrTabContent /> props ", this.props)
        const bundleListId = ( this.props.local === true)?'blc':'blr';
        return (
            <div style={{ height: this.props.height, position: 'relative' }} >
                <FormLayout>
                    <Label id={'obr-url-text'} text={"URL:"} />
                    <Label text={this.props.url} />
                </FormLayout>
                <div className={"obrs-bundles-rows"} style={{ height: this.getListHeight() }} >
                    <BundlesList
                        key={bundleListId}
                        local={this.props.local}
                        bundles={this.props.bundles}
                        onResolve={this.props.onResolve}
                        onFindDeps={this.props.onFindDeps}
                        width={this.props.width}
                    />
                </div>
            </div>
        )
    }
}