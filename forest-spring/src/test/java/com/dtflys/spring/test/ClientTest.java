package com.dtflys.spring.test;

import junit.framework.TestCase;
import com.dtflys.spring.test.client0.BeastshopClient;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-04-24 19:39
 */
public class ClientTest extends TestCase {

    private ClassPathXmlApplicationContext applicationContext;

    public void testScan() {
        applicationContext = new ClassPathXmlApplicationContext(
                new String[] { "classpath:client-test.xml" });
        BeastshopClient beastshopClient =
                (BeastshopClient) applicationContext.getBean("beastshopClient");
        assertNotNull(beastshopClient);
        String result = beastshopClient.index();
        assertNotNull(result);
    }

}
