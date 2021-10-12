/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';

import LightTree from '../../commons/LightTree'
import { isEqual } from 'lodash'
import $ from "jquery"

export default class BundleTree extends Component {

    constructor(props) {
        super(props)
        this.state = { data: [] }
        this.bsn = this.props.bundle.bsn
        this.bsnv = this.props.bundle.version
    }

    componentWillMount() {
        this.buildTreeData(this.props.list)
    }

    componentWillReceiveProps(nextProps) {
        if (!isEqual(nextProps.list, this.props.list))
            this.buildTreeData(nextProps.list)
    }

    buildTreeData = (resources) => {
        console.log('>>>>>>>>>>>>>>>>>>>>>> buildTreeData', resources)
        const self = this
        let data = []
        $.each(resources,
            function (index, resource) {
                if (!resource)
                    return true;
                let name = resource.bsn;
                if (!name)
                    name = "--";
                let version = resource.version;
                if (!version)
                    version = "none";

                if (name === self.bsn && version === self.bsnv) {
                    return true;
                }

                let rr = new ResolvedResource(name, version);
                let reqs = resource.reqs;
                let requirements = []

                if (reqs && reqs.length) {
                    for (let i = 0; i < reqs.length; i++) {
                        let req = reqs[i];
                        let requirement = new ResolvedRequirement(req.ns);
                        requirements.push(requirement);
                        requirements = requirements.sort(
                            function (c1, c2) {
                                let res = c1.label.localeCompare(c2.label);
                                return res;
                            }
                        );

                        let dirs = req.directives;
                        let directives = []
                        if (dirs) {
                            for (let key in dirs) {
                                directives.push({ label: key + '=' + dirs[key] });
                            }
                            requirement.addDirectives(directives)
                        }
                        let attrs = req.attributes;
                        let attributes = []
                        if (attrs) {
                            for (let key in attrs) {
                                attributes.push({ label: key + '=' + attrs[key] });
                            }
                            requirement.addAttributes(attributes)
                        }

                    }
                    rr.addRequirements(requirements)
                }

                let capabilities = []
                let caps = resource.caps;
                let stop = true
                if (caps && caps.length) {
                    //                    stop = false
                    for (let i = 0; i < caps.length; i++) {
                        let cap = caps[i];
                        let capability = new ResolvedCapability(cap.ns);
                        capabilities.push(capability);
                        capabilities = capabilities.sort(
                            function (c1, c2) {
                                let res = c1.label.localeCompare(c2.label);
                                return res;
                            }
                        );

                        let dirs = cap.directives;
                        let directives = []
                        if (dirs) {
                            for (let key in dirs) {
                                directives.push({ label: key + '=' + dirs[key] });
                            }
                            capability.addDirectives(directives)
                        }

                        let attrs = cap.attributes;
                        let attributes = [];
                        if (attrs) {
                            for (let key in attrs) {
                                attributes.push({ label: key + '=' + attrs[key] });
                            }
                            capability.addAttributes(attributes)
                        }
                    }
                    rr.addCapabilities(capabilities)
                }

                data.push(rr);
                return stop
            }
        );
        data = data.sort(
            function (d1, d2) {
                let res = d1.label.localeCompare(d2.label);
                return res;
            }
        );

        let newState = { data: data };
        if( data.length === 0 ) {
            const message = (this.props.isResolved)?'Nothing to list.':'No dependency.'
            newState['message'] = message;
        }
        this.setState(newState);
    }

    render() {
        console.log('<BundleTree /> props , this.state.data', this.props, this.state)
        const headerText = 'Bundle: ' + this.props.bundle.fid
        return (
                <LightTree id={'bundleTree'} title={headerText} data={this.state.data} message={this.state.message} />
        )
    }
}

// Class tools
const defaultNameSpaceDisplay = 'no namespace';
function ResolvedResource(name, version) {
    this.isContainer = true;
    this.label = name + ' ' + version;
    this.toolTipText = name + '@' + version;
    this.collapsed = true;
}
ResolvedResource.prototype.addRequirements = function (requirements) {
    let requirementsFolder = {
        isContainer : true,
        label: 'requirements',
        toolTipText: 'requirements',
        collapsed: false,
        children: requirements
    }
    if (!this.children)
        this.children = []
    this.children.push(requirementsFolder)
}

ResolvedResource.prototype.addCapabilities = function (capabilities) {
    let capabilitiesFolder = {
        isContainer : true,
        label: 'capabilities',
        toolTipText: 'capabilities',
        collapsed: false,
        children: capabilities
    }
    if (!this.children)
        this.children = []
    this.children.push(capabilitiesFolder)
}


function ResolvedRequirement(namespace) {
    this.isContainer = true;
    if (namespace === '') {
        namespace = defaultNameSpaceDisplay;
    }
    this.label = namespace;
    this.toolTipText = namespace;
    this.collapsed = true;
}
ResolvedRequirement.prototype.addDirectives = function (directives) {
    let directivesFolder = {
        isContainer : true,
        label: 'directives',
        toolTipText: 'directives',
        collapsed: false,
        children: directives
    }
    if (!this.children)
        this.children = []
    this.children.push(directivesFolder)
}

ResolvedRequirement.prototype.addAttributes = function (attributes) {
    let attributesFolder = {
        isContainer : true,
        label: 'attributes',
        toolTipText: 'attributes',
        collapsed: false,
        children: attributes
    }
    if (!this.children)
        this.children = []
    this.children.push(attributesFolder)
}



function ResolvedCapability(namespace) {
    this.isContainer = true;
    if (namespace === '') {
        namespace = defaultNameSpaceDisplay;
    }
    this.label = namespace;
    this.toolTipText = namespace;
    this.collapsed = true;
}
ResolvedCapability.prototype.addDirectives = function (directives) {
    let directivesFolder = {
        isContainer : true,
        label: 'directives',
        toolTipText: 'directives',
        collapsed: false,
        children: directives
    }
    if (!this.children)
        this.children = []
    this.children.push(directivesFolder)
}

ResolvedCapability.prototype.addAttributes = function (attributes) {
    let attributesFolder = {
        isContainer : true,
        label: 'attributes',
        toolTipText: 'attributes',
        collapsed: false,
        children: attributes
    }
    if (!this.children)
        this.children = []
    this.children.push(attributesFolder)
}
