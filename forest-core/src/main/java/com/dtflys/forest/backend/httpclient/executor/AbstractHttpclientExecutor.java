package com.dtflys.forest.backend.httpclient.executor;

import com.dtflys.forest.backend.AbstractHttpExecutor;
import com.dtflys.forest.backend.BodyBuilder;
import com.dtflys.forest.backend.httpclient.HttpclientRequestProvider;
import com.dtflys.forest.backend.httpclient.body.HttpclientBodyBuilder;
import com.dtflys.forest.backend.url.URLBuilder;
import com.dtflys.forest.http.ForestCookie;
import com.dtflys.forest.http.ForestCookies;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.http.ForestResponseFactory;
import com.dtflys.forest.logging.LogConfiguration;
import com.dtflys.forest.logging.ForestLogHandler;
import com.dtflys.forest.logging.ResponseLogMessage;
import com.dtflys.forest.utils.RequestNameValue;
import com.dtflys.forest.utils.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpRequestBase;
import com.dtflys.forest.converter.json.ForestJsonConverter;
import com.dtflys.forest.backend.httpclient.request.HttpclientRequestSender;
import com.dtflys.forest.backend.httpclient.response.HttpclientForestResponseFactory;
import com.dtflys.forest.backend.httpclient.response.HttpclientResponseHandler;
import com.dtflys.forest.handler.LifeCycleHandler;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.mapping.MappingTemplate;


import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author gongjun
 * @since 2016-06-14
 */
public abstract class AbstractHttpclientExecutor<T extends  HttpRequestBase> extends AbstractHttpExecutor {
    private static Logger log = LoggerFactory.getLogger(AbstractHttpclientExecutor.class);

    protected final HttpclientResponseHandler httpclientResponseHandler;
    protected String url;
    protected final String typeName;
    protected T httpRequest;
    protected BodyBuilder<T> bodyBuilder;
    protected CookieStore cookieStore;


    protected T buildRequest() {
        url = buildUrl();
        return getRequestProvider().getRequest(url);
    }

    protected abstract HttpclientRequestProvider<T> getRequestProvider();

    protected abstract URLBuilder getURLBuilder();


    protected String buildUrl() {
        return getURLBuilder().buildUrl(request);
    }


    protected void prepareBodyBuilder() {
        bodyBuilder = new HttpclientBodyBuilder();
    }

    protected void prepare(LifeCycleHandler lifeCycleHandler) {
        httpRequest = buildRequest();
        prepareBodyBuilder();
        prepareCookies(lifeCycleHandler);
        prepareHeaders();
        prepareBody(lifeCycleHandler);
    }

    public AbstractHttpclientExecutor(ForestRequest request, HttpclientResponseHandler httpclientResponseHandler, HttpclientRequestSender requestSender) {
        super(request, requestSender);
        this.typeName = request.getType().getName();
        this.httpclientResponseHandler = httpclientResponseHandler;
    }

    public void prepareHeaders() {
        ForestJsonConverter jsonConverter = request.getConfiguration().getJsonConverter();
        List<RequestNameValue> headerList = request.getHeaderNameValueList();
        String contentType = request.getContentType();
        String contentEncoding = request.getContentEncoding();
        if (headerList != null && !headerList.isEmpty()) {
            for (RequestNameValue nameValue : headerList) {
                String name = nameValue.getName();
                if (!"Content-Type".equalsIgnoreCase(name)
                        && !"Content-Encoding".equalsIgnoreCase(name)) {
                    httpRequest.setHeader(name, MappingTemplate.getParameterValue(jsonConverter, nameValue.getValue()));
                }
            }
        }
        if (StringUtils.isNotEmpty(contentType)) {
            httpRequest.setHeader("Content-Type", contentType);
        }
        if (StringUtils.isNotEmpty(contentEncoding)) {
            httpRequest.setHeader("Content-Encoding", contentEncoding);
        }

    }


    public void prepareCookies(LifeCycleHandler lifeCycleHandler) {
        cookieStore = new BasicCookieStore();
        ForestCookies cookies = new ForestCookies();
        lifeCycleHandler.handleLoadCookie(request, cookies);
        for (ForestCookie cookie : cookies) {
            BasicClientCookie httpCookie = new BasicClientCookie(
                    cookie.getName(),
                    cookie.getValue()
            );
            httpCookie.setDomain(cookie.getDomain());
            httpCookie.setPath(cookie.getPath());
            httpCookie.setSecure(cookie.isSecure());
            httpCookie.setExpiryDate(new Date(cookie.getExpiresTime()));
            cookieStore.addCookie(httpCookie);
        }
    }

    public void prepareBody(LifeCycleHandler lifeCycleHandler) {
        bodyBuilder.buildBody(httpRequest, request, lifeCycleHandler);
    }



    public void logResponse(ForestResponse response) {
        LogConfiguration logConfiguration = request.getLogConfiguration();
        if (!logConfiguration.isLogEnabled() || response.isLogged()) {
            return;
        }
        response.setLogged(true);
        ResponseLogMessage logMessage = new ResponseLogMessage(response, response.getStatusCode());
        ForestLogHandler logHandler = logConfiguration.getLogHandler();
        if (logHandler != null) {
            if (logConfiguration.isLogResponseStatus()) {
                logHandler.logResponseStatus(logMessage);
            }
            if (logConfiguration.isLogResponseContent() && logConfiguration.isLogResponseContent()) {
                logHandler.logResponseContent(logMessage);
            }
        }
    }


    @Override
    public void execute(LifeCycleHandler lifeCycleHandler) {
        prepare(lifeCycleHandler);
        execute(0, lifeCycleHandler);
    }


    public void execute(int retryCount, LifeCycleHandler lifeCycleHandler) {
        Date startDate = new Date();
        ForestResponseFactory forestResponseFactory = new HttpclientForestResponseFactory();
        try {
            requestSender.sendRequest(
                    request,
                    httpclientResponseHandler,
                    httpRequest,
                    lifeCycleHandler,
                    cookieStore,
                    startDate, 0);
        } catch (IOException e) {
            if (retryCount >= request.getRetryCount()) {
                httpRequest.abort();
                response = forestResponseFactory.createResponse(request, null, lifeCycleHandler, e, startDate);
                lifeCycleHandler.handleSyncWithException(request, response, e);
                return;
            }
            log.error(e.getMessage());
        } catch (ForestRuntimeException e) {
            httpRequest.abort();
            throw e;
        }
    }

    @Override
    public void close() {
/*
        if (httpResponse != null) {
            try {
                EntityUtils.consume(httpResponse.getEntity());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        client0.getConnectionManager().closeExpiredConnections();
*/
    }


}
