/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React from 'react';
import PropTypes from 'prop-types';

import { BRAND_LOGOS, EXTENDED_COLORS, ICON_AND_TEXT_STYLES, THEMES } from '../../../state/ducks/settings/constants';
import SelectItem from '@nokia-csf-uxr/csfWidgets/SelectItem';


const BrandingSettings = (props) => {
  const themeOptions = Object.values(THEMES).map(theme => ({
    label: theme.name,
    value: theme.name.toUpperCase()
  }));

  const primaryColorOptions = EXTENDED_COLORS;

  const logoOptions = Object.values(BRAND_LOGOS);

  const styleOverrideOptions = Object.values(ICON_AND_TEXT_STYLES).map(style => ({
    label: style,
    value: style
  }));

  const handleChangeTheme = (event) => {
    props.onThemeSelect(event.value); // 17.1.0
  }

  const handleChangeColor = (event) => {
    props.onPrimaryColorSelect(event.value); // 17.1.0
  }

  const handleChangeLogo = (event) => {
    props.onLogoSelect(event.value); // 17.1.0
  }

  const handleChangeStyle = (event) => {
    props.onTextAndIconStyleSelect(event.value); // 17.1.0
  }

  console.log("<BrandingSettings /> props",props,themeOptions)

  return (
      <div className="col-xs-5">
        <h1>Change theme options</h1>
        <SelectItem
          data={themeOptions}
          label="Theme"
          name="theme-select"
          onChange={handleChangeTheme}
          placeholder="Custom"
          selectedItem={props.selectedTheme}
        />
        <SelectItem
          data={primaryColorOptions}
          label="Primary color"
          name="primary-color-select"
          onChange={handleChangeColor}
          selectedItem={props.selectedPrimaryColor}
        />
        <SelectItem
          data={logoOptions}
          label="Logo"
          name="logo-select"
          onChange={handleChangeLogo}
          selectedItem={props.selectedLogo}
        />
        <SelectItem
          data={styleOverrideOptions}
          label="Text and icon style"
          name="text-and-icon-select"
          onChange={handleChangeStyle}
          selectedItem={props.selectedTextAndIconStyle}
        />
      </div>
  );
};

BrandingSettings.propTypes = {
  onLogoSelect: PropTypes.func.isRequired,
  onPrimaryColorSelect: PropTypes.func.isRequired,
  onTextAndIconStyleSelect: PropTypes.func.isRequired,
  onThemeSelect: PropTypes.func.isRequired,
  selectedLogo: PropTypes.string.isRequired,
  selectedPrimaryColor: PropTypes.string.isRequired,
  selectedTextAndIconStyle: PropTypes.string.isRequired,
  selectedTheme: PropTypes.string.isRequired
};

export default BrandingSettings;
