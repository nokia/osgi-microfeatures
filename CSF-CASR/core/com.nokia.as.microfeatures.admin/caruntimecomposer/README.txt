This project contains the new GUI for microfeatures.

This project was bootstrapped with [Create React App](https://github.com/facebookincubator/create-react-app).

After extracting this React project from GIT, it must be first compiled in order to be packaged from
the com.nokia.as.features.admin java project which ships the javascript part of the GUI.
Once compiled, a 'build' folder contains the whole javascript, css and images which can be use as resources from
com.nokia.as.features.admin point of view.
Indeed, the com.nokia.as.features.admin bnd file uses the property :
    -includeresource: resources = ../caruntimecomposer/build
to build the entire project to generate the jar file.

Prerequisites:
-1°) Make sure you have a recent version of Node.js installed.See https://nodejs.org/en/ ,
Recommended by UXR team : Node ~8.9.0 ( includes npm ^5.6.0) LTS versions are recommended.

-2°) Install the Create React App script in your global npm repository
npm install -g create-react-app


Before compiling this React project, some commands should be done such as:
    a°) cd caruntimecomposer
    b°) npm install
(Forget error message such as :[server error] Cannot load the stats for react-slick – please try again later )
    c°)To compile:
    - cd caruntimecomposer
    - npm run build

Remark: For performance consideration, we use the 'babel-plugin-transform-remove-console' plugin
to remove all console.log calls when building this webapp. This is done via the build command
which use a shell script first.
See the build command in the package.json :
                    "build": "sh prebuild.sh;react-scripts build"

To see in details what the prebuild.sh done, see its comments.


For development only :
=> npm start : Allows to start a server on localhost:3000 and automatically reload the browser on any update of source files.

To update csfWidgets version to the last release:
    - npm uninstall @nokia-csf-uxr/csfWidgets ; npm install @nokia-csf-uxr/csfWidgets


/**** DEPRECATED **********
The following patch is automatically done via the build task ( see c°) )
    d°) Check the node_modules/react-scripts/config/webpack.config.prod.js (*)
    Search the part where babelrc is used and add the 'plugins:[ "transform-remove-console"]' if not present.
    options: {
              // @remove-on-eject-begin
              babelrc: false,
              presets: [require.resolve('babel-preset-react-app')],
              // @remove-on-eject-end
              compact: true,
            plugins:[ "transform-remove-console"]
            },
    This allows to supress the calls to the console.log method for production purpose ( i.e when you compile this project)
*/