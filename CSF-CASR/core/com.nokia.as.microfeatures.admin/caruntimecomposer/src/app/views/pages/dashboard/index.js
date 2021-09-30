import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { entitiesSelectors, entitiesOperations } from '../../../state/ducks/entities'
import CardCollection from '@nokia-csf-uxr/csfWidgets/Card/CardCollection';
//import SelectItem from '@nokia-csf-uxr/csfWidgets/SelectItem';
import {default as SelectItem} from '@nokia-csf-uxr/csfWidgets/SelectItemNew';
import Card from '@nokia-csf-uxr/csfWidgets/Card';
import Label from '@nokia-csf-uxr/csfWidgets/Label';
import PieChart from '@nokia-csf-uxr/csfWidgets/PieChart/PieChart';
import Image from '@nokia-csf-uxr/csfWidgets/Image';

import ToolBar from '../../commons/ToolBar';
import $ from "jquery"

const OBR_REGEXP = /.*\/((\w+\.)*(\w)+-.*)\.xml$/
class DashBoard extends Component {
    constructor(props) {
        super(props)
        this.state = {
            selectorItems: this.getSelectorItems(),
        }
    }
/*

    componentDidMount = () => {
//        this.resizeCards()
    }

    componentDidUpdate = () => {
//        this.resizeCards()
    }

    resizeCards = () => {
        // CSS tricks to fill all reserved height by CardCollection layout configuration       
        $('#cardObr').parent().css('height', '100%')
        $('#cardStatus').parent().css('height', '100%')
        $('#cardContact').parent().css('height', '100%')
        $('#cardFeaturesUsed').parent().css('height', '100%')
    }
*/

    getSelectorItems = () => {
        const items = []
        const urls = this.props.remoteObrs;
        for (let i = 0; i < urls.length; i++) {
            let item = { label: this.getUrlLabel(urls[i]), value: urls[i] }
            items.push(item)
        }
        return items
    }

    getUrlLabel = (url) => {
        // Check if remote obr correspond to the .m2 obr ( case of -Dm2 option when the jar is launched )
        if( url.startsWith("file:")) {
            return url.substring(5);
        }
        // Reset global regexp index to parse properly
        OBR_REGEXP.lastIndex = 0
        const match = OBR_REGEXP.exec(url)
        if (!match || match.length < 3) {
            console.warn("getUrlLabel no matching", url, match)
            return undefined;
        }
        return match[1]
    }

    onSelectObr = (event) => {
        const newUrl = event.value; // 17.1.0
        console.log("newUrl", newUrl)
        this.props.selectObr(newUrl);
    }

    renderObrSelector = () => {

        return (
            <SelectItem id={"cardObrSelectNew"}
            hasOutline={false}
            placeholder={'Select a remote OBR'}
            options={this.state.selectorItems}
            width={250}
            selectedItem={this.props.selectedObr}
            onChange={this.onSelectObr}

        />
        )
    }

    render() {
        console.log("<DashBoard /> props ", this.props)
        const featureText = (this.props.nbFeatures > 1) ? 'Features' : 'Feature'
        const assemblyText = (this.props.nbAssemblies > 1) ? 'Assemblies' : 'Assembly'
        const snapshotText = (this.props.nbSnapshots > 1) ? 'Snapshots' : 'Snapshot'
        const useds = this.props.nbUsedFeatures - this.props.nbNotFoundFeatures
        const unusedFeatures = this.props.nbFeatures - useds;
        const usedFeaturesData = [
            { name: 'Unused', value: unusedFeatures, fill: '#DFF0FD' },
            { name: 'Used', value: useds, fill: '#0F3576' },
            { name: 'Not found', value: this.props.nbNotFoundFeatures, fill: '#FF0000' }
        ];

        return (
            <div>
                <ToolBar pageTitle={'Dashboard'} />
                <div style={{overflow: 'hidden'}}> 
                    <CardCollection
                        dynamicWidth={true}
                        isDraggable={false}
                        margin={[16, 12]}
                        padding={[12, 12]}
                        layout={[
                            { "x": 0, "y": 0, "w": 1, "h": 1, "i": "0" },
                            { "x": 0, "y": 1, "w": 1, "h": 2, "i": "1" },
                            { "x": 1, "y": 0, "w": 1, "h": 3, "i": "2" },
                            { "x": 2, "y": 1, "w": 1, "h": 2, "i": "3" }

                        ]}
                        cols={2}
                    >
                        <Card id={"cardObr"} className="card" autoResize={true} css={{ height: '100%', width: 'auto' }} >
                            <Label id={"cardObrLabel"} text={"Current OBR"} />
                            {this.renderObrSelector()}
                        </Card>

                        <Card id={"cardStatus"} className="card" autoResize={true} css={{ height: '100%', width: 'auto' }} >
                            <Label id={"cardStatusLabel"} text={"Current status"} />
                            <table>
                                <tbody>
                                    <tr>
                                        <td>
                                            <div style={{ textAlign: 'center' }}>
                                                <Image internalSrc={'ic_matrix.svg'} alt={"features"} width={'84px'} />
                                                <p><span className={'dashboard-nb-entities'} >{this.props.nbFeatures}</span></p>
                                                <p><span className={'dashboard-entity-name'} >{featureText}</span></p>
                                            </div>
                                        </td>
                                        <td>
                                            <div style={{ textAlign: 'center' }}>
                                                <Image internalSrc={'ic_subnets.svg'} alt={"assemblies"} width={'84px'} />
                                                <p><span className={'dashboard-nb-entities'} >{this.props.nbAssemblies}</span></p>
                                                <p><span className={'dashboard-entity-name'} >{assemblyText}</span></p>
                                            </div>
                                        </td>
                                        <td>
                                            <div style={{ textAlign: 'center' }}>
                                                <Image internalSrc={'ic_snapshot.svg'} alt={"snapshots"} width={'84px'} />
                                                <p><span className={'dashboard-nb-entities'} >{this.props.nbSnapshots}</span></p>
                                                <p><span className={'dashboard-entity-name'} >{snapshotText}</span></p>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </Card>
                        <Card id={"cardContact"} className="card" autoResize={true} css={{ height: '100%', width: 'auto' }} >
                            <Label id={"cardContactLabel"} text={'Features referenced by assemblies'} />
                            <PieChart 
                                id={'featuresUsedGraph'}
                                title={'Used/Unused'}
                                width={300}
                                height={300}
                                data={usedFeaturesData} 
                                nameKey="name"
                                valueLabel={'Nb'}
                                dataKey="value"
                                labelKey="name"
                                animate={true}
                                outerRadius={80}
                                activeIndex={1}
                            />
                        </Card>
                        <Card id={"cardFeaturesUsed"} className="card" autoResize={true} css={{ height: '100%', width: 'auto' }} >
                            <Label id={"cardFeaturesUsedLabel"} text={"Contact us"} />
                            <ul style={{ listStyleType: 'none' }}>
                                <li style={{ paddingBottom: '20px' }}>
                                    <p><span className={'dashboard-contact'} >{'Support :'}</span></p>
                                    <p><a className={'dashboard-contact'} href={'https://greenhopper.app.alcatel-lucent.com/browse/CSFS'} target={'_blank'} >{'CSFS Support Request'}</a></p>
                                </li>
                                <li>
                                    <p><span className={'dashboard-contact'} >{'Confluence :'}</span></p>
                                    <p><a className={'dashboard-contact'} href={'https://confluence.app.alcatel-lucent.com/display/plateng/CASR+-+MicroFeatures'} target={'_blank'} >{'CASR - MicroFeatures'}</a></p>
                                </li>
                            </ul>
                        </Card>
                    </CardCollection>
                </div>
            </div>
        )
    }
}



//
// CONNECTED TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function (state) {
    return {
        selectedObr: entitiesSelectors.getSelectedObr(state),
        remoteObrs: entitiesSelectors.getRemoteObrs(state),
        nbFeatures: Object.keys(entitiesSelectors.getFeatures(state)).length,
        nbAssemblies: Object.keys(entitiesSelectors.getAssemblies(state)).length,
        nbSnapshots: Object.keys(entitiesSelectors.getSnapshots(state)).length,
        nbUsedFeatures: entitiesSelectors.getUsedFeaturesIds(state).length,
        nbNotFoundFeatures: entitiesSelectors.getFeaturesNotFound(state)
    }
}

const mapDispatchToProps = function (dispatch) {
    return bindActionCreators({
        selectObr: entitiesOperations.selectObr
    }, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(DashBoard);