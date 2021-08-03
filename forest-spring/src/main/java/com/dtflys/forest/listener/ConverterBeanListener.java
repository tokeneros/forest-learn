package com.dtflys.forest.listener;

import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.converter.ForestConverter;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import java.util.Map;

/**
 * forest数据类型转换器自动注入
 *
 * @author caihongming
 * @version v1.0
 * @since 2021-03-30
 **/
public class ConverterBeanListener implements ApplicationListener<ApplicationContextEvent> {

    /**
     * 自定义上下文
     */
    private ForestConfiguration forestConfiguration;

    /**
     * spring 事件响应
     * TODO 为什么会存在该步骤？？？
     * @param event 事件
     */
    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        // 获取spring上下文
        ApplicationContext applicationContext = event.getApplicationContext();
        // 获取自定义上下文，如果不存在，则获取对应信息
        ForestConfiguration forestConfiguration = this.forestConfiguration;
        if (forestConfiguration == null) {
            try {
                forestConfiguration = applicationContext.getBean(ForestConfiguration.class);
            } catch (Exception ignored) {
                throw new ForestRuntimeException("property forestConfiguration undefined", ignored);
            }
        }
        // 通过 class 类型获取 转换器实例
        Map<String, ForestConverter> forestConverterMap = applicationContext.getBeansOfType(ForestConverter.class);
        for (ForestConverter forestConverter : forestConverterMap.values()) {
            // 将转换器注册到自定义上下文中
            forestConfiguration.getConverterMap().put(forestConverter.getDataType(), forestConverter);
        }
    }

    public ForestConfiguration getForestConfiguration() {
        return forestConfiguration;
    }

    public void setForestConfiguration(ForestConfiguration forestConfiguration) {
        this.forestConfiguration = forestConfiguration;
    }
}
