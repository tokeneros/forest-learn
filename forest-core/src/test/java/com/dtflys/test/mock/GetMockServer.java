package com.dtflys.test.mock;

import org.apache.http.HttpHeaders;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.Header;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-05-17 16:08
 */
public class GetMockServer extends MockServerRule {

    public final static String EXPECTED = "{\"status\":\"ok\"}";

    public final static Integer port = 5002;

    public GetMockServer(Object target) {
        super(target, port);
    }

    public void initServer() {
        MockServerClient mockClient = new MockServerClient("localhost", port);
        mockClient.when(
                request()
                        .withPath("/hello/user")
                        .withMethod("GET")
                        .withHeader(new Header(HttpHeaders.ACCEPT, "text/plain"))
                        .withQueryStringParameter("username",  "foo")
        )
        .respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(
                                new Header("Content-Type", "application/vnd.kafka.v2+json"),
                                new Header("Vary", "Accept-Encoding, User-Agent")
                        )
                        .withBody(EXPECTED)
        );

    }

}
