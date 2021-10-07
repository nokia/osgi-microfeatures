import Nokia_logo_blue from '@nokia-csf-uxr/csfWidgets/images/Nokia_logo_blue.svg'
import Nokia_logo_white from '@nokia-csf-uxr/csfWidgets/images/Nokia_logo_white.svg'
//
// BRANDING
//
const PRIMARY_COLORS = {
    NOKIA: {
        label: 'Nokia blue',
        value: '#124191'
    }
};

const BRAND_LOGOS = {
    NOKIA_BLUE: {
        label: 'Nokia blue logo',
        value: Nokia_logo_blue
    },
    NOKIA_WHITE: {
        label: 'Nokia white logo',
        value: Nokia_logo_white
    }
};

const ICON_AND_TEXT_STYLES = {
    AUTO: 'auto',
    BLACK: 'black',
    WHITE: 'white'
};

const THEMES = {
    NOKIA: {
        logo: BRAND_LOGOS.NOKIA_WHITE.value,
        name: 'NOKIA',
        color: PRIMARY_COLORS.NOKIA.value,
        style: ICON_AND_TEXT_STYLES.AUTO
    }
};

const EXTENDED_COLORS = [
    ...Object.values(PRIMARY_COLORS),
    {
        label: 'Black',
        value: '#000000'
    },
    {
        label: 'Orange',
        value: '#FF9933'
    },
    {
        label: 'Light blue',
        value: '#99CCFF'
    }
];


//
// MENU
//
const MENU_TYPES = [
    {
        label: 'Persistent',
        value: 'persistent'
    },
    {
        label: 'Temporary',
        value: 'temporary'
    },
    {
        label: 'Compact',
        value: 'compact'
    }
];

const InitialMenu = { type: MENU_TYPES[0].value };

const InitialTheme =  THEMES.NOKIA


export {
    BRAND_LOGOS,
    EXTENDED_COLORS,
    ICON_AND_TEXT_STYLES,
    THEMES,
    MENU_TYPES,
    InitialMenu,
    InitialTheme
};