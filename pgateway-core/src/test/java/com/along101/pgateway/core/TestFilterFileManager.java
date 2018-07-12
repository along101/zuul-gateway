package com.along101.pgateway.core;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class TestFilterFileManager {
	 @Mock
     private File nonGroovyFile;
     @Mock
     private File groovyFile;

     @Mock
     private File directory;

     @Before
     public void before() {
         MockitoAnnotations.initMocks(this);
     }


     @Test
     public void testFileManagerInit() throws Exception, InstantiationException, IllegalAccessException {
         FilterFileManager manager = new FilterFileManager();

         manager = spy(manager);
         manager.instance = manager;
         doNothing().when(manager.instance).manageFiles();
         manager.init(1, "test", "test1");
         verify(manager, atLeast(1)).manageFiles();
         verify(manager, times(1)).startPoller();
         assertNotNull(manager.poller);

     }

}
