package com.dtflys.forest.beans;

import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.utils.ClientFactoryBeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 自定义适配的FactoryBean，在初始已经识别到我们所有后面使用的BeanDefinition，并且都配置为ClientFactoryBean类型
 *
 * @author gongjun[jun.gong@thebeastshop.com]
 * @see ClientFactoryBeanUtils#setupClientFactoryBean
 * @since 2017-04-24 18:47
 */
public class ClientFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    // 通过BeanDefinition 注册进来的
    private ForestConfiguration forestConfiguration;
    // 通过BeanDefinition 注册进来的
    private Class<T> interfaceClass;

    public ForestConfiguration getForestConfiguration() {
        return forestConfiguration;
    }

    public void setForestConfiguration(ForestConfiguration forestConfiguration) {
        this.forestConfiguration = forestConfiguration;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * spring 初始化Bean会调用该方法去完成实例化
     *
     * @return
     * @throws Exception
     */
    @Override
    public T getObject() throws Exception {
        // 判断自定义上下文是否存在，如果不存在想办法获取
        if (forestConfiguration == null) {
            // 通过 同步代码块 来保证线程安全
            synchronized (this) {
                // 如果为空
                if (forestConfiguration == null) {
                    try {
                        // 查看spring上下文中是否包含该对象
                        forestConfiguration = applicationContext.getBean(ForestConfiguration.class);
                    } catch (Throwable th) {
                    }
                    // 如果不包含，采用默认的上下文
                    if (forestConfiguration == null) {
                        forestConfiguration = ForestConfiguration.getDefaultConfiguration();
                    }
                }
            }
        }
        // 调用生成代理对象
        return forestConfiguration.createInstance(interfaceClass);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * spring提供的自动注入spring上下文
     *
     * @param context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
}
