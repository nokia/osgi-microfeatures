import React, { Component } from 'react';
import * as Labels from '../labels';
import FormLayout from '@nokia-csf-uxr/csfWidgets/FormLayout';
import SelectItemNew from '@nokia-csf-uxr/csfWidgets/SelectItemNew';

import { viewSelectorValues, fluxSelectorValues } from '../actions/actionTypes'
class Configurator extends Component {

    constructor(props) {
        super(props);
        const { updateView , updateFlux } = props;
        this.onViewChange = (event) => { updateView(JSON.parse(event.value)) };
        this.onFluxChange = (event) => { updateFlux(event.value) };
        
    }

//    onViewChange = (event) => this.props.updateView(JSON.parse(event.value));
//    onFluxChange = (event) => this.props.updateFlux(event.value);

    componentWillMount() {
        console.info("<Configurator/> -MOUNTED-")
    }

    componentWillUnmount() {
        this.onViewChange = null;
        this.onFluxChange = null;
        console.info("<Configurator/> UNMOUNTED! ")
    }

    render() {
        console.log("<Configurator props />", this.props);
        const {isStarted2d, config} = this.props;
        const { view, flux } = config;
        const selectedViewItem = JSON.stringify(view);
        return (
            <div className={"configurator"} >
                <FormLayout>
                    <div className="row">
                        <div className="col-sm-6">
                            <SelectItemNew id="viewType"
                                options={[
                                    { label: Labels.LABEL_VIEW_STANDARD, value: JSON.stringify(viewSelectorValues.standard) },
                                    { label: Labels.LABEL_VIEW_HORIZONTAL, value: JSON.stringify(viewSelectorValues.horizontal) },
                                    { label: Labels.LABEL_VIEW_VERTICAL, value: JSON.stringify(viewSelectorValues.vertical) }
                                ]}
                                isDisabled={!isStarted2d}
                                onChange={this.onViewChange}
                                label="View type"
                                name="viewtype"
                                selectedItem={selectedViewItem} />
                        </div>
                        <div className="col-sm-6">
                            <SelectItemNew id="fluxType"
                                options={[
                                    { label: Labels.LABEL_FLUX_REQRES, value: fluxSelectorValues.requestResponse },
                                    { label: Labels.LABEL_FLUX_REQ, value: fluxSelectorValues.request },
                                    { label: Labels.LABEL_FLUX_RES, value: fluxSelectorValues.response },
                                    { label: Labels.LABEL_FLUX_LOST, value: fluxSelectorValues.lost },
                                    { label: Labels.LABEL_FLUX_PERCENTAGE, value: fluxSelectorValues.percentage }
                                ]}
                                isDisabled={!isStarted2d}
                                onChange={this.onFluxChange}
                                label="Flux"
                                name="fluxtype"
                                selectedItem={flux}
                            />
                        </div>
                    </div>

                </FormLayout>
            </div>
        )
    }
}

export default Configurator;
