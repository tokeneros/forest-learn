package com.dtflys.forest.springboot;

import com.dtflys.forest.springboot.annotation.ForestScannerRegister;
import com.dtflys.forest.springboot.properties.ForestConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Forest入口类
 * @see Configuration 配置类都会使用，此处可不配置，不影响spring加载
 * @see ComponentScan 扫描配置文件，可读取用户自定义参数，完成部分基础配置
 * @see EnableConfigurationProperties 加载@ConfigurationProperties配置文件，此处和上面扫描完成同样的工作
 * @see Import
 *      1. 导入ImportBeanDefinitionRegistrar接口的registerBeanDefinitions方法
 *      2. 如果@Configuration，可以加载所有的其内所有bean
 *      3. 实例化该对象
 */
@Configuration
@ComponentScan("com.dtflys.forest.springboot.properties")
@EnableConfigurationProperties({ForestConfigurationProperties.class})
@Import({ForestScannerRegister.class})
public class ForestAutoConfiguration {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    /**
     * Forest Bean注册器
     * @param forestConfigurationProperties 自定义配置信息
     * @return
     */
    @Bean
    public ForestBeanRegister forestBeanRegister(ForestConfigurationProperties forestConfigurationProperties) {
        ForestBeanRegister forestBeanRegister = new ForestBeanRegister(applicationContext, forestConfigurationProperties);
        // 注册配置
        forestBeanRegister.registerForestConfiguration(forestConfigurationProperties);
        // 注册
        forestBeanRegister.registerScanner(forestConfigurationProperties);
        return forestBeanRegister;
    }

}
