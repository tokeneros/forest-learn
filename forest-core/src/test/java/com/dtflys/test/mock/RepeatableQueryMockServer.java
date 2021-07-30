package com.dtflys.test.mock;

import org.apache.http.HttpHeaders;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.Header;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RepeatableQueryMockServer extends MockServerRule {

    public final static String EXPECTED = "{\"status\": \"ok\"}";

    public final static Integer port = 5102;

    public RepeatableQueryMockServer(Object target) {
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
                        .withQueryStringParameter("username",  "bar")
                        .withQueryStringParameter("username",  "user1")
                        .withQueryStringParameter("username",  "user2")
                        .withQueryStringParameter("password",  "123456")

        )
        .respond(
                response()
                        .withStatusCode(200)
                        .withBody(EXPECTED)
        );

        mockClient.when(
                request()
                        .withPath("/hello/user/array")
                        .withMethod("GET")
                        .withHeader(new Header(HttpHeaders.ACCEPT, "text/plain"))
                        .withQueryStringParameter("username_0",  "foo")
                        .withQueryStringParameter("username_1",  "bar")
                        .withQueryStringParameter("username_2",  "user1")
                        .withQueryStringParameter("username_3",  "user2")
                        .withQueryStringParameter("password",  "123456")
        )
        .respond(
                response()
                        .withStatusCode(200)
                        .withBody(EXPECTED)
        );

    }

}
