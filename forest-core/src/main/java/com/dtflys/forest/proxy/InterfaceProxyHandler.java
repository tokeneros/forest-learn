package com.dtflys.forest.proxy;

import com.dtflys.forest.annotation.BaseLifeCycle;
import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.BaseURL;
import com.dtflys.forest.annotation.MethodLifeCycle;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.config.VariableScope;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.interceptor.Interceptor;
import com.dtflys.forest.interceptor.InterceptorFactory;
import com.dtflys.forest.lifecycles.BaseAnnotationLifeCycle;
import com.dtflys.forest.logging.ForestLogHandler;
import com.dtflys.forest.logging.LogConfiguration;
import com.dtflys.forest.mapping.MappingTemplate;
import com.dtflys.forest.mapping.MappingVariable;
import com.dtflys.forest.reflection.ForestMethod;
import com.dtflys.forest.reflection.MetaRequest;
import com.dtflys.forest.utils.StringUtils;
import com.dtflys.forest.utils.URLUtils;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 代理处理程序类，这里使用jdk的代理方法，只能实现interface接口类的代理
 *
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2016-05-04
 */
public class InterfaceProxyHandler<T> implements InvocationHandler, VariableScope {

    // 自定义上下文
    private ForestConfiguration configuration;

    // 代理工厂
    private ProxyFactory proxyFactory;

    // 代理类
    private Class<T> interfaceClass;

    // 代理方法
    private Map<Method, ForestMethod> forestMethodMap = new HashMap<Method, ForestMethod>();

    // 自定义元请求信息
    private MetaRequest baseMetaRequest = new MetaRequest();

    // 拦截器工厂
    private InterceptorFactory interceptorFactory;

    // 基础URL
    private String baseURL;

    // 日志配置信息
    private LogConfiguration baseLogConfiguration;

    // 构造方法-未指定任何Class，后面调用传入 Class 和 作用域
    private final Constructor<MethodHandles.Lookup> defaultMethodConstructor;

    // 实例化对象
    private MethodHandles.Lookup defaultMethodLookup;


    private List<Annotation> baseAnnotations = new LinkedList<>();


    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    /**
     * 代理处理程序类 构造方法
     *
     * @param configuration  自定义上下文
     * @param proxyFactory   代理工厂
     * @param interfaceClass 代理方法
     */
    public InterfaceProxyHandler(ForestConfiguration configuration, ProxyFactory proxyFactory, Class<T> interfaceClass) {
        this.configuration = configuration;
        this.proxyFactory = proxyFactory;
        this.interfaceClass = interfaceClass;
        // 拦截器工厂
        this.interceptorFactory = configuration.getInterceptorFactory();

        try {
            // 创建一个MethodHandles.Lookup对象的构造函数 含有两个参数 Class 和 int TODO 这里需要仔细去看看...
            defaultMethodConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            // 如果该构造函数私有，设置可用
            if (!defaultMethodConstructor.isAccessible()) {
                defaultMethodConstructor.setAccessible(true);
            }
            // 通过构造函数创建 MethodHandles.Lookup 对象
            defaultMethodLookup = defaultMethodConstructor.newInstance(interfaceClass, MethodHandles.Lookup.PRIVATE);
        } catch (Throwable e) {
            throw new ForestRuntimeException(e);
        }
        prepareBaseInfo();
        initMethods();
    }


    private void prepareBaseInfo() {
        // 获取接口类上所有注解
        Annotation[] annotations = interfaceClass.getAnnotations();

        for (int i = 0; i < annotations.length; i++) {
            // 遍历注解
            Annotation annotation = annotations[i];
            // 判断注解是否属于 BaseURL.class
            if (annotation instanceof BaseURL) {
                // 获取注解内容
                BaseURL baseURLAnn = (BaseURL) annotation;
                String value = baseURLAnn.value();
                // 如果value值为空，则跳转到下一个注解
                if (value == null || value.trim().length() == 0) {
                    continue;
                }
                // 基础URL设置
                baseURL = value.trim();
                // 元请求信息，基础URL设置
                baseMetaRequest.setUrl(baseURL);
            } else {
                // 这里应该是后期扩展的，按照注解类上的生命周期来实现拦截器操作，这里可以多样化操作，框架设计较好 TODO 多参考下
                // 基础生命周期
                BaseLifeCycle baseLifeCycle = annotation.annotationType().getAnnotation(BaseLifeCycle.class);
                // 方法生命周期
                MethodLifeCycle methodLifeCycle = annotation.annotationType().getAnnotation(MethodLifeCycle.class);
                if (baseLifeCycle != null || methodLifeCycle != null) {
                    // 通过注解形式添加 生命周期拦截器
                    if (baseLifeCycle != null) {
                        // 获取基础生命周期拦截器Class
                        Class<? extends BaseAnnotationLifeCycle> interceptorClass = baseLifeCycle.value();
                        // 如果基础生命周期拦截器Class不为空，则实例化生命周期拦截器
                        if (interceptorClass != null) {
                            // 通过拦截器工厂获取对应的拦截器，如果不存在则创建新的拦截器，这里工厂做了一步缓存
                            BaseAnnotationLifeCycle baseInterceptor = interceptorFactory.getInterceptor(interceptorClass);
                            // 调用基础拦截器 初始化操作，默认不做任何操作
                            baseInterceptor.onProxyHandlerInitialized(this, annotation);
                        }
                    }
                    baseAnnotations.add(annotation);
                }
            }
        }
    }


    private void initMethods() {
        Method[] methods = interfaceClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if(method.isDefault()){
                continue;
            }
            ForestMethod forestMethod = new ForestMethod(this, configuration, method);
            forestMethodMap.put(method, forestMethod);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (method.isDefault()) {
          return invokeDefaultMethod(proxy, method, args);
        }
        if ("toString".equals(methodName) && (args == null || args.length == 0)) {
            return "{Forest Proxy Object of " + interfaceClass.getName() + "}";
        }
        if ("equals".equals(methodName) && (args != null && args.length == 1)) {
            Object obj = args[0];
            if (Proxy.isProxyClass(obj.getClass())) {
                InvocationHandler h1 = Proxy.getInvocationHandler(proxy);
                InvocationHandler h2 = Proxy.getInvocationHandler(obj);
                return h1.equals(h2);
            }
            return false;
        }
        ForestMethod forestMethod = forestMethodMap.get(method);
        return forestMethod.invoke(args);
    }

  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
          throws Throwable {
    return defaultMethodLookup.unreflectSpecial(method, interfaceClass)
            .bindTo(proxy).invokeWithArguments(args);
  }

    public MetaRequest getBaseMetaRequest() {
        return baseMetaRequest;
    }

    @Override
    public boolean isVariableDefined(String name) {
        return configuration.isVariableDefined(name);
    }

    @Override
    public Object getVariableValue(String name) {
        return configuration.getVariableValue(name);
    }


    public List<Annotation> getBaseAnnotations() {
        return baseAnnotations;
    }

    @Override
    public MappingVariable getVariable(String name) {
        return null;
    }

    @Override
    public ForestConfiguration getConfiguration() {
        return configuration;
    }

    public LogConfiguration getBaseLogConfiguration() {
        return baseLogConfiguration;
    }

    public void setBaseLogConfiguration(LogConfiguration baseLogConfiguration) {
        this.baseLogConfiguration = baseLogConfiguration;
    }
}
