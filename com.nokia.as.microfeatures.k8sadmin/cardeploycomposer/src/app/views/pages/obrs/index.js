/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { PureComponent } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { entitiesSelectors } from '../../../state/ducks/entities'
import { bundlesOperations, bundlesSelectors } from '../../../state/ducks/bundles'

import ToolBar from '../../commons/ToolBar';
import ObrTabContent from './ObrTabContent'
import BundlePanel from './BundlePanel'


class Obrs extends PureComponent {
    constructor(props) {
        super(props)
        this.toolbarh = 69;
        this.tabHeaderHeight = 48;
        this.indexTab = 0;
    }

    getTabContentHeight = () => this.props.height - (this.toolbarh + this.tabHeaderHeight);

    changeTab = (event) => {
        console.log("changeTab", event);
        this.indexTab = event.value;
    }

    componentDidMount() {
        console.log("<Obrs/> componentDidMount");
    }

    componentWillUnmount() {
        console.log("<Obrs/> componentWillUnmount");
        this.props.closeBundlePanel();
    }

    render() {
        console.log("<Obrs /> props ", this.props)
        return (
            <div style={{ height: this.props.height, position: 'relative' }} >
                <ToolBar pageTitle={'Browse Obrs'} />
                {this.props.isStartedOp === true && <div className="overlayBundle"></div>}
                {this.props.isOpenBundlePanel &&
                    <div style={{ verticalAlign: 'top' }}>
                        <BundlePanel
                            isResolveOperation={this.props.isResolveOperation}
                            bundle={this.props.operationBundle}
                            resources={this.props.opResources}
                            onCancel={this.props.closeBundlePanel}
                        />
                    </div>
                }
                {this.props.isOpenBundlePanel === false &&
                    <ObrTabContent url={this.props.currentObr}
                                height={this.getTabContentHeight()}
                                width={this.props.width}
                                local={false}
                                bundles={this.props.currentBundles}
                                onResolve={this.props.resolveBundle}
                                onFindDeps={this.props.findDepsBundle}
                            />
                }
            </div>
        );
    }
}

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function (state) {
    return {
        currentObr: entitiesSelectors.getSelectedObr(state),
        currentBundles: bundlesSelectors.getAllCurrentBundles(state),
        operationBundle: bundlesSelectors.getBundleForOp(state),
        isOpenBundlePanel: bundlesSelectors.isOpenPanel(state),
        isStartedOp: bundlesSelectors.isBundleOpStarted(state),
        isResolveOperation: bundlesSelectors.isResolveOperation(state),
        opResources: bundlesSelectors.getOpRessources(state)
    }
}

const mapDispatchToProps = function (dispatch) {
    return bindActionCreators({
        ...bundlesOperations
    }, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Obrs);