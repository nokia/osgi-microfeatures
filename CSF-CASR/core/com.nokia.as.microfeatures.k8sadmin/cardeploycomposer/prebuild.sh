#!/bin/bash
#################################################################################################
# This script is used to patch the node_modules/react-scripts/config/webpack.config.prod.js file.
# The file is used to configure the build task to produce a web application in production.
#
# This patch declares to the babel configuration a plugin which must be installed first as a 
# devDependencies ( see package.json ) : babel-plugin-transform-remove-console
# This plugin allows to remove all calls of the console.log method
# ( but keeps the console.error and console.warn ) in production
#
# Patching the file avoids to 'eject' this project created with the 'create-creact-app' script
#
################################################################################################

file=$(find . -name webpack.config.prod.js | grep node_modules/react-scripts/config)

foo=/tmp/foo
echo "Checking the babel configuration to remove all console.log calls in production..."

if [ ! -f "$file" ]; then

  echo "ERROR UPON BABEL CONFIGURATION : webpack.config.prod.js not found! console.log will be not removed in production."

else

  grep  -q '"plugins": \[ \["transform-remove-console", { "exclude": \[ "error", "warn"] }] ]' $file

  if [ $? != 0 ]; then

    echo '              "plugins": [ ["transform-remove-console", { "exclude": [ "error", "warn"] }] ],' > $foo

    if [ $? != 0 ]; then

      echo "ERROR UPON BABEL CONFIGURATION :Cannot create tmp file! console.log will be not removed in production."

    else

      sed -i -e "/babelrc: false/r $foo" $file

      if [ $? != 0 ]; then
        
        echo "ERROR UPON BABEL CONFIGURATION : sed failed! console.log will be not removed in production."

      else

        echo "BABEL CONFIGURATION OK ( Plugin added )"

      fi

    fi

  else

    echo "BABEL CONFIGURATION OK ( plugins already added )"

  fi

fi
