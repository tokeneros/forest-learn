/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Gong
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dtflys.forest.interceptor;

import com.dtflys.forest.exceptions.ForestRuntimeException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认拦截器工厂
 *
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-05-15 11:18
 */
public class DefaultInterceptorFactory implements InterceptorFactory {

    /**
     * 拦截器实例缓存
     */
    protected final Map<Class, Interceptor> interceptorMap = new ConcurrentHashMap<>();

    /**
     * 拦截器调用链
     */
    protected InterceptorChain interceptorChain = new InterceptorChain();

    @Override
    public InterceptorChain getInterceptorChain() {
        return interceptorChain;
    }

    /**
     * 拦截器获取
     *
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T extends Interceptor> T getInterceptor(Class<T> clazz) {
        // 判断当前拦截器中是否含有该 Class类对应的拦截器
        Interceptor interceptor = interceptorMap.get(clazz);
        // 如果拦截器不存在，则新建
        if (interceptor == null) {
            // 通过同步代码块。保证线程安全
            synchronized (DefaultInterceptorFactory.class) {
                interceptor = interceptorMap.get(clazz);
                if (interceptor == null) {
                    // 创建拦截器
                    interceptor = createInterceptor(clazz);
                }
            }
        }
        // 拦截器
        return (T) interceptor;
    }

    /**
     * 创建拦截器
     * @param clazz 需要拦截的类
     * @param <T> {@link InterceptorChain}
     * @return
     */
    protected <T extends Interceptor> Interceptor createInterceptor(Class<T> clazz) {
        Interceptor interceptor;
        try {
            // 实例化拦截器
            interceptor = clazz.newInstance();
            // 存放到缓存中
            interceptorMap.put(clazz, interceptor);
        } catch (InstantiationException e) {
            throw new ForestRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new ForestRuntimeException(e);
        }
        return interceptor;
    }

}
