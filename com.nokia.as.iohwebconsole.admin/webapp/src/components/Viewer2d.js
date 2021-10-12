/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
//import * as Labels from '../labels';
//import CardCollection from '@nokia-csf-uxr/csfWidgets/Card/CardCollection';
import CardCollection from './CardCollection';
import Card from '@nokia-csf-uxr/csfWidgets/Card';
import * as Labels from '../labels';
import admin from '../managers/main'

import Configurator from './Configurator';

class Viewer2d extends Component {

    constructor(props) {
        super(props);
        this._2dmanager = admin.get2DManager();
        this.state = { height: 800, width: 800, rowHeight: 125 };
    }

    componentDidMount() {
        console.info("<Viewer2d /> -MOUNTED-")
        window.addEventListener('resize', this.updateCardCollectionSizing);
        this.updateCardCollectionSizing();
        this._2dmanager.startDisplay(this.graph2d,this.info2d);
    }

    componentDidUpdate(prevProps) {
        // when config has been change, triggs the 2d manager to change display
        if( this.props.isStarted2d === true) {
            if ( JSON.stringify(this.props.config) !== JSON.stringify(prevProps.config) ) {
                console.log("componentDidUpdate new config",this.props.config);
                this._2dmanager.changeConfig(this.props.config);
            }
        }
    }


    componentWillUnmount() {
        window.removeEventListener('resize', this.updateCardCollectionSizing);

        this._2dmanager = null;
        this.viewer2d = null;
        this.graph2d = null;
        this.info2d = null;
        console.info("<Viewer2d /> UNMOUNTED!")
    }

    updateCardCollectionSizing = () => {
        const { width, height } = this.viewer2d.getBoundingClientRect();
        console.log("viewer2d height", height);
        const configuratorHeight = 64;
        const remainingHeight = height - (configuratorHeight);
        let rowHeight = Math.floor((remainingHeight - (14 * 3)) / 3);
        rowHeight = Math.max(48, rowHeight);

        const ccsizing = { height: remainingHeight, width: width, rowHeight: rowHeight }
        this.setState(ccsizing);

    }


    render() {
        console.log("<Viewer2d props />", this.props, this.state)
        const { height, width, rowHeight } = this.state;

        return (
            <div className="viewer2d" ref={(viewer2d) => { this.viewer2d = viewer2d; }} >
                <Configurator {...this.props} />
                <CardCollection
                    id="cardCollectionViewer"
                    className="cardCollection2d"
                    height={height}
                    width={width}
                    cols={5}
                    rowHeight={rowHeight}
                    margin={[16, 12]}
                    padding={[12, 12]}
                    layout={[
                        { "x": 0, "y": 0, "w": 4, "h": 3, "i": "0" },
                        { "x": 4, "y": 0, "w": 1, "h": 2, "i": "1" },
                        { "x": 4, "y": 2, "w": 1, "h": 1, "i": "2" }
                    ]}
                >
                    <Card id={"cardGraph2d"} key={"cardGraph2d"} className="card" autoResize css={{ height: '100%', width: '100%' }} >
                        <div id={'right2dContainer'} ref={(graph2d) => { this.graph2d = graph2d; }}></div>
                    </Card>
                    <Card id={"cardInfo2d"} key={"cardInfo2d"} className="card" autoResize css={{ height: '100%', width: '100%' }} >
                        <div id={"configurator2dinfo"} ref={(info2d) => { this.info2d = info2d; }}></div>
                    </Card>
                    <Card id={"cardLegend2d"} key={"cardLegend2d"}
                        className="card"
                        autoResize
                        css={{ height: '100%', width: '100%' }}
                    >
                        <h2><div>Nodes:</div></h2>
                        <div>
                            <img src={process.env.PUBLIC_URL + '/images/ic_happy_face.svg'} alt={"client"} style={{ width: '40px' }} />
                            <span style={{ paddingLeft: '35px', verticalAlign: 'super' }} >Client</span>
                        </div>
                        <div>
                            <img src={process.env.PUBLIC_URL + '/images/ioh.svg'} alt={"ioh"} style={{ width: '40px' }} />
                            <span style={{ paddingLeft: '35px', verticalAlign: 'super' }} >IOH</span>
                        </div>
                        <div style={{ marginTop: '5px' }}>
                            <span style={{ borderRadius: '4px', padding: '2px', background: '#d2e5ff', border: '1px solid #428bed' }} >name</span>
                            <span style={{ paddingLeft: '28px' }} >Agent</span>
                        </div>
                        <h2><div>Colors:</div></h2>
                        <div>{ Labels.LABEL_CARDLEGENDCOLOR }</div>
                    </Card>
                </CardCollection>
            </div>
        )
    }

}

export default Viewer2d;
