import React from 'react';
import PropTypes from 'prop-types';

import { MENU_TYPES } from '../../../state/ducks/settings/constants';
import SelectItem from '@nokia-csf-uxr/csfWidgets/SelectItem';


const MenuSettings = (props) => {

  const typesOptions = MENU_TYPES;

  const handleChangeMenuType = (event) => {
    props.onTypeSelect(event.value); // 17.1.0
  }

  return (
      <div className="col-xs-5">
        <h1>Change menu options</h1>
        <SelectItem
          data={typesOptions}
          label="Open modes"
          name="type-select"
          onChange={handleChangeMenuType}
//          placeholder="Custom"
          selectedItem={props.selectedType}
        />
      </div>
  );
};

MenuSettings.propTypes = {
  onTypeSelect: PropTypes.func.isRequired,
  selectedType: PropTypes.string.isRequired
};

export default MenuSettings;
