Place your non-OSGI JAR or WAR archives in this directory. They will be automatically
transformed into an OSGi bundle and then installed in the framework.

If a .bnd file is found in the directory matching your JAR or WAR filename
(for example test.jar and test.bnd), then this file will be used as an instruction list 
for customizing the OSGi bundling process.