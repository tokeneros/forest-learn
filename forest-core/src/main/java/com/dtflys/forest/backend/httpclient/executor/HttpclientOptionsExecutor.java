package com.dtflys.forest.backend.httpclient.executor;

import com.dtflys.forest.backend.httpclient.HttpclientRequestProvider;
import com.dtflys.forest.backend.httpclient.entity.HttpOptionsWithBodyEntity;
import com.dtflys.forest.backend.httpclient.request.HttpclientRequestSender;
import com.dtflys.forest.backend.httpclient.response.HttpclientResponseHandler;
import com.dtflys.forest.backend.url.URLBuilder;
import com.dtflys.forest.http.ForestRequest;
import org.apache.http.client.methods.HttpOptions;

/**
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2017-04-20 14:44
 */
public class HttpclientOptionsExecutor extends AbstractHttpclientExecutor<HttpOptionsWithBodyEntity> {

    @Override
    protected HttpclientRequestProvider<HttpOptionsWithBodyEntity> getRequestProvider() {
        return url -> new HttpOptionsWithBodyEntity(url);
    }

    @Override
    protected URLBuilder getURLBuilder() {
        return URLBuilder.getQueryableURLBuilder();
    }


    public HttpclientOptionsExecutor(ForestRequest request, HttpclientResponseHandler httpclientResponseHandler, HttpclientRequestSender requestSender) {
        super(request, httpclientResponseHandler, requestSender);
    }

}
