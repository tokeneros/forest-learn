package com.dtflys.forest.springboot.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author guihuo   (E-mail:1620657419@qq.com)
 * @since 2018-09-25 11:59
 *
 * @see BeanFactoryAware 获取spring创建的BeanFactory
 * @see ResourceLoaderAware 获取spring资源加载的类
 * 上面的两个方法会在 {@link ImportBeanDefinitionRegistrar#registerBeanDefinitions} 之前调用
 * @see ImportBeanDefinitionRegistrar spring提供给我们在BeanDefinition定义级别可执行的操作
 *
 */
public class ForestScannerRegister implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {


    private ResourceLoader resourceLoader;

    public static List<String> basePackages = new ArrayList<>();

    public static String configurationId;

    private BeanFactory beanFactory;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 我们可以自定义一些BeanDefinition注册进去
     * @param importingClassMetadata 元注解信息
     * @param registry BeanDefinition注册中心
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        // 判断元注解信息 是否含有自定义的注解
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(ForestScan.class.getName());
        if (annotationAttributes == null) {
            // 如果不包含自定义注解，识别根目录
            ForestScannerRegister.basePackages.addAll(AutoConfigurationPackages.get(beanFactory));
        } else {
            // 如果含有该自定义注解
            AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(annotationAttributes);
            // 获取自定义注解value某个属性值，该处为多个包路径
            for (String pkg : annoAttrs.getStringArray("value")) {
                if (StringUtils.hasText(pkg)) {
                    basePackages.add(pkg);
                }
            }
            // 获取自定义注解basePackages某个属性值，该处为多个包路径
            for (String pkg : annoAttrs.getStringArray("basePackages")) {
                if (StringUtils.hasText(pkg)) {
                    basePackages.add(pkg);
                }
            }

            // 获取自定义注解basePackageClasses某个属性值，通过Class类获取包路径
            for (Class<?> clazz : annoAttrs.getClassArray("basePackageClasses")) {
                basePackages.add(ClassUtils.getPackageName(clazz));
            }

            // 本次创建的BeanDefinition的id。也就是map中的key标识。注意不要重复
            configurationId = annoAttrs.getString("configuration");
        }

        // 之前是该处进行bean扫描的
//        ClassPathClientScanner scanner = new ClassPathClientScanner(configurationId, registry);
//        // this check is needed in Spring 3.1
//        if (resourceLoader != null) {
//            scanner.setResourceLoader(resourceLoader);
//        }
//
//        scanner.registerFilters();
//        scanner.doScan(StringUtils.toStringArray(basePackages));

    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
