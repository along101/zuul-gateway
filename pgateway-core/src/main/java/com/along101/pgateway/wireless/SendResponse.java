package com.along101.pgateway.wireless;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.util.Pair;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateHeaders;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

public class SendResponse extends GateFilter {
	private static final DynamicIntProperty BUFFER_SIZE = DynamicPropertyFactory.getInstance().getIntProperty(Constants.GateInitialStreamBufferSize, 1024);
    private static final DynamicBooleanProperty CONTENT_LENGTH = DynamicPropertyFactory.getInstance().getBooleanProperty(Constants.GateSetContentLength, false);

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 100;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext context = RequestContext.getCurrentContext();
		return context.getResponseBody() != null || context.getResponseDataStream() != null;
	}

	@Override
	public Object run() {
		Transaction tran = Cat.getProducer().newTransaction("Filter", "SendResponse");
		RequestContext context = RequestContext.getCurrentContext();

		HttpServletResponse servletResponse = context.getResponse();
		String responseBody = context.getResponseBody();
		InputStream responseDataStream = context.getResponseDataStream();

		InputStream inStream = null;
		OutputStream outStream = null;
		try {
			tran.setStatus(Transaction.SUCCESS);
			addResponseHeaders();
			outStream = servletResponse.getOutputStream();
			if (responseBody != null) {
				inStream = new ByteArrayInputStream(responseBody.getBytes());
				writeResponse(inStream, outStream);
				return null;
			} else {
				inStream = responseDataStream;
			}
            if (!context.getResponseGZipped()) {
                writeResponse(inStream, outStream);
                return null;
            }            
            boolean isGZipRequested = false;
            final String requestEncoding = context.getRequest().getHeader(GateHeaders.ACCEPT_ENCODING);
            if (requestEncoding != null && requestEncoding.contains("gzip")) {
                isGZipRequested = true;
            }
            // if origin response is gzipped, and client has not requested gzip, decompress stream
            // before sending to client
            // else, stream gzip directly to client
            if (isGZipRequested) {
                servletResponse.setHeader(GateHeaders.CONTENT_ENCODING, "gzip");
            } else {
                try {
                	inStream = new GZIPInputStream(responseDataStream);
                } catch (java.util.zip.ZipException e) {
                    Cat.logError("gzip expected but not received assuming unencoded response" + RequestContext.getCurrentContext().getRequest().getRequestURL().toString(),e);
                }
            }
			writeResponse(inStream, outStream);
			
		} catch (Throwable t) {
			tran.setStatus(t);
			Cat.logError(t);
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
				if (outStream != null) {
					outStream.flush();
					outStream.close();
				}
			} catch (IOException e) {
			}
			tran.complete();
		}
		return null;
	}

	private void addResponseHeaders() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletResponse servletResponse = ctx.getResponse();

		for (Pair<String, String> p : ctx.getGateResponseHeaders()) {
			servletResponse.addHeader(p.first(), p.second());
		}

		Integer contentLength = ctx.getOriginContentLength();

		// only inserts Content-Length if origin provides it and origin response
		// is not gzipped
		if (CONTENT_LENGTH.get()) {
			if (contentLength != null && !ctx.getResponseGZipped())
			servletResponse.setContentLength(contentLength);
		}

	}

	private void writeResponse(InputStream zin, OutputStream out) throws IOException {
		long start = System.currentTimeMillis();

		long readCost = 0; // store the cost for reading data from server
		long writeCost = 0; // store the cost for sending data to client

		long begin = 0;
		long end = 0;
		try {
			byte[] bytes = new byte[BUFFER_SIZE.get()];
			int bytesRead = -1;

			begin = System.currentTimeMillis();
			while ((bytesRead = zin.read(bytes)) != -1) {
				end = System.currentTimeMillis();
				readCost += (end - begin);

				begin = end;
				try {
					out.write(bytes, 0, bytesRead);
					out.flush();
				} catch (IOException e) {
					Cat.logError(e);
				} finally {
					end = System.currentTimeMillis();
					writeCost += (end - begin);
				}

				// doubles buffer size if previous read filled it
				if (bytesRead == bytes.length) {
					bytes = new byte[bytes.length * 2];
				}

				begin = end;
			}
		} catch (Exception e) {
			// String tString = e.toString();
		} finally {
			RequestContext.getCurrentContext().set("sendResponseCost", System.currentTimeMillis() - start);
			RequestContext.getCurrentContext().set("sendResponseCost:read", readCost);
			RequestContext.getCurrentContext().set("sendResponseCost:write", writeCost);
		}
	}
}