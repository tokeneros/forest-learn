package com.dtflys.forest.scanner;

import com.dtflys.forest.annotation.BaseLifeCycle;
import com.dtflys.forest.annotation.MethodLifeCycle;
import com.dtflys.forest.file.SpringResource;
import com.dtflys.forest.http.body.MultipartRequestBodyBuilder;
import com.dtflys.forest.http.body.RequestBodyBuilder;
import com.dtflys.forest.http.body.ResourceRequestBodyBuilder;
import com.dtflys.forest.multipart.ForestMultipartFactory;
import com.dtflys.forest.utils.ClientFactoryBeanUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 自定义BeanDefinition定义扫描器
 *
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-04-24 14:46
 */
public class ClassPathClientScanner extends ClassPathBeanDefinitionScanner {

    private final String configurationId;

    private boolean allInterfaces = true;

    public ClassPathClientScanner(String configurationId, BeanDefinitionRegistry registry) {
        super(registry, false);
        this.configurationId = configurationId;
        // 注册过滤器
        registerFilters();
        // 注册多种类型
        registerMultipartTypes();
    }

    /**
     * TODO 这一块暂时没看懂
     * 注册能上传下载的文件类型
     */
    public void registerMultipartTypes() {
        // 自定义类型工厂 TODO
        ForestMultipartFactory.registerFactory(Resource.class, SpringResource.class);
        // 响应体 TODO
        RequestBodyBuilder.registerBodyBuilder(Resource.class, new ResourceRequestBodyBuilder());
        try {
            Class multipartFileClass = Class.forName("org.springframework.web.multipart.MultipartFile");
            Class springMultipartFileClass = Class.forName("com.dtflys.forest.file.SpringMultipartFile");
            ForestMultipartFactory.registerFactory(multipartFileClass, springMultipartFileClass);
            RequestBodyBuilder.registerBodyBuilder(multipartFileClass, new MultipartRequestBodyBuilder());
        } catch (ClassNotFoundException e) {
        }
    }


    /**
     * 注册过滤器
     */
    public void registerFilters() {
        // 这里为注册BeanDefinition的开关，默认为true
        if (allInterfaces) {
            // include all interfaces
            // 满足该条件即可注册
            addIncludeFilter(new TypeFilter() {
                @Override
                public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                    // 获取元数据 的 className
                    String className = metadataReader.getClassMetadata().getClassName();
                    // 反射获取Class对象
                    Class clazz = null;
                    try {
                        clazz = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                    }
                    if (clazz == null) {
                        return false;
                    }
                    // 获取该Class对象上的注解
                    Annotation[] baseAnns = clazz.getAnnotations();
                    // 判断注解是否包含BaseLifeCycle, 如果包含直接返回true
                    for (Annotation ann : baseAnns) {
                        Annotation lcAnn = ann.annotationType().getAnnotation(BaseLifeCycle.class);
                        if (lcAnn != null) {
                            return true;
                        }
                    }
                    // 获取该Class对象中所有的方法
                    Method[] methods = clazz.getMethods();
                    // 判断该类上方法是否包含注解 MethodLifeCycle
                    for (Method method : methods) {
                        Annotation[] mthAnns = method.getAnnotations();
                        for (Annotation ann : mthAnns) {
                            Annotation mlcAnn = ann.annotationType().getAnnotation(MethodLifeCycle.class);
                            if (mlcAnn != null) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }

        // exclude package-info.java
        // 不满足该条件才可注册
        addExcludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                // 获取元数据 的 className
                String className = metadataReader.getClassMetadata().getClassName();
                // 如果满足 className末尾为 package-info，不允许实例化
                return className.endsWith("package-info");
            }
        });
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        GenericBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (GenericBeanDefinition) holder.getBeanDefinition();

            if (logger.isDebugEnabled()) {
                logger.debug("[Forest] Creating Forest Client Bean with name '" + holder.getBeanName()
                        + "' and Proxy of '" + definition.getBeanClassName() + "' client interface");
            }

            String beanClassName = definition.getBeanClassName();
            ClientFactoryBeanUtils.setupClientFactoryBean(definition, configurationId, beanClassName);
            logger.info("[Forest] Created Forest Client Bean with name '" + holder.getBeanName()
                    + "' and Proxy of '" + beanClassName + "' client interface");

        }
    }


    /**
     * 重写扫描逻辑
     * @param basePackages 请求接口类所在的包路径，只能是第一层的包，不包含子包
     * @return BeanDefinitionHolder实例集合
     */
    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            logger.warn("[Forest] No Forest client is found in package '" + Arrays.toString(basePackages) + "'.");
        }
        processBeanDefinitions(beanDefinitions);
        return beanDefinitions;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }
}
