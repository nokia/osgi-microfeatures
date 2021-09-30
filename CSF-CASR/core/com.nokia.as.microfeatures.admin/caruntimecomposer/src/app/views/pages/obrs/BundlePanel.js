import React, { PureComponent } from 'react';
import RcOverlayPanel from '../../commons/RcoverlayPanel'
import BundleTree from './BundleTree'
import $ from "jquery"

export default class BundlePanel extends PureComponent {

    componentDidMount() {
        const toggleButton = $('#overlay-bundleoperation-openPanel-button')
        console.log('toggleButton', toggleButton)
        toggleButton.click()
    }

    componentWillUnmount() {
        console.log("componentWillUnmount")
        // Close the panel
//        this.props.onCancel();
    }

    renderContent = () => {
        //        const bundles = (this.props.isResolveOperation)?this.props.resolveds:this.props.dependents
        return (
            <BundleTree
                bundle={this.props.bundle}
                list={this.props.resources}
                isResolved={this.props.isResolveOperation} />
        )
    }

    handlePanelAction = (json) => {
        console.log("##### onPanelAction ######", json)
        const isClosed = (json.state === "closed")
        if (isClosed)
            this.props.onCancel();
    }

    renderTitle = () => (this.props.isResolveOperation === true) ? 'Resolveds' : 'Dependents'


    render() {
        console.log('<BundlePanel /> props', this.props)

        return (
            <RcOverlayPanel
                id={'overlay-bundleoperation'}
                title={this.renderTitle()}
                content={this.renderContent()}
                onPanelAction={(o) => this.handlePanelAction(o)}
                hideFooter={true}
            />
        )
    }
}