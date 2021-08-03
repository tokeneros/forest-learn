package com.dtflys.forest.springboot.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，为了指定包名进行扫描
 *
 * @author guihuo   (E-mail:1620657419@qq.com)
 * @since 2018-09-25 11:58
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({ForestScannerRegister.class})
public @interface ForestScan {

    /**
     * 包路径
     */
    String[] value() default {};

    String configuration() default "";

    /**
     * 包路径
     */
    String[] basePackages() default {};

    /**
     * 指定要加的Class类
     */
    Class<?>[] basePackageClasses() default {};

}
