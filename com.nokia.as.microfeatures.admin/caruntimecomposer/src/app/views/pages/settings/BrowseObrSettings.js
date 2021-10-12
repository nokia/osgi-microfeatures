/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React from 'react';
import PropTypes from 'prop-types';

import CheckBox from '@nokia-csf-uxr/csfWidgets/CheckBox';


const BrowseObrSettings = (props) => {

  const handleChangeELA = (event) => {
    props.onChangeEnableLocalActions(event.value); // 17.1.0
  }

  return (
      <div className="col-xs-5">
        <h1>Change Browse Obrs options</h1>
        <h3>Local obr</h3>
        <CheckBox
          value={props.displayLocalActions}
          label="Allows to resolve bundles and find their dependencies."
          onChange={handleChangeELA}
        />
      </div>
  );
};

BrowseObrSettings.propTypes = {
  onChangeEnableLocalActions: PropTypes.func.isRequired,
  displayLocalActions: PropTypes.bool.isRequired
};

export default BrowseObrSettings;
