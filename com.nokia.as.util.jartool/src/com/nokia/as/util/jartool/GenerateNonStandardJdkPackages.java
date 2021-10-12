// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.jartool;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class is used to generate the list of all jre packages which are not exported by default in OSGi.
 * The class generates a list of jre packages which can then be used to create a "system" fragment bundle
 * (see com.nokia.as.osgi.jre18 for example).
 * The generated packages can alternatively be added in felix.properties (see org.osgi.framework.system.packages.extra property).
 */
public class GenerateNonStandardJdkPackages {

	// List of standard pkgs that are reexported by felix. This list has been obtained using "headers 0" gogo command with felix 6.0.1
	private final static Set<String> _standardPkg_1_8 = new HashSet<>(Arrays.asList("org.osgi.framework",
			"org.osgi.framework.dto", "org.osgi.framework.hooks.bundle", "org.osgi.framework.hooks.resolver",
			"org.osgi.framework.hooks.service", "org.osgi.framework.hooks.weaving", "org.osgi.framework.launch",
			"org.osgi.framework.namespace", "org.osgi.framework.startlevel", "org.osgi.framework.startlevel.dto",
			"org.osgi.framework.wiring", "org.osgi.framework.wiring.dto", "org.osgi.resource", "org.osgi.resource.dto",
			"org.osgi.service.packageadmin", "org.osgi.service.startlevel", "org.osgi.service.url",
			"org.osgi.service.resolver", "org.osgi.util.tracker", "org.osgi.dto", "java.applet", "java.awt",
			"java.awt.color", "java.awt.datatransfer", "java.awt.dnd", "java.awt.event", "java.awt.font",
			"java.awt.geom", "java.awt.im", "java.awt.im.spi", "java.awt.image", "java.awt.image.renderable",
			"java.awt.print", "java.beans", "java.beans.beancontext", "java.io", "java.lang", "java.lang.annotation",
			"java.lang.instrument", "java.lang.management", "java.lang.ref", "java.lang.reflect", "java.math",
			"java.net", "java.nio", "java.nio.channels", "java.nio.channels.spi", "java.nio.charset",
			"java.nio.charset.spi", "java.rmi", "java.rmi.activation", "java.rmi.dgc", "java.rmi.registry",
			"java.rmi.server", "java.security", "java.security.acl", "java.security.cert", "java.security.interfaces",
			"java.security.spec", "java.sql", "java.text", "java.text.spi", "java.util", "java.util.concurrent",
			"java.util.concurrent.atomic", "java.util.concurrent.locks", "java.util.jar", "java.util.logging",
			"java.util.prefs", "java.util.regex", "java.util.spi", "java.util.zip", "javax.accessibility",
			"javax.activation", "javax.activity", "javax.annotation", "javax.annotation.processing",
			"javax.crypto.interfaces", "javax.crypto.spec", "javax.imageio", "javax.imageio.event",
			"javax.imageio.metadata", "javax.imageio.plugins.bmp", "javax.imageio.plugins.jpeg", "javax.imageio.spi",
			"javax.imageio.stream", "javax.jws", "javax.jws.soap", "javax.lang.model.util", "javax.management",
			"javax.management.loading", "javax.management.modelmbean", "javax.management.openmbean",
			"javax.management.relation", "javax.management.remote", "javax.management.remote.rmi",
			"javax.management.timer", "javax.naming", "javax.naming.directory", "javax.naming.event",
			"javax.naming.ldap", "javax.naming.spi", "javax.net", "javax.net.ssl", "javax.print",
			"javax.print.attribute", "javax.print.attribute.standard", "javax.print.event", "javax.rmi",
			"javax.rmi.CORBA", "javax.rmi.ssl", "javax.script", "javax.security.auth", "javax.security.auth.callback",
			"javax.security.auth.kerberos", "javax.security.auth.login", "javax.security.auth.spi",
			"javax.security.auth.x500", "javax.security.cert", "javax.security.sasl", "javax.sound.midi",
			"javax.sound.midi.spi", "javax.sound.sampled", "javax.sound.sampled.spi", "javax.sql", "javax.sql.rowset",
			"javax.sql.rowset.serial", "javax.sql.rowset.spi", "javax.swing", "javax.swing.border", "javax.swing.event",
			"javax.swing.filechooser", "javax.swing.plaf", "javax.swing.plaf.metal", "javax.swing.plaf.multi",
			"javax.swing.table", "javax.swing.text", "javax.swing.text.html", "javax.swing.text.html.parser",
			"javax.swing.text.rtf", "javax.swing.tree", "javax.swing.undo", "javax.tools", "javax.transaction",
			"javax.transaction.xa", "javax.xml", "javax.xml.bind", "javax.xml.bind.annotation",
			"javax.xml.bind.annotation.adapters", "javax.xml.bind.attachment", "javax.xml.bind.helpers",
			"javax.xml.bind.util", "javax.xml.crypto", "javax.xml.crypto.dom", "javax.xml.crypto.dsig",
			"javax.xml.crypto.dsig.dom", "javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto.dsig.spec",
			"javax.xml.datatype", "javax.xml.namespace", "javax.xml.parsers", "javax.xml.soap", "javax.xml.stream",
			"javax.xml.stream.events", "javax.xml.stream.util", "javax.xml.transform", "javax.xml.transform.dom",
			"javax.xml.transform.sax", "javax.xml.transform.stax", "javax.xml.transform.stream", "javax.xml.validation",
			"javax.xml.ws.handler", "javax.xml.ws.handler.soap", "javax.xml.ws.http", "javax.xml.ws.soap",
			"javax.xml.ws.spi", "javax.xml.ws.wsaddressing", "javax.xml.xpath", "org.ietf.jgss", "org.omg.CORBA",
			"org.omg.CORBA.DynAnyPackage", "org.omg.CORBA.ORBPackage", "org.omg.CORBA.TypeCodePackage",
			"org.omg.CORBA.portable", "org.omg.CORBA_2_3", "org.omg.CORBA_2_3.portable", "org.omg.CosNaming",
			"org.omg.CosNaming.NamingContextExtPackage", "org.omg.CosNaming.NamingContextPackage", "org.omg.Dynamic",
			"org.omg.DynamicAny", "org.omg.DynamicAny.DynAnyFactoryPackage", "org.omg.DynamicAny.DynAnyPackage",
			"org.omg.IOP", "org.omg.IOP.CodecFactoryPackage", "org.omg.IOP.CodecPackage", "org.omg.Messaging",
			"org.omg.PortableInterceptor", "org.omg.PortableInterceptor.ORBInitInfoPackage", "org.omg.PortableServer",
			"org.omg.PortableServer.CurrentPackage", "org.omg.PortableServer.POAManagerPackage",
			"org.omg.PortableServer.POAPackage", "org.omg.PortableServer.ServantLocatorPackage",
			"org.omg.PortableServer.portable", "org.omg.SendingContext", "org.omg.stub.java.rmi", "org.w3c.dom",
			"org.w3c.dom.bootstrap", "org.w3c.dom.ls", "org.w3c.dom.events", "org.w3c.dom.views",
			"org.w3c.dom.traversal", "org.w3c.dom.ranges", "org.w3c.dom.css", "org.w3c.dom.html",
			"org.w3c.dom.stylesheets", "org.xml.sax", "org.xml.sax.ext", "org.xml.sax.helpers", "java.lang.invoke",
			"java.nio.file", "java.nio.file.attribute", "java.nio.file.spi", "javax.lang.model.element",
			"javax.lang.model.type", "javax.management.monitor", "javax.swing.colorchooser", "javax.swing.plaf.basic",
			"javax.swing.plaf.nimbus", "javax.swing.plaf.synth", "javax.xml.ws", "javax.xml.ws.spi.http", "java.time",
			"java.time.chrono", "java.time.format", "java.time.temporal", "java.time.zone", "java.util.function",
			"java.util.stream", "javax.crypto", "javax.lang.model"));
	
	private final static Set<String> _standardPkg_1_9 = new HashSet<>(Arrays.asList("com.oracle.awt", "com.oracle.net",
			"com.oracle.nio", "com.oracle.tools.packager", "com.sun.jarsigner", "com.sun.java.accessibility.util",
			"com.sun.java.browser.plugin2", "com.sun.javadoc", "com.sun.javafx.tools.packager",
			"com.sun.javafx.tools.packager.bundlers", "com.sun.javafx.tools.resource", "com.sun.jdi",
			"com.sun.jdi.connect", "com.sun.jdi.connect.spi", "com.sun.jdi.event", "com.sun.jdi.request",
			"com.sun.management", "com.sun.net.httpserver", "com.sun.net.httpserver.spi", "com.sun.nio.file",
			"com.sun.nio.sctp", "com.sun.security.auth", "com.sun.security.auth.callback",
			"com.sun.security.auth.login", "com.sun.security.auth.module", "com.sun.security.jgss",
			"com.sun.source.doctree", "com.sun.source.tree", "com.sun.source.util", "com.sun.tools.attach",
			"com.sun.tools.attach.spi", "com.sun.tools.doclets", "com.sun.tools.doclets.standard",
			"com.sun.tools.javac", "com.sun.tools.javadoc", "com.sun.tools.jconsole", "java.applet", "java.awt",
			"java.awt.color", "java.awt.datatransfer", "java.awt.desktop", "java.awt.dnd", "java.awt.event",
			"java.awt.font", "java.awt.geom", "java.awt.im", "java.awt.image", "java.awt.image.renderable",
			"java.awt.im.spi", "java.awt.print", "java.beans", "java.beans.beancontext", "javafx.animation",
			"javafx.application", "javafx.beans", "javafx.beans.binding", "javafx.beans.property",
			"javafx.beans.property.adapter", "javafx.beans.value", "javafx.collections",
			"javafx.collections.transformation", "javafx.concurrent", "javafx.css", "javafx.css.converter",
			"javafx.embed.swing", "javafx.event", "javafx.fxml", "javafx.geometry", "javafx.print", "javafx.scene",
			"javafx.scene.canvas", "javafx.scene.chart", "javafx.scene.control", "javafx.scene.control.cell",
			"javafx.scene.control.skin", "javafx.scene.effect", "javafx.scene.image", "javafx.scene.input",
			"javafx.scene.layout", "javafx.scene.media", "javafx.scene.paint", "javafx.scene.shape",
			"javafx.scene.text", "javafx.scene.transform", "javafx.scene.web", "javafx.stage", "javafx.util",
			"javafx.util.converter", "java.io", "java.lang", "java.lang.annotation", "java.lang.instrument",
			"java.lang.invoke", "java.lang.management", "java.lang.module", "java.lang.ref", "java.lang.reflect",
			"java.math", "java.net", "java.net.spi", "java.nio", "java.nio.channels", "java.nio.channels.spi",
			"java.nio.charset", "java.nio.charset.spi", "java.nio.file", "java.nio.file.attribute", "java.nio.file.spi",
			"java.rmi", "java.rmi.activation", "java.rmi.dgc", "java.rmi.registry", "java.rmi.server", "java.security",
			"java.security.acl", "java.security.cert", "java.security.interfaces", "java.security.spec", "java.sql",
			"java.text", "java.text.spi", "java.time", "java.time.chrono", "java.time.format", "java.time.temporal",
			"java.time.zone", "java.util", "java.util.concurrent", "java.util.concurrent.atomic",
			"java.util.concurrent.locks", "java.util.function", "java.util.jar", "java.util.logging", "java.util.prefs",
			"java.util.regex", "java.util.spi", "java.util.stream", "java.util.zip", "javax.accessibility",
			"javax.annotation.processing", "javax.crypto", "javax.crypto.interfaces", "javax.crypto.spec",
			"javax.imageio", "javax.imageio.event", "javax.imageio.metadata", "javax.imageio.plugins.bmp",
			"javax.imageio.plugins.jpeg", "javax.imageio.plugins.tiff", "javax.imageio.spi", "javax.imageio.stream",
			"javax.jnlp", "javax.lang.model", "javax.lang.model.element", "javax.lang.model.type",
			"javax.lang.model.util", "javax.management", "javax.management.loading", "javax.management.modelmbean",
			"javax.management.monitor", "javax.management.openmbean", "javax.management.relation",
			"javax.management.remote", "javax.management.remote.rmi", "javax.management.timer", "javax.naming",
			"javax.naming.directory", "javax.naming.event", "javax.naming.ldap", "javax.naming.spi", "javax.net",
			"javax.net.ssl", "javax.print", "javax.print.attribute", "javax.print.attribute.standard",
			"javax.print.event", "javax.rmi.ssl", "javax.script", "javax.security.auth", "javax.security.auth.callback",
			"javax.security.auth.kerberos", "javax.security.auth.login", "javax.security.auth.spi",
			"javax.security.auth.x500", "javax.security.cert", "javax.security.sasl", "javax.smartcardio",
			"javax.sound.midi", "javax.sound.midi.spi", "javax.sound.sampled", "javax.sound.sampled.spi", "javax.sql",
			"javax.sql.rowset", "javax.sql.rowset.serial", "javax.sql.rowset.spi", "javax.swing", "javax.swing.border",
			"javax.swing.colorchooser", "javax.swing.event", "javax.swing.filechooser", "javax.swing.plaf",
			"javax.swing.plaf.basic", "javax.swing.plaf.metal", "javax.swing.plaf.multi", "javax.swing.plaf.nimbus",
			"javax.swing.plaf.synth", "javax.swing.table", "javax.swing.text", "javax.swing.text.html",
			"javax.swing.text.html.parser", "javax.swing.text.rtf", "javax.swing.tree", "javax.swing.undo",
			"javax.tools", "javax.transaction.xa", "javax.xml", "javax.xml.catalog", "javax.xml.crypto",
			"javax.xml.crypto.dom", "javax.xml.crypto.dsig", "javax.xml.crypto.dsig.dom",
			"javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto.dsig.spec", "javax.xml.datatype", "javax.xml.namespace",
			"javax.xml.parsers", "javax.xml.stream", "javax.xml.stream.events", "javax.xml.stream.util",
			"javax.xml.transform", "javax.xml.transform.dom", "javax.xml.transform.sax", "javax.xml.transform.stax",
			"javax.xml.transform.stream", "javax.xml.validation", "javax.xml.xpath", "jdk.dynalink",
			"jdk.dynalink.beans", "jdk.dynalink.linker", "jdk.dynalink.linker.support", "jdk.dynalink.support",
			"jdk.javadoc.doclet", "jdk.jfr", "jdk.jfr.consumer", "jdk.jshell", "jdk.jshell.execution", "jdk.jshell.spi",
			"jdk.jshell.tool", "jdk.management.cmm", "jdk.management.jfr", "jdk.management.resource",
			"jdk.nashorn.api.scripting", "jdk.nashorn.api.tree", "jdk.net", "jdk.packager.services",
			"jdk.security.jarsigner", "netscape.javascript", "org.ietf.jgss", "org.w3c.dom", "org.w3c.dom.bootstrap", "org.w3c.dom.css",
			"org.w3c.dom.events", "org.w3c.dom.html", "org.w3c.dom.ls", "org.w3c.dom.ranges", "org.w3c.dom.stylesheets",
			"org.w3c.dom.traversal", "org.w3c.dom.views", "org.w3c.dom.xpath", "org.xml.sax", "org.xml.sax.ext",
			"org.xml.sax.helpers", "sun.misc", "sun.reflect"));
	
	private final static Set<String> _standardPkg_1_10 = new HashSet<>(Arrays.asList("java.util.spi", "java.text.spi",
			"java.nio", "java.util.zip", "java.security.cert", "java.nio.file", "java.security.spec",
			"java.nio.file.spi", "java.lang.ref", "java.time", "java.util.regex", "java.net",
			"javax.security.auth.callback", "java.security.interfaces", "java.lang.reflect", "java.security",
			"java.time.format", "java.util.concurrent", "javax.security.auth.spi", "java.util.concurrent.locks",
			"java.lang", "javax.net", "java.net.spi", "java.util.function", "javax.crypto", "java.util",
			"javax.security.auth.x500", "java.security.acl", "java.util.stream", "java.lang.annotation",
			"java.nio.channels", "java.time.temporal", "java.nio.charset", "java.time.chrono", "java.time.zone",
			"javax.security.auth", "java.lang.invoke", "java.math", "javax.security.cert",
			"java.util.concurrent.atomic", "javax.crypto.spec", "java.lang.module", "java.text", "java.io",
			"java.nio.channels.spi", "java.util.jar", "javax.crypto.interfaces", "javax.net.ssl",
			"javax.security.auth.login", "java.nio.charset.spi", "java.nio.file.attribute",
			"javax.annotation.processing", "javax.lang.model.util", "javax.tools", "javax.lang.model",
			"javax.lang.model.type", "javax.lang.model.element", "java.awt.datatransfer", "java.applet",
			"java.awt.image", "java.beans", "javax.swing.table", "java.awt.desktop", "javax.print.event",
			"javax.imageio.plugins.jpeg", "javax.imageio.metadata", "javax.accessibility", "java.awt.font",
			"javax.sound.midi.spi", "javax.swing.filechooser", "javax.imageio.plugins.bmp", "javax.sound.sampled.spi",
			"javax.print.attribute.standard", "javax.imageio", "javax.swing.text", "javax.sound.sampled",
			"javax.swing.plaf.synth", "javax.swing.plaf", "javax.swing.text.html", "javax.swing.plaf.nimbus",
			"java.awt.event", "javax.swing.plaf.basic", "javax.swing.border", "javax.swing.tree",
			"javax.swing.plaf.metal", "java.awt.image.renderable", "javax.imageio.plugins.tiff",
			"javax.swing.colorchooser", "javax.imageio.spi", "javax.sound.midi", "javax.imageio.stream",
			"java.awt.im.spi", "java.beans.beancontext", "java.awt.geom", "java.awt.dnd", "javax.swing.event",
			"java.awt.im", "javax.swing.text.html.parser", "java.awt.color", "java.awt", "javax.swing.text.rtf",
			"javax.swing.undo", "java.awt.print", "javax.swing.plaf.multi", "javax.imageio.event", "javax.print",
			"javax.print.attribute", "javax.swing", "java.lang.instrument", "javax.jnlp", "java.util.logging",
			"javax.management.modelmbean", "java.lang.management", "javax.management.timer", "javax.management",
			"javax.management.monitor", "javax.management.loading", "javax.management.openmbean",
			"javax.management.remote", "javax.management.relation", "javax.management.remote.rmi",
			"javax.naming.directory", "javax.naming.ldap", "javax.naming.spi", "javax.naming", "javax.naming.event",
			"java.util.prefs", "java.rmi", "javax.rmi.ssl", "java.rmi.server", "java.rmi.registry",
			"java.rmi.activation", "java.rmi.dgc", "javax.script", "org.ietf.jgss", "javax.security.auth.kerberos",
			"javax.security.sasl", "javax.smartcardio", "java.sql", "javax.transaction.xa", "javax.sql",
			"javax.sql.rowset", "javax.sql.rowset.spi", "javax.sql.rowset.serial", "javax.xml", "org.xml.sax.ext",
			"org.w3c.dom.ls", "javax.xml.stream", "javax.xml.transform.dom", "javax.xml.xpath", "org.w3c.dom.views",
			"javax.xml.namespace", "org.xml.sax", "javax.xml.stream.util", "org.w3c.dom.bootstrap", "javax.xml.catalog",
			"org.xml.sax.helpers", "javax.xml.validation", "org.w3c.dom", "javax.xml.transform.sax",
			"javax.xml.transform.stax", "javax.xml.parsers", "javax.xml.transform", "org.w3c.dom.traversal",
			"org.w3c.dom.events", "javax.xml.transform.stream", "javax.xml.datatype", "javax.xml.stream.events",
			"org.w3c.dom.ranges", "javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto", "javax.xml.crypto.dsig.dom",
			"javax.xml.crypto.dsig", "javax.xml.crypto.dom", "javax.xml.crypto.dsig.spec", "javafx.collections",
			"javafx.beans.property", "javafx.beans.binding", "javafx.event", "javafx.beans.value",
			"javafx.collections.transformation", "javafx.beans", "javafx.beans.property.adapter",
			"javafx.util.converter", "javafx.util", "javafx.scene.control.cell", "javafx.scene.control",
			"javafx.scene.chart", "javafx.scene.control.skin", "javafx.fxml", "javafx.scene.input",
			"javafx.scene.effect", "javafx.scene.text", "javafx.scene.layout", "javafx.animation", "javafx.concurrent",
			"javafx.scene.transform", "javafx.application", "javafx.scene.paint", "javafx.stage", "javafx.scene.shape",
			"javafx.print", "javafx.scene.image", "javafx.geometry", "javafx.css", "javafx.css.converter",
			"javafx.scene.canvas", "javafx.scene", "javafx.scene.media", "javafx.embed.swing", "javafx.scene.web",
			"com.sun.java.accessibility.util", "com.sun.tools.attach", "com.sun.tools.attach.spi",
			"com.sun.source.util", "com.sun.source.tree", "com.sun.tools.javac", "com.sun.source.doctree",
			"jdk.dynalink.support", "jdk.dynalink.beans", "jdk.dynalink", "jdk.dynalink.linker.support",
			"jdk.dynalink.linker", "com.sun.net.httpserver.spi", "com.sun.net.httpserver", "jdk.security.jarsigner",
			"com.sun.jarsigner", "jdk.javadoc.doclet", "com.sun.tools.javadoc", "com.sun.javadoc",
			"com.sun.tools.jconsole", "com.sun.jdi.event", "com.sun.jdi.connect.spi", "com.sun.jdi",
			"com.sun.jdi.request", "com.sun.jdi.connect", "jdk.jfr", "jdk.jfr.consumer", "jdk.jshell", "jdk.jshell.spi",
			"jdk.jshell.execution", "jdk.jshell.tool", "netscape.javascript", "com.sun.management",
			"jdk.management.cmm", "jdk.management.jfr", "jdk.management.resource", "jdk.net",
			"com.sun.javafx.tools.packager", "com.sun.javafx.tools.packager.bundlers", "com.sun.javafx.tools.resource",
			"com.oracle.tools.packager", "jdk.packager.services.singleton", "jdk.packager.services",
			"jdk.nashorn.api.scripting", "jdk.nashorn.api.tree", "com.sun.nio.sctp", "com.sun.security.auth.module",
			"com.sun.security.auth.callback", "com.sun.security.auth", "com.sun.security.auth.login",
			"com.sun.security.jgss", "com.sun.nio.file", "sun.reflect", "sun.misc", "org.w3c.dom.xpath",
			"org.w3c.dom.css", "org.w3c.dom.html", "org.w3c.dom.stylesheets", "com.oracle.awt", "com.oracle.net",
			"com.oracle.nio"));
	
	private final static Set<String> _standardPkg_1_11 = new HashSet<>(Arrays.asList("java.text.spi", "java.util.spi",
			"java.nio", "java.util.zip", "java.security.cert", "java.nio.file", "java.security.spec",
			"java.nio.file.spi", "java.time", "java.lang.ref", "java.net", "java.util.regex",
			"javax.security.auth.callback", "java.security.interfaces", "java.lang.reflect", "java.security",
			"java.time.format", "java.util.concurrent", "javax.security.auth.spi", "java.util.concurrent.locks",
			"java.lang", "java.net.spi", "java.util.function", "javax.net", "javax.crypto", "java.util",
			"javax.security.auth.x500", "java.security.acl", "java.util.stream", "java.lang.annotation",
			"java.time.temporal", "java.nio.channels", "java.nio.charset", "java.time.chrono", "java.time.zone",
			"javax.security.auth", "java.lang.invoke", "java.math", "javax.security.cert",
			"java.util.concurrent.atomic", "javax.crypto.spec", "java.lang.module", "java.text", "java.io",
			"java.util.jar", "java.nio.channels.spi", "javax.crypto.interfaces", "javax.net.ssl",
			"java.nio.charset.spi", "javax.security.auth.login", "java.nio.file.attribute",
			"javax.annotation.processing", "javax.lang.model.util", "javax.tools", "javax.lang.model",
			"javax.lang.model.type", "javax.lang.model.element", "java.awt.datatransfer", "java.applet",
			"java.awt.image", "java.beans", "javax.swing.table", "java.awt.desktop", "javax.imageio.plugins.jpeg",
			"javax.print.event", "javax.imageio.metadata", "javax.accessibility", "java.awt.font",
			"javax.sound.midi.spi", "javax.swing.filechooser", "javax.imageio.plugins.bmp", "javax.sound.sampled.spi",
			"javax.print.attribute.standard", "javax.imageio", "javax.swing.text", "javax.sound.sampled",
			"javax.swing.plaf.synth", "javax.swing.plaf", "javax.swing.text.html", "javax.swing.plaf.nimbus",
			"javax.swing.plaf.basic", "java.awt.event", "javax.swing.border", "javax.swing.tree",
			"javax.swing.plaf.metal", "java.awt.image.renderable", "javax.imageio.plugins.tiff",
			"javax.swing.colorchooser", "javax.imageio.spi", "javax.sound.midi", "javax.imageio.stream",
			"java.awt.im.spi", "java.beans.beancontext", "java.awt.dnd", "java.awt.geom", "javax.swing.event",
			"java.awt.im", "javax.swing.text.html.parser", "java.awt.color", "java.awt", "javax.swing.text.rtf",
			"javax.swing.undo", "java.awt.print", "javax.swing.plaf.multi", "javax.imageio.event", "javax.print",
			"javax.print.attribute", "javax.swing", "java.lang.instrument", "java.util.logging",
			"javax.management.modelmbean", "java.lang.management", "javax.management.monitor", "javax.management",
			"javax.management.timer", "javax.management.loading", "javax.management.openmbean",
			"javax.management.remote", "javax.management.relation", "javax.management.remote.rmi",
			"javax.naming.directory", "javax.naming.ldap", "javax.naming.spi", "javax.naming", "javax.naming.event",
			"java.net.http", "java.util.prefs", "java.rmi", "javax.rmi.ssl", "java.rmi.server", "java.rmi.activation",
			"java.rmi.registry", "java.rmi.dgc", "javax.script", "org.ietf.jgss", "javax.security.auth.kerberos",
			"javax.security.sasl", "javax.smartcardio", "java.sql", "javax.sql", "javax.sql.rowset",
			"javax.sql.rowset.spi", "javax.sql.rowset.serial", "javax.transaction.xa", "javax.xml", "org.xml.sax.ext",
			"org.w3c.dom.ls", "javax.xml.stream", "javax.xml.transform.dom", "javax.xml.xpath", "org.w3c.dom.views",
			"javax.xml.namespace", "org.xml.sax", "javax.xml.stream.util", "org.w3c.dom.bootstrap", "javax.xml.catalog",
			"org.xml.sax.helpers", "org.w3c.dom", "javax.xml.validation", "javax.xml.transform.sax",
			"javax.xml.transform.stax", "javax.xml.parsers", "javax.xml.transform", "org.w3c.dom.traversal",
			"org.w3c.dom.events", "javax.xml.transform.stream", "javax.xml.datatype", "org.w3c.dom.ranges",
			"javax.xml.stream.events", "javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto", "javax.xml.crypto.dsig.dom",
			"javax.xml.crypto.dom", "javax.xml.crypto.dsig", "javax.xml.crypto.dsig.spec",
			"com.sun.java.accessibility.util", "com.sun.tools.attach", "com.sun.tools.attach.spi",
			"com.sun.source.util", "com.sun.source.tree", "com.sun.tools.javac", "com.sun.source.doctree",
			"jdk.dynalink.support", "jdk.dynalink.beans", "jdk.dynalink", "jdk.dynalink.linker.support",
			"jdk.dynalink.linker", "com.sun.net.httpserver.spi", "com.sun.net.httpserver", "jdk.security.jarsigner",
			"com.sun.jarsigner", "jdk.javadoc.doclet", "com.sun.tools.javadoc", "com.sun.javadoc",
			"com.sun.tools.jconsole", "com.sun.jdi.event", "com.sun.jdi.connect.spi", "com.sun.jdi",
			"com.sun.jdi.request", "com.sun.jdi.connect", "jdk.jfr", "jdk.jfr.consumer", "jdk.jshell",
			"jdk.jshell.execution", "jdk.jshell.spi", "jdk.jshell.tool", "netscape.javascript", "com.sun.management",
			"jdk.management.jfr", "jdk.net", "jdk.nio", "jdk.nashorn.api.scripting", "jdk.nashorn.api.tree",
			"com.sun.nio.sctp", "com.sun.security.auth.module", "com.sun.security.auth.callback",
			"com.sun.security.auth", "com.sun.security.auth.login", "com.sun.security.jgss", "com.sun.nio.file",
			"sun.reflect", "sun.misc", "jdk.swing.interop", "org.w3c.dom.xpath", "org.w3c.dom.css", "org.w3c.dom.html",
			"org.w3c.dom.stylesheets"));

	private final static Set<String> _pkgs = new TreeSet<>();

	public static void main(String[] args) throws IOException {
		String javaVersion = System.getProperty("java.specification.version", "1.8");
		System.out.println("Geneting extra packages for java " + javaVersion);

		Path path = Paths.get(args[0]);
		listFiles(path);
		
		System.out.println("jre=\\");
		System.out.println(" com.nokia.as.dtls.provider; \\");
		for (String pkg : _pkgs) {
			System.out.println(" " + pkg + "; \\");
		}
		System.out.println(" version=\"0.0.0\"");
	}

	static void listFiles(Path path) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					listFiles(entry);
				}
				dumpPackagesFromJar(entry);
			}
		}
	}

	private static void dumpPackagesFromJar(Path entry) throws IOException {
	        String file = entry.toFile().toString();
		if (! file.endsWith(".jar") && ! file.endsWith("jmod")) {
			return;
		}
		JarFile jarFile = new JarFile(entry.toFile());
		Enumeration e = jarFile.entries();
		Set<String> standardPkg = getStandardPkg();
		while (e.hasMoreElements()) {
			process(standardPkg, e.nextElement());
		}
	}

	private static Set<String> getStandardPkg() {
		String javaVersion = System.getProperty("java.version", "1.8");

		if (javaVersion.startsWith("1.8")) {
			return _standardPkg_1_8;
		} else if (javaVersion.startsWith("9")) {
			return _standardPkg_1_9;
		} else if (javaVersion.startsWith("10")) {
			return _standardPkg_1_10;
		} else if (javaVersion.startsWith("11")) {
			return _standardPkg_1_11;
		} else {
			throw new IllegalStateException("java version unsupported: " + javaVersion);
		}
	}

	private static void process(Set<String> standardPkgs, Object obj) {
		JarEntry entry = (JarEntry) obj;
		String name = entry.getName();
		if (name.endsWith(".class")) {
			int lastIndex = name.lastIndexOf("/");
			if (lastIndex != -1) {
				String pkg = name.substring(0, lastIndex).replace("/", ".");
				if (! standardPkgs.contains(pkg) && ! pkg.startsWith("java.")) {
					_pkgs.add(pkg);
				}
			}
		}
	}

}
