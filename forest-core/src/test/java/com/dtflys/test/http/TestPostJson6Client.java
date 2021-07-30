package com.dtflys.test.http;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dtflys.forest.backend.HttpBackend;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.converter.json.ForestFastjsonConverter;
import com.dtflys.test.http.client.PostClient;
import com.dtflys.test.mock.PostJson6MockServer;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-05-11 16:55
 */
public class TestPostJson6Client extends BaseClientTest {

    private final static Logger log = LoggerFactory.getLogger(TestPostJson6Client.class);

    @Rule
    public PostJson6MockServer server = new PostJson6MockServer(this);

    private static ForestConfiguration configuration;

    private static PostClient postClient;


    @BeforeClass
    public static void prepareClient() {
        configuration = ForestConfiguration.configuration();
        configuration.setVariableValue("port", PostJson6MockServer.port);
        ForestFastjsonConverter fastjsonConverter = new ForestFastjsonConverter();
        fastjsonConverter.setSerializerFeature(SerializerFeature.SortField);
        configuration.setJsonConverter(fastjsonConverter);
    }

    public TestPostJson6Client(HttpBackend backend) {
        super(backend, configuration);
        postClient = configuration.createInstance(PostClient.class);
    }

    @Before
    public void prepareMockServer() {
        server.initServer();
    }


    @Test
    public void testJsonPostObjWithList1() {
        Map<String, Object> obj = new LinkedHashMap<>();
        List<String> data = Lists.newArrayList("A", "B", "C");
        obj.put("name", "test");
        obj.put("data", data);
        String result = postClient.postJsonObjWithList1(obj);
        log.info("response: " + result);
        assertNotNull(result);
        Assert.assertEquals(PostJson6MockServer.EXPECTED, result);
    }

    @Test
    public void testJsonPostObjWithList2() {
        List<String> data = Lists.newArrayList("A", "B", "C");
        String result = postClient.postJsonObjWithList2("test", data);
        log.info("response: " + result);
        assertNotNull(result);
        Assert.assertEquals(PostJson6MockServer.EXPECTED, result);
    }

    @Test
    public void testJsonPostObjWithList2AndDefaultBody() {
        String result = postClient.postJsonObjWithList2AndDefaultBody(null, null);
        log.info("response: " + result);
        assertNotNull(result);
        Assert.assertEquals(PostJson6MockServer.EXPECTED, result);
    }


/*
    @Test
    public void testJsonPostJsonDate() throws ParseException {
//        configuration.getJsonConverter().setDateFormat("yyyy/MM/dd hh:mm:ss");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = format.parse("2020-10-11 10:12:00");
        JsonTestDate jsonTestDate = new JsonTestDate();
        jsonTestDate.setCreateTime(date);
        String result = postClient.postJsonDate(jsonTestDate);
        log.info("response: " + result);
        assertNotNull(result);
        Assert.assertEquals(PostJson6MockServer.EXPECTED, result);
    }
*/

}
