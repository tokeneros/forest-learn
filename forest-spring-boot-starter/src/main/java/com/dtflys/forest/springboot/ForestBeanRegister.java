package com.dtflys.forest.springboot;

import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.converter.ForestConverter;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.interceptor.SpringInterceptorFactory;
import com.dtflys.forest.listener.ConverterBeanListener;
import com.dtflys.forest.logging.ForestLogHandler;
import com.dtflys.forest.scanner.ClassPathClientScanner;
import com.dtflys.forest.schema.ForestConfigurationBeanDefinitionParser;
import com.dtflys.forest.springboot.annotation.ForestScannerRegister;
import com.dtflys.forest.springboot.properties.ForestConverterItemProperties;
import com.dtflys.forest.springboot.properties.ForestSSLKeyStoreProperties;
import com.dtflys.forest.utils.ForestDataType;
import com.dtflys.forest.utils.StringUtils;
import com.dtflys.forest.springboot.properties.ForestConfigurationProperties;
import com.dtflys.forest.springboot.properties.ForestConvertProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Forest Bean 注册器
 * 1. 他们不会使用@Bean注解来申明对象，都是通过注册BeanDefinition来申明对象
 * 2. 他们会存在唯一的一个上下文，通过属性注册来将其他相关参数存放进去
 */
public class ForestBeanRegister implements ResourceLoaderAware, BeanPostProcessor {

    private final ConfigurableApplicationContext applicationContext;

    private ResourceLoader resourceLoader;

    private ForestConfigurationProperties forestConfigurationProperties;


    public ForestBeanRegister(ConfigurableApplicationContext applicationContext, ForestConfigurationProperties forestConfigurationProperties) {
        this.applicationContext = applicationContext;
        this.forestConfigurationProperties = forestConfigurationProperties;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     *
     * @param forestConfigurationProperties
     * @return
     */
    public ForestConfiguration registerForestConfiguration(ForestConfigurationProperties forestConfigurationProperties) {
        // 生成BeanClass 为 ForestConfiguration 的 BeanDefinitionBuilder构建对象
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ForestConfiguration.class);
        // 自定义beanId，默认为 forestConfiguration
        String id = forestConfigurationProperties.getBeanId();
        if (StringUtils.isBlank(id)) {
            id = "forestConfiguration";
        }

        // 实例化 - 日志处理器接口
        Class<? extends ForestLogHandler> logHandlerClass = forestConfigurationProperties.getLogHandler();
        ForestLogHandler logHandler = null;
        if (logHandlerClass != null) {
            try {
                logHandler = logHandlerClass.newInstance();
            } catch (InstantiationException e) {
                throw new ForestRuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new ForestRuntimeException(e);
            }
        }

        // 向 BeanDefinitionBuilder构建对象 添加属性值
        beanDefinitionBuilder
                .addPropertyValue("maxConnections", forestConfigurationProperties.getMaxConnections())
                .addPropertyValue("maxRouteConnections", forestConfigurationProperties.getMaxRouteConnections())
                .addPropertyValue("timeout", forestConfigurationProperties.getTimeout())
                .addPropertyValue("connectTimeout", forestConfigurationProperties.getConnectTimeout())
                .addPropertyValue("charset", forestConfigurationProperties.getCharset())
                .addPropertyValue("retryer", forestConfigurationProperties.getRetryer())
                .addPropertyValue("retryCount", forestConfigurationProperties.getRetryCount())
                .addPropertyValue("maxRetryInterval", forestConfigurationProperties.getMaxRetryInterval())
                .addPropertyValue("logEnabled", forestConfigurationProperties.isLogEnabled())
                .addPropertyValue("logRequest", forestConfigurationProperties.isLogRequest())
                .addPropertyValue("logResponseStatus", forestConfigurationProperties.isLogResponseStatus())
                .addPropertyValue("logResponseContent", forestConfigurationProperties.isLogResponseContent())
                .addPropertyValue("logHandler", logHandler)
                .addPropertyValue("backendName", forestConfigurationProperties.getBackend())
                .addPropertyValue("interceptors", forestConfigurationProperties.getInterceptors())
                .addPropertyValue("sslProtocol", forestConfigurationProperties.getSslProtocol())
                .addPropertyValue("variables", forestConfigurationProperties.getVariables())
                .setLazyInit(false)
                .setFactoryMethod("configuration");

        // 注册拦截器对象
        BeanDefinition interceptorFactoryBeanDefinition = registerInterceptorFactoryBean();
        // 将拦截器对象注入到ForestConfiguration中
        beanDefinitionBuilder.addPropertyValue("interceptorFactory", interceptorFactoryBeanDefinition);

        // 获取配置中的密钥信息
        List<ForestSSLKeyStoreProperties> sslKeyStorePropertiesList = forestConfigurationProperties.getSslKeyStores();
        // Spring中能包括运行时 bean 引用（要解析为 bean 对象) 而存在的map对象, 这里VALUE为BeanDefinition
        ManagedMap<String, BeanDefinition> sslKeystoreMap = new ManagedMap<>();
        for (ForestSSLKeyStoreProperties keyStoreProperties : sslKeyStorePropertiesList) {
            // 注册密钥信息
            registerSSLKeyStoreBean(sslKeystoreMap, keyStoreProperties);
        }

        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        beanDefinition.getPropertyValues().addPropertyValue("sslKeyStores", sslKeystoreMap);

        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(id, beanDefinition);

        ForestConfiguration configuration = applicationContext.getBean(id, ForestConfiguration.class);

        Map<String, Class> filters = forestConfigurationProperties.getFilters();
        for (Map.Entry<String, Class> entry : filters.entrySet()) {
            String filterName = entry.getKey();
            Class filterClass = entry.getValue();
            configuration.registerFilter(filterName, filterClass);
        }

        ForestConvertProperties convertProperties = forestConfigurationProperties.getConverters();
        if (convertProperties != null) {
            registerConverter(configuration, ForestDataType.TEXT, convertProperties.getText());
            registerConverter(configuration, ForestDataType.JSON, convertProperties.getJson());
            registerConverter(configuration, ForestDataType.XML, convertProperties.getXml());
            registerConverter(configuration, ForestDataType.BINARY, convertProperties.getBinary());
        }

        registerConverterBeanListener(configuration);
        return configuration;
    }

    public ConverterBeanListener registerConverterBeanListener(ForestConfiguration forestConfiguration) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ConverterBeanListener.class);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        beanDefinition.getPropertyValues().addPropertyValue("forestConfiguration", forestConfiguration);
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition("forestConverterBeanListener", beanDefinition);
        return applicationContext.getBean("forestConverterBeanListener", ConverterBeanListener.class);
    }

    private void registerConverter(ForestConfiguration configuration, ForestDataType dataType, ForestConverterItemProperties converterItemProperties) {
        if (converterItemProperties == null) {
            return;
        }
        Class type = converterItemProperties.getType();
        if (type != null) {
            ForestConverter converter = null;
            try {
                converter = (ForestConverter) type.newInstance();

                Map<String, Object> parameters = converterItemProperties.getParameters();
                PropertyDescriptor[] descriptors = ReflectUtils.getBeanSetters(type);
                for (PropertyDescriptor descriptor : descriptors) {
                    String name = descriptor.getName();
                    Object value = parameters.get(name);
                    Method method = descriptor.getWriteMethod();
                    if (method != null) {
                        try {
                            method.invoke(converter, value);
                        } catch (IllegalAccessException e) {
                            throw new ForestRuntimeException("An error occurred during setting the property " + type.getName() + "." + name, e);
                        } catch (InvocationTargetException e) {
                            throw new ForestRuntimeException("An error occurred during setting the property " + type.getName() + "." + name, e);
                        }
                    }
                }
                configuration.getConverterMap().put(dataType, converter);
            } catch (InstantiationException e) {
                throw new ForestRuntimeException("[Forest] Convert type '" + type.getName() + "' cannot be initialized!", e);
            } catch (IllegalAccessException e) {
                throw new ForestRuntimeException("[Forest] Convert type '" + type.getName() + "' cannot be initialized!", e);
            }
        }
    }

    /**
     * 注册 拦截器对象
     * @return
     */
    public BeanDefinition registerInterceptorFactoryBean() {
        // 生成BeanClass 为 SpringInterceptorFactory 的 BeanDefinitionBuilder构建对象
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(SpringInterceptorFactory.class);
        // 获取BeanDefinition
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        // TODO 获取spring上下文的 BeanDefinitionRegistry 可注册我们想申明的Bean
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition("forestInterceptorFactory", beanDefinition);
        return beanDefinition;
    }

    /**
     * 注册密钥信息
     * @param map
     * @param sslKeyStoreProperties
     * @return
     */
    public BeanDefinition registerSSLKeyStoreBean(ManagedMap<String, BeanDefinition> map, ForestSSLKeyStoreProperties sslKeyStoreProperties) {
        // 密钥信息主键
        String id = sslKeyStoreProperties.getId();
        // 密钥信息不允许为空，不允许重复
        if (StringUtils.isBlank(id)) {
            throw new ForestRuntimeException("[Forest] Property 'id' of SSL keystore can not be empty or blank");
        }
        if (map.containsKey(id)) {
            throw new ForestRuntimeException("[Forest] Duplicate SSL keystore id '" + id + "'");
        }

        //
        BeanDefinition beanDefinition = ForestConfigurationBeanDefinitionParser.createSSLKeyStoreBean(
                id,
                sslKeyStoreProperties.getType(),
                sslKeyStoreProperties.getFile(),
                sslKeyStoreProperties.getKeystorePass(),
                sslKeyStoreProperties.getCertPass(),
                sslKeyStoreProperties.getProtocols(),
                sslKeyStoreProperties.getCipherSuites(),
                sslKeyStoreProperties.getSslSocketFactoryBuilder()
        );
        map.put(id, beanDefinition);
        return beanDefinition;
    }

    public ClassPathClientScanner registerScanner(ForestConfigurationProperties forestConfigurationProperties) {
        List<String> basePackages = ForestScannerRegister.basePackages;
        String configurationId = ForestScannerRegister.configurationId;

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext.getBeanFactory();

        ClassPathClientScanner scanner = new ClassPathClientScanner(configurationId, registry);
        // this check is needed in Spring 3.1
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }
//        scanner.registerFilters();
        if (basePackages == null || basePackages.size() == 0) {
            return scanner;
        }
        scanner.doScan(org.springframework.util.StringUtils.toStringArray(basePackages));
        return scanner;
    }


    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        registerForestConfiguration(forestConfigurationProperties);
        registerScanner(forestConfigurationProperties);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
