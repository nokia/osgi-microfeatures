/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import SelectItemNew from "@nokia-csf-uxr/csfWidgets/SelectItemNew/SelectItemNew";
/**
 * This class is a WORK ARROUND for rendering issue.
 * From the UX Component release npm 17.40.2 the select item once is mounted does not show the
 * options when the user click on, or when the user want to enter a new option, then the 'add myoption'
 * is not show.
 * This wrong behavior has been found when the user want to set a runtime for a new Route
 * 
 * 
 */
class RcSelectItemNew extends Component {

componentDidMount() {
    this.forceUpdate();
}


render() {
    console.log("<RcSelectItemNew /> props",this.props);
    return(
        <SelectItemNew {...this.props}  />
    )
}



}
export default RcSelectItemNew