/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import Button from '@nokia-csf-uxr/csfWidgets/Button';
import TextInput from '@nokia-csf-uxr/csfWidgets/TextInput';
import RcOverlayPanel from '../../commons/RcoverlayPanel'
import { merge } from 'lodash'
import PropTypes from 'prop-types'

class CreateAsmb extends Component {
    constructor(props) {
        super(props);
        console.log("<CreateAsmb /> props", props)
        this.initialState = {
            disablePanelBtn: true,
            // Name properties
            bname: "",
            hitbname: false,
            isValidbname: true,
            // Version properties
            version: "",
            hitversion: false,
            isValidversion: true,
            // Desc properties
            desc: "",
            hitdesc: false,
            isValiddesc: true,
            // Doc properties
            doc: "",
            hitdoc: true,
            isValiddoc: true
        }

        // preset status on preset operation
        this.presetFieldStatus = {
            hitbname: true,
            isValidbname: true,
            hitversion: true,
            isValidversion: true,
            hitdesc: true,
            isValiddesc: true,
            hitdoc: true,
            isValiddoc: true
        }
        this.fieldsDesc = []
        this.fieldsDesc['bname'] = { isMandatory: true, regexp: /^([a-zA-Z0-9]+)(\.[a-zA-Z0-9]+)*$/ }
        this.fieldsDesc['version'] = { isMandatory: true, regexp: /^[1-9]\d*(\.\d+){2}$/ }
        this.fieldsDesc['desc'] = { isMandatory: true, regexp: /^(\w\s?)*\w$/ }
        this.fieldsDesc['doc'] = {
            isMandatory: false,
            regexp: /^(https?|ftp):\/\/([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/
        }
        this.comprefs = []; // Hold references on components

        this.state = merge({}, this.initialState)
    }

    componentWillMount() {
        //        console.log("componentWillMount+++++++++++",this.props.form);
        this.setState(Object.assign({}, this.props.form))
    }

    componentWillReceiveProps(nextProps) {
        console.log("componentWillReceiveProps----------------", nextProps)
        let newState = Object.assign({}, this.initialState, nextProps.form, { disablePanelBtn: (nextProps.form.name === '') })
        // When the state will be preset by pertinent value, we should preset status field too
        if( nextProps.form.name !== '' ) {
            newState = Object.assign(newState,this.presetFieldStatus)
        }
        console.log("newState", newState)
        this.setState(newState)
    }
    /*
        componentWillUpdate(nextProps,nextState) {
            if( nextProps.form ) {
                console.log("componentWillUpdate assembly====================================",nextProps.form)
            }
        }
    */
    componentWillUnmount() {
        this.props.setAsmbPanelStatus(false);
    }

    onPanelAction = (json) => {
        console.log("##### onPanelAction ######", json)
        const isClosed = (json.state === "closed")
        this.props.setAsmbPanelStatus(!isClosed)
    }
    /*
        onPanelOpenClose = () => {
            console.log("------onPanelOpenClose------")
        }
    */
    createAssembly = (e) => {
        console.log('createAssembly');
        const payload = {
            name: this.state.bname,
            version: this.state.version,
            desc: this.state.desc,
            doc: this.state.doc,
            features: this.props.assemblyFeatures
        };
        this.setState({ disablePanelBtn: true })
        this.comprefs['overlay'].closePanel();
        this.props.createAssembly(payload);
    }

    onsubmitform = () => {
        console.log("form submitted!")
    }
    onsubmit = (e) => {
        console.log("onsubmit!", e)
    }

    isValidFields = (field, isValid) => {
        if (isValid === false)
            return false;
        // Check the validity of all others fields
        const fieldnames = ['bname', 'version', 'desc', 'doc'].filter(word => word !== field);
        let property;
        for (let i = 0; i < fieldnames.length; i++) {
            property = 'isValid' + fieldnames[i];
            if (this.state[property] !== true || this.state['hit' + fieldnames[i]] !== true)
                return false;
        }
        return true;
    }

    isValidField = (name, value) => {
        if (this.fieldsDesc[name].isMandatory === false && value.length === 0)
            return true;
        return this.fieldsDesc[name].regexp.test(value);
    }

    handleOnChange = (e) => {
//        console.log('handleOnChange',e)
        const name = e.nativeEvent.target.name;
        const value = e.value;
        let o = {}
        o[name] = value;
        const isValidField = this.isValidField(name, value);
        o['isValid' + name] = isValidField;
        o['hit' + name] = true;
        o['disablePanelBtn'] = !this.isValidFields(name, isValidField);
        console.log("onchange!", e, o)
        this.setState(o);
    }

    renderContent = () => {
        return (
            <div>
                <TextInput id={'crasmbbsn'}
                    name={'bname'}
                    placeholder={'Bundle name'}
                    text={this.state.bname}
                    errorMsg={'Input format: com.domain.package'}
                    spellcheck={false}
                    focus
                    label={'Bundle Symbolic Name'}
                    required={this.fieldsDesc['bname'].isMandatory}
                    error={!this.state.isValidbname}
                    onChange={(e) => this.handleOnChange(e)}
                />
                <TextInput id={'crasmbvn'}
                    name={'version'}
                    placeholder={'1.0.0'}
                    text={this.state.version}
                    validationPattern={this.fieldsDesc['version'].validationPattern}
                    errorMsg={'Input format: major.minor.micro'}
                    spellcheck={false}
                    label={'Version'}
                    required={this.fieldsDesc['version'].isMandatory}
                    error={!this.state.isValidversion }
                    onChange={(e) => this.handleOnChange(e)}
                />
                <TextInput id={'crasmbshortn'}
                    name={'desc'}
                    placeholder={'Short name'}
                    text={this.state.desc}
                    errorMsg={'Input format: any word'}
                    spellcheck={false}
                    label={'Short name'}
                    required={this.fieldsDesc['desc'].isMandatory}
                    error={!this.state.isValiddesc }
                    onChange={(e) => this.handleOnChange(e)}
                />
                <TextInput id={'crasmbdoc'}
                    name={'doc'}
                    placeholder={'URL http(s):// '}
                    text={this.state.doc}
                    errorMsg={'None or consistent URL to point on documentation.'}
                    spellcheck={false}
                    label={'Doc URL'}
                    required={this.fieldsDesc['doc'].isMandatory}
                    error={!this.state.isValiddoc }
                    onChange={(e) => this.handleOnChange(e)}
                />
            </div>
        )
    }

    /**
     * This function is not a callback function.
     * It will be executed by the RcOverlayPanel ( child of our component)in its own context,
     * and allows to overrides the original implementation of its toggle button
     */
    getToogleButton = (context) => (
        <Button
            ref={
                (toggleButton) => {
                    context.toggleButton = toggleButton
                }
            }
            text="CREATE ASSEMBLY"
            isCallToAction
            disabled={context.props.disabled}

            id={context.props.id + "-openPanel"}
            onClick={context.onOpen}
//            data-open-panel={context.state.buttonState === 'opened-panel'}
            iconColor={context.state.buttonState === 'opened-panel' ? 'rgba(18,65,145,1)' : 'rgba(0,0,1,.54)'}
        />
    )

    render() {
        console.log("<CreateAsmb /> render() props", this.props)
        return (
            <RcOverlayPanel ref={(c) => this.comprefs['overlay'] = c}
                id={'overlay-createasmb'}
                title={'Create Assembly'}
                disabled={this.props.disabled}
                content={this.renderContent()}
                buttonType={'action'}
                buttonText={'CREATE'}
                getToogleButton={this.getToogleButton}
                disablePanelBtn={this.state.disablePanelBtn}
                onClick={(e) => this.createAssembly(e)}
                onPanelAction={(o) => this.onPanelAction(o)}
                wrapperOffset={83}
                panelOffset={-84}
            />
        )
    }
}

CreateAsmb.defaultProps = {
    assemblyFeatures: []
}

CreateAsmb.propTypes = {
    form: PropTypes.shape({
        name: PropTypes.string.isRequired,
        version: PropTypes.string.isRequired,
        desc: PropTypes.string.isRequired,
        doc: PropTypes.string.isRequired
    }).isRequired,
    assemblyFeatures: PropTypes.array.isRequired,
    setAsmbPanelStatus: PropTypes.func.isRequired,
    createAssembly: PropTypes.func.isRequired,
    disabled: PropTypes.bool.isRequired
}

export default CreateAsmb

