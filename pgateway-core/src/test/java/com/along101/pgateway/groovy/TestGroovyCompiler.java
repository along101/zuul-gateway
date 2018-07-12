package com.along101.pgateway.groovy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;

import org.junit.Test;

import groovy.lang.GroovyObject;

public class TestGroovyCompiler {
	  @Test
      public void testLoadGroovyFromString() {

          GroovyCompiler compiler = spy(new GroovyCompiler());

          try {

              String code = "class test { public String hello(){return \"hello\" } } ";
              Class clazz = compiler.compile(code, "test");
              assertNotNull(clazz);
              assertEquals(clazz.getName(), "test");
              GroovyObject groovyObject = (GroovyObject) clazz.newInstance();
              Object[] args = {};
              String s = (String) groovyObject.invokeMethod("hello", args);
              assertEquals(s, "hello");


          } catch (Exception e) {
              assertFalse(true);
          }

      }
}
