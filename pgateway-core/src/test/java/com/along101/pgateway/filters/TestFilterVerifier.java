package com.along101.pgateway.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.along101.pgateway.common.FilterInfo;

public class TestFilterVerifier {


    String sGoodGroovyScriptFilter = "import com.along101.pgateway.filters.GateFilter\n" +
            "//import com.ctriposs.gatekeeper.context.CTRequestContext\n" +
            "\n" +
            "class filter extends GateFilter {\n" +
            "\n" +
            "    @Override\n" +
            "    String filterType() {\n" +
            "        return 'pre'\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    int filterOrder() {\n" +
            "        return 1\n" +
            "    }\n" +
            "\n" +
            "    boolean shouldFilter() {\n" +
            "        return true\n" +
            "    }\n" +
            "\n" +
            "    Object run() {\n" +
            "        return null\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "}";

    String sNotGateFilterGroovy = "import com.along101.pgateway.filters.GateFilter\n" +
            "//import com.ctriposs.gatekeeper.context.CTRequestContext\n" +
            "\n" +
            "class filter  {\n" +
            "\n" +
            "    @Override\n" +
            "    String filterType() {\n" +
            "        return 'pre'\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    int filterOrder() {\n" +
            "        return 1\n" +
            "    }\n" +
            "\n" +
            "    boolean shouldFilter() {\n" +
            "        return true\n" +
            "    }\n" +
            "\n" +
            "    Object run() {\n" +
            "        return null\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "}";

    String sCompileFailCode = "import com.along101.pgateway.filters.GateFilter\n" +
            "//import com.ctriposs.gatekeeper.context.CTRequestContext\n" +
            "\n" +
            "cclass filter extends GateFilter {\n" +
            "\n" +
            "    @Override\n" +
            "    String filterType() {\n" +
            "        return 'pre'\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    int filterOrder() {\n" +
            "        return 1\n" +
            "    }\n" +
            "\n" +
            "    boolean shouldFilter() {\n" +
            "        return true\n" +
            "    }\n" +
            "\n" +
            "    Object run() {\n" +
            "        return null\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "}";

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testCompile() {
		Class filterClass = FilterVerifier.getInstance().compileGroovy(sGoodGroovyScriptFilter);
		assertNotNull(filterClass);
		filterClass = FilterVerifier.getInstance().compileGroovy(sNotGateFilterGroovy);
		assertNotNull(filterClass);

		try {
			filterClass = FilterVerifier.getInstance().compileGroovy(sCompileFailCode);
			assertFalse(true); // we shouldn't get here
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testGateFilterInstance() {
		Class filterClass = FilterVerifier.getInstance().compileGroovy(sGoodGroovyScriptFilter);
		assertNotNull(filterClass);
		try {
			Object filter = FilterVerifier.getInstance().instanciateClass(filterClass);
			try {
				FilterVerifier.getInstance().checkGateFilterInstance(filter);
			} catch (InstantiationException e) {
				e.printStackTrace();
				assertFalse(true); // we shouldn't get here
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		}

		filterClass = FilterVerifier.getInstance().compileGroovy(sNotGateFilterGroovy);
		assertNotNull(filterClass);
		try {
			Object filter = FilterVerifier.getInstance().instanciateClass(filterClass);
			try {
				FilterVerifier.getInstance().checkGateFilterInstance(filter);
				assertFalse(true); // we shouldn't get here
			} catch (InstantiationException e) {
				e.printStackTrace();
				assertTrue(true); // this
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		}
	}

	@Test
	public void testVerify() {

		try {
			FilterInfo filterInfo = FilterVerifier.getInstance().verifyFilter(sGoodGroovyScriptFilter);
			assertNotNull(filterInfo);
			assertEquals(filterInfo.getFilterId(), "pgateway:filter:pre");
			assertEquals(filterInfo.getFilterType(), "pre");
			assertEquals(filterInfo.getFilterName(), "filter");
			assertFalse(filterInfo.isActive());
			assertFalse(filterInfo.isCanary());

		} catch (InstantiationException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		}

		try {
			FilterInfo filterInfo = FilterVerifier.getInstance().verifyFilter(sNotGateFilterGroovy);
			assertFalse(true);// shouldn't get here
		} catch (InstantiationException e) {
			e.printStackTrace();
			assertTrue(true); // we shouldn't get here
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		}

		try {
			FilterInfo filterInfo = FilterVerifier.getInstance().verifyFilter(sCompileFailCode);
			assertFalse(true);// shouldn't get here
		} catch (CompilationFailedException e) {
			assertTrue(true); // we shouldn't get here
		} catch (InstantiationException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			assertFalse(true); // we shouldn't get here
		}

	}
}
