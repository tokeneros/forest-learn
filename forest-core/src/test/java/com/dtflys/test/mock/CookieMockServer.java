package com.dtflys.test.mock;

import org.apache.http.HttpHeaders;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-05-17 16:08
 */
public class CookieMockServer extends MockServerRule {

    public final static String EXPECTED = "{\"status\": \"ok\"}";

    public final static Integer port = 5053;

    public CookieMockServer(Object target) {
        super(target, port);
    }

    public void initServer() {
        MockServerClient mockClient = new MockServerClient("localhost", port);
        mockClient.when(
                request()
                        .withPath("/login")
                        .withMethod("POST")
                        .withHeader(new Header(HttpHeaders.ACCEPT, "text/plain"))
                        .withQueryStringParameter("username",  "foo")
        )
        .respond(
                response()
                        .withStatusCode(200)
                        .withBody(EXPECTED)
                        .withCookie(new Cookie("cookie_foo", "cookie_bar"))
        );


        mockClient.when(
                request()
                        .withPath("/test")
                        .withMethod("POST")
                        .withHeader(new Header(HttpHeaders.ACCEPT, "text/plain"))
                        .withCookie(new Cookie("cookie_foo", "cookie_bar"))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(EXPECTED)
                );

    }

}
