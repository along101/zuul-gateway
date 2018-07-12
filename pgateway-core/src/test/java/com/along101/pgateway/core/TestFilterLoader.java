package com.along101.pgateway.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.along101.pgateway.common.IDynamicCodeCompiler;
import com.along101.pgateway.filters.FilterRegistry;
import com.along101.pgateway.filters.GateFilter;

public class TestFilterLoader {

	@Mock
	File file;

	@Mock
	IDynamicCodeCompiler compiler;

	@Mock
	FilterRegistry registry;

	FilterLoader loader;

	TestGateFilter filter = new TestGateFilter();

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		loader = spy(new FilterLoader());
		loader.setCompiler(compiler);
		loader.setFilterRegistry(registry);
	}

	@Test
	public void testGetFilterFromFile() throws Exception {
		doReturn(TestGateFilter.class).when(compiler).compile(file);
		assertTrue(loader.putFilter(file));
		verify(registry).put(any(String.class), any(GateFilter.class));
	}

	@Test
	public void testGetFiltersByType() throws Exception {
		doReturn(TestGateFilter.class).when(compiler).compile(file);
		assertTrue(loader.putFilter(file));

		verify(registry).put(any(String.class), any(GateFilter.class));

		final List<GateFilter> filters = new ArrayList<GateFilter>();
		filters.add(filter);
		when(registry.getAllFilters()).thenReturn(filters);

		List<GateFilter> list = loader.getFiltersByType("test");
		assertTrue(list != null);
		assertTrue(list.size() == 1);
		GateFilter filter = list.get(0);
		assertTrue(filter != null);
		assertTrue(filter.filterType().equals("test"));
	}

	@Test
	public void testGetFilterFromString() throws Exception {
		String string = "";
		doReturn(TestGateFilter.class).when(compiler).compile(string, string);
		GateFilter filter = loader.getFilter(string, string);

		assertNotNull(filter);
		assertTrue(filter.getClass() == TestGateFilter.class);
	}

	public static class TestGateFilter extends GateFilter {

		public TestGateFilter() {
			super();
		}

		@Override
		public String filterType() {
			return "test";
		}

		@Override
		public int filterOrder() {
			return 0;
		}

		public boolean shouldFilter() {
			return false;
		}

		public Object run() {
			return null;
		}
	}
}
