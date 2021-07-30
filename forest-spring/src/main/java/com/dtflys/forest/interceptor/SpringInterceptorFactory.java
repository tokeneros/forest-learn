package com.dtflys.forest.interceptor;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 针对spring的 拦截器工厂
 */
public class SpringInterceptorFactory extends DefaultInterceptorFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 创建拦截器
     * @param clazz 需要拦截的类
     * @param <T>
     * @return
     */
    @Override
    protected <T extends Interceptor> Interceptor createInterceptor(Class<T> clazz) {
        Interceptor interceptor = null;
        try {
            // 按照类型获取spring容器中的 拦截器
            interceptor = applicationContext.getBean(clazz);
        } catch (Throwable th) {}
        // 如果spring中没有对应的 拦截器，自己生成一个拦截器
        if (interceptor != null) {
            interceptorMap.put(clazz, interceptor);
        } else {
            return super.createInterceptor(clazz);
        }
        return interceptor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
