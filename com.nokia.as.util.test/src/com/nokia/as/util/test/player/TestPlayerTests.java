package com.nokia.as.util.test.player;

import java.util.*;
import java.io.*;

import org.junit.*;
import org.junit.runner.*;

public class TestPlayerTests extends TestPlayer {

    public String getMethod (String cmd){
	if (cmd.equals ("false")) return "_false";
	if (cmd.equals ("true")) return "_true";
	return super.getMethod (cmd);
    }

    @Test
    public void testParse1() throws Exception {
	log ("TestPlayerTests.testParse1");
	set ("test", "acme");
	Assert.assertEquals (get ("$test"), "acme");
	Assert.assertEquals (get ("\\$test"), "$test");
	Assert.assertEquals (get ("$(test)"), "acme");
	Assert.assertEquals (get ("\\$(test)"), "$(test)");
	Assert.assertEquals (get ("X$test"), "Xacme");
	Assert.assertEquals (get ("X\\$test"), "X$test");
	Assert.assertEquals (get ("X$(test)Y"), "XacmeY");
	Assert.assertEquals (get ("X\\$(test)Y"), "X$(test)Y");
    }
    @Test
    public void testParse2() throws Exception {
	log ("TestPlayerTests.testParse2");
	set ("test", "acme");
	Assert.assertEquals (escape ("$test"), "\\$test");
	Assert.assertEquals (escape ("$(test)"), "\\$(test)");
	Assert.assertEquals (escape ("test$"), "test\\$");
	Assert.assertEquals (escape ("X$test"), "X\\$test");
	Assert.assertEquals (escape ("\\$test"), "\\$test");
    }
    @Test
    public void testEngine1() throws Exception {
	log ("TestPlayerTests.testEngine1");
	Assert.assertTrue (play ("set: test acme"));
	Assert.assertTrue (play ("equal: $test acme"));
	Assert.assertTrue (play ("set: test"));
	Assert.assertTrue (play ("equal: $test"));
	Assert.assertTrue (play ("equal: X$test X"));
    }
    @Test
    public void testEngine2() throws Exception {
	log ("TestPlayerTests.testEngine2");
	Assert.assertTrue (play ("for: iter 2 \\ endfor: iter"));
    }
     @Test
    public void testEngine3() throws Exception {
	log ("TestPlayerTests.testEngine3");
	Assert.assertTrue (play ("set: test acme"));
	Assert.assertTrue (play ("like: $test .*cm.*"));
    }
    @Test
    public void testIO1() throws Exception {
	log ("TestPlayerTests.testIO1");
	Assert.assertTrue (play ("set: @test acme"));
	Assert.assertTrue (play ("equal: @test acme"));
	Assert.assertTrue (play ("set: @test"));
	Assert.assertTrue (play ("equal: @test"));
	Assert.assertTrue (play ("equal: X@test X"));
	Assert.assertTrue (play ("set: @test1 acme"));
	Assert.assertTrue (play ("set: content acme"));
	Assert.assertTrue (play ("set: @test2 $content"));
	Assert.assertTrue (play ("equal: @test1 @test2"));
	Assert.assertTrue (play ("set: @test1"));
	Assert.assertTrue (play ("set: @test2"));
    }

    public boolean exception (String s) throws Exception {
	throw new Exception ("TestPlayerTests.exception");
    }

    public boolean _false (String s) throws Exception {
	return false;
    }
    public boolean _true (String s) throws Exception {
	return true;
    }
    
}