package com.dtflys.forest.proxy;

import com.dtflys.forest.config.ForestConfiguration;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 代理工厂
 *
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2016-03-25 18:17
 */
public class ProxyFactory<T> {

    // 自定义上下文
    private ForestConfiguration configuration;

    // 需要代理的Class
    private Class<T> interfaceClass;

    public ProxyFactory(ForestConfiguration configuration, Class<T> interfaceClass) {
        this.configuration = configuration;
        this.interfaceClass = interfaceClass;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * 创建代理类
     *
     * @return
     */
    public T createInstance() {
        // 1. 先获取自定义上下文中请求接口的实例缓存对象
        // 2. 获取实例缓存
        T instance = (T) configuration.getInstanceCache().get(interfaceClass);
        // 判断是否允许缓存实例对象
        boolean cacheEnabled = configuration.isCacheEnabled();
        // 当条件为允许缓存实例对象 并且 实例缓存不为空时，直接返回实例对象
        if (cacheEnabled && instance != null) {
            return instance;
        }
        // 这里使用同步代码块，保证自定义上下文中的实例缓存对象 只能被一个线程使用
        synchronized (configuration.getInstanceCache()) {
            // 这里重复下，未加锁的操作
            instance = (T) configuration.getInstanceCache().get(interfaceClass);
            if (cacheEnabled && instance != null) {
                return instance;
            }
            // 生成 代理处理程序类
            InterfaceProxyHandler<T> interfaceProxyHandler = new InterfaceProxyHandler<T>(configuration, this, interfaceClass);
            instance = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass, ForestClientProxy.class}, interfaceProxyHandler);
            if (cacheEnabled) {
                configuration.getInstanceCache().put(interfaceClass, instance);
            }
            return instance;
        }
    }

}
