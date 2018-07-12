package com.along101.pgateway.servlet;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.along101.pgateway.common.CatContext;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.monitor.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.Cat.Context;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.along101.pgateway.common.CatContext;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.core.GateCallable;
import com.along101.pgateway.core.GateRunner;
import com.along101.pgateway.monitor.MetricReporter;

public class AsyncGateServlet extends HttpServlet {
	private static Logger LOGGER = LoggerFactory.getLogger(AsyncGateServlet.class);
	
	private DynamicIntProperty asyncTimeout = DynamicPropertyFactory.getInstance().getIntProperty(Constants.GateServletAsyncTimeOut, 20000);
	private DynamicIntProperty coreSize = DynamicPropertyFactory.getInstance().getIntProperty(Constants.GateThreadPoolCodeSize, 200);
	private DynamicIntProperty maximumSize = DynamicPropertyFactory.getInstance().getIntProperty(Constants.GateThreadPoolMaxSize, 2000);
	private DynamicLongProperty aliveTime = DynamicPropertyFactory.getInstance().getLongProperty(Constants.GateThreadPoolAliveTime, 1000 * 60 * 5);
	
	private GateRunner gateRunner = new GateRunner();
	private AtomicReference<ThreadPoolExecutor> poolExecutorRef = new AtomicReference<ThreadPoolExecutor>();	    
	private AtomicLong rejectedRequests = new AtomicLong(0);
    @Override
    public void init() throws ServletException {
    	reNewThreadPool();
        Runnable c = new Runnable() {
            @Override
            public void run() {
                ThreadPoolExecutor p = poolExecutorRef.get();
                p.setCorePoolSize(coreSize.get());
                p.setMaximumPoolSize(maximumSize.get());
                p.setKeepAliveTime(aliveTime.get(),TimeUnit.MILLISECONDS);
            }
        };
        
        coreSize.addCallback(c);
        maximumSize.addCallback(c);
        aliveTime.addCallback(c);
        MetricReporter.getInstance().setThreadPoolExecutor(poolExecutorRef, rejectedRequests);
    }
    
    private void reNewThreadPool() {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(coreSize.get(), maximumSize.get(), aliveTime.get(), TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
        ThreadPoolExecutor old = poolExecutorRef.getAndSet(poolExecutor);
        if (old != null) {
            shutdownPoolExecutor(old);
        }
    }
    
    private void shutdownPoolExecutor(ThreadPoolExecutor old) {
        try {
            old.awaitTermination(5, TimeUnit.MINUTES);
            old.shutdown();
        } catch (InterruptedException e) {
            old.shutdownNow();
            LOGGER.error("Shutdown Gate Thread Pool:", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	Transaction tran = Cat.getProducer().newTransaction("AsyncGateServlet", req.getRequestURL().toString());
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(asyncTimeout.get());
        asyncContext.addListener(new AsyncGateListener());
        try {
        	Context ctx = new CatContext();
        	Cat.logRemoteCallClient(ctx);
            poolExecutorRef.get().submit(new GateCallable(ctx,asyncContext, gateRunner,req));            
            tran.setStatus(Transaction.SUCCESS);
        } catch (RuntimeException e) {
            Cat.logError(e);
            tran.setStatus(e);
            rejectedRequests.incrementAndGet();
            throw e;
        }finally{
        	tran.complete();
        }
    }
    

    @Override
    public void destroy() {
        shutdownPoolExecutor(poolExecutorRef.get());
        MetricReporter.getInstance().shutdown();
    }
}
