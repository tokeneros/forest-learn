package com.dtflys.forest.reflection;

import com.dtflys.forest.annotation.BaseLifeCycle;
import com.dtflys.forest.annotation.MethodLifeCycle;
import com.dtflys.forest.annotation.ParamLifeCycle;
import com.dtflys.forest.annotation.RequestAttributes;
import com.dtflys.forest.backend.ContentType;
import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnLoadCookie;
import com.dtflys.forest.callback.OnProgress;
import com.dtflys.forest.callback.OnSaveCookie;
import com.dtflys.forest.callback.OnSuccess;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.config.VariableScope;
import com.dtflys.forest.converter.ForestConverter;
import com.dtflys.forest.converter.json.ForestJsonConverter;
import com.dtflys.forest.exceptions.ForestInterceptorDefineException;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.filter.Filter;
import com.dtflys.forest.http.ForestQueryParameter;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestRequestBody;
import com.dtflys.forest.http.ForestRequestType;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.http.body.RequestBodyBuilder;
import com.dtflys.forest.http.body.StringRequestBody;
import com.dtflys.forest.interceptor.Interceptor;
import com.dtflys.forest.interceptor.InterceptorAttributes;
import com.dtflys.forest.interceptor.InterceptorFactory;
import com.dtflys.forest.lifecycles.BaseAnnotationLifeCycle;
import com.dtflys.forest.lifecycles.MethodAnnotationLifeCycle;
import com.dtflys.forest.lifecycles.ParameterAnnotationLifeCycle;
import com.dtflys.forest.lifecycles.method.RequestLifeCycle;
import com.dtflys.forest.logging.DefaultLogHandler;
import com.dtflys.forest.logging.LogConfiguration;
import com.dtflys.forest.logging.ForestLogHandler;
import com.dtflys.forest.mapping.MappingParameter;
import com.dtflys.forest.mapping.MappingTemplate;
import com.dtflys.forest.mapping.MappingVariable;
import com.dtflys.forest.mapping.SubVariableScope;
import com.dtflys.forest.multipart.ForestMultipart;
import com.dtflys.forest.multipart.ForestMultipartFactory;
import com.dtflys.forest.proxy.InterfaceProxyHandler;
import com.dtflys.forest.retryer.Retryer;
import com.dtflys.forest.ssl.SSLKeyStore;
import com.dtflys.forest.utils.ForestDataType;
import com.dtflys.forest.utils.NameUtils;
import com.dtflys.forest.utils.ReflectUtils;
import com.dtflys.forest.utils.RequestNameValue;
import com.dtflys.forest.utils.StringUtils;
import com.dtflys.forest.utils.URLUtils;
import okio.ByteString;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static com.dtflys.forest.mapping.MappingParameter.*;

/**
 * 通过代理调用的实际执行的方法对象
 * @author gongjun
 * @since 2016-05-03
 */
public class ForestMethod<T> implements VariableScope {

    private final InterfaceProxyHandler interfaceProxyHandler;
    private final ForestConfiguration configuration;
    private InterceptorFactory interceptorFactory;
    private final Method method;
    private String[] methodNameItems;
    private Class returnClass;
    private Type returnType;
    private MetaRequest metaRequest;
    private MappingTemplate baseUrlTemplate;
    private MappingTemplate urlTemplate;
    private MappingTemplate typeTemplate;
    private MappingTemplate dataTypeTemplate;
    private Integer baseTimeout = null;
    private Integer timeout = null;
    private MappingTemplate sslProtocolTemplate;
    private Class baseRetryerClass = null;
    private Integer baseRetryCount = null;
    private Long baseMaxRetryInterval;
    private Integer retryCount = null;
    private long maxRetryInterval;
    private MappingTemplate baseEncodeTemplate = null;
    private MappingTemplate encodeTemplate = null;
    private MappingTemplate charsetTemplate = null;
    private MappingTemplate baseContentTypeTemplate;
    private MappingTemplate baseUserAgentTemplate;
    private MappingTemplate baseCharsetTemplate;
    private MappingTemplate baseSslProtocolTemplate;
    private MappingTemplate contentTypeTemplate;
    private MappingTemplate userAgentTemplate;
    private long progressStep = -1;
    private ForestConverter decoder = null;
    private MappingTemplate sslKeyStoreId;
    private MappingTemplate[] dataTemplateArray;
    private MappingTemplate[] headerTemplateArray;
    private MappingParameter[] parameterTemplateArray;
    private MappingParameter[] forestParameters;
    private List<MappingParameter> namedParameters = new ArrayList<>();
    private List<ForestMultipartFactory> multipartFactories = new ArrayList<>();
    private Map<String, MappingVariable> variables = new HashMap<>();
    private MappingParameter onSuccessParameter = null;
    private MappingParameter onErrorParameter = null;
    private MappingParameter onProgressParameter = null;
    private MappingParameter onLoadCookieParameter = null;
    private MappingParameter onSaveCookieParameter = null;
    private List<Interceptor> globalInterceptorList;
    private List<Interceptor> baseInterceptorList;
    private List<Interceptor> interceptorList;
    private List<InterceptorAttributes> interceptorAttributesList;
    private Type onSuccessClassGenericType = null;
    private Class retryerClass = null;
    private boolean async = false;

    private LogConfiguration baseLogConfiguration = null;

    private boolean logEnabled = true;
    private boolean logRequest = true;
    private boolean logResponseStatus = true;
    private boolean logResponseContent = false;
    private ForestLogHandler logHandler = null;
    private LogConfiguration logConfiguration = null;
    private Map<String, Object> extensionParameters = new HashMap<>();

    public ForestMethod(InterfaceProxyHandler interfaceProxyHandler, ForestConfiguration configuration, Method method) {
        this.interfaceProxyHandler = interfaceProxyHandler;
        this.configuration = configuration;
        this.method = method;
        this.interceptorFactory = configuration.getInterceptorFactory();
        this.methodNameItems = NameUtils.splitCamelName(method.getName());
        this.forestParameters = new MappingParameter[method.getParameterCount()];
        processBaseProperties();
        processMethodAnnotations();
    }

    @Override
    public ForestConfiguration getConfiguration() {
        return configuration;
    }


    @Override
    public boolean isVariableDefined(String name) {
        return configuration.isVariableDefined(name);
    }

    @Override
    public Object getVariableValue(String name) {
        return configuration.getVariableValue(name);
    }

    public MappingTemplate makeTemplate(String text) {
        return new MappingTemplate(text, this, configuration.getProperties(), forestParameters);
    }


    public Class getReturnClass() {
        return returnClass;
    }


    @Override
    public MappingVariable getVariable(String name) {
        return variables.get(name);
    }

    public MappingParameter[] getParameters() {
        return forestParameters;
    }

    private void processBaseProperties() {
        MetaRequest baseMetaRequest = interfaceProxyHandler.getBaseMetaRequest();
        String baseUrl = baseMetaRequest.getUrl();
        if (StringUtils.isNotBlank(baseUrl)) {
            baseUrlTemplate = makeTemplate(baseUrl);
        }
        String baseContentEncoding = baseMetaRequest.getContentEncoding();
        if (StringUtils.isNotBlank(baseContentEncoding)) {
            baseEncodeTemplate = makeTemplate(baseContentEncoding);
        }
        String baseContentType = baseMetaRequest.getContentType();
        if (StringUtils.isNotBlank(baseContentType)) {
            baseContentTypeTemplate = makeTemplate(baseContentType);
        }
        String baseUserAgent = baseMetaRequest.getUserAgent();
        if (StringUtils.isNotBlank(baseUserAgent)) {
            baseUserAgentTemplate = makeTemplate(baseUserAgent);
        }
        String baseCharset = baseMetaRequest.getCharset();
        if (StringUtils.isNotBlank(baseCharset)) {
            baseCharsetTemplate = makeTemplate(baseCharset);
        }
        String baseSslProtocol = baseMetaRequest.getSslProtocol();
        if (StringUtils.isNotBlank(baseSslProtocol)) {
            baseSslProtocolTemplate = makeTemplate(baseSslProtocol);
        }

        baseLogConfiguration = interfaceProxyHandler.getBaseLogConfiguration();

        baseTimeout = baseMetaRequest.getTimeout();
        baseRetryerClass = baseMetaRequest.getRetryer();
        baseRetryCount = baseMetaRequest.getRetryCount();
        baseMaxRetryInterval = baseMetaRequest.getMaxRetryInterval();

        List<Class> globalInterceptorClasses = configuration.getInterceptors();
        if (globalInterceptorClasses != null && globalInterceptorClasses.size() > 0) {
            globalInterceptorList = new LinkedList<>();
            for (Class clazz : globalInterceptorClasses) {
                if (!Interceptor.class.isAssignableFrom(clazz) || clazz.isInterface()) {
                    throw new ForestRuntimeException("Class [" + clazz.getName() + "] is not a implement of [" +
                            Interceptor.class.getName() + "] interface.");
                }
                Interceptor interceptor = interceptorFactory.getInterceptor(clazz);
                globalInterceptorList.add(interceptor);
            }
        }

        Class[] baseInterceptorClasses = baseMetaRequest.getInterceptor();
        if (baseInterceptorClasses != null && baseInterceptorClasses.length > 0) {
            baseInterceptorList = new LinkedList<>();
            for (int cidx = 0, len = baseInterceptorClasses.length; cidx < len; cidx++) {
                Class clazz = baseInterceptorClasses[cidx];
                if (!Interceptor.class.isAssignableFrom(clazz) || clazz.isInterface()) {
                    throw new ForestRuntimeException("Class [" + clazz.getName() + "] is not a implement of [" +
                            Interceptor.class.getName() + "] interface.");
                }
                Interceptor interceptor = interceptorFactory.getInterceptor(clazz);
                baseInterceptorList.add(interceptor);
            }
        }

        List<Annotation> baseAnnotationList = interfaceProxyHandler.getBaseAnnotations();
        Map<Annotation, Class> baseAnnMap = new HashMap<>(baseAnnotationList.size());
        for (Annotation annotation : baseAnnotationList) {
            Class interceptorClass = getAnnotationLifeCycleClass(annotation);
            baseAnnMap.put(annotation, interceptorClass);
        }
        addMetaRequestAnnotations(baseAnnMap);
    }

    private <T extends Interceptor> T addInterceptor(Class<T> interceptorClass) {
        if (interceptorList == null) {
            interceptorList = new LinkedList<>();
        }
        if (!Interceptor.class.isAssignableFrom(interceptorClass) || interceptorClass.isInterface()) {
            throw new ForestRuntimeException("Class [" + interceptorClass.getName() + "] is not a implement of [" +
                    Interceptor.class.getName() + "] interface.");
        }
        T interceptor = interceptorFactory.getInterceptor(interceptorClass);
        interceptorList.add(interceptor);
        return interceptor;
    }


    private Class getAnnotationLifeCycleClass(Annotation annotation) {
        Class<? extends Annotation> annType = annotation.annotationType();
        Class<? extends MethodAnnotationLifeCycle> interceptorClass = null;
        MethodLifeCycle methodLifeCycleAnn = annType.getAnnotation(MethodLifeCycle.class);
        if (methodLifeCycleAnn == null) {
            BaseLifeCycle baseLifeCycle = annType.getAnnotation(BaseLifeCycle.class);
            if (baseLifeCycle != null) {
                Class<? extends BaseAnnotationLifeCycle> baseAnnLifeCycleClass = baseLifeCycle.value();
                if (baseAnnLifeCycleClass != null) {
                    if (MethodAnnotationLifeCycle.class.isAssignableFrom(baseAnnLifeCycleClass)) {
                        interceptorClass = (Class<? extends MethodAnnotationLifeCycle>) baseAnnLifeCycleClass;
                    } else {
                        return baseAnnLifeCycleClass;
                    }
                }
            }
        }
        if (methodLifeCycleAnn != null || interceptorClass != null) {
            if (interceptorClass == null) {
                interceptorClass = methodLifeCycleAnn.value();
                if (!Interceptor.class.isAssignableFrom(interceptorClass)) {
                    throw new ForestInterceptorDefineException(interceptorClass);
                }
            }
        }

        return interceptorClass;
    }

    /**
     * 设置扩展参数值
     *
     * @param name 参数名
     * @param value 参数值
     */
    public void setExtensionParameterValue(String name, Object value) {
        this.extensionParameters.put(name, value);
    }

    /**
     * 获取扩展参数值
     *
     * @param name 参数名
     * @return 参数值
     */
    public Object getExtensionParameterValue(String name) {
        return this.extensionParameters.get(name);
    }

    /**
     * 添加元请求注释表
     * @param annMap 请求注释表
     */
    private void addMetaRequestAnnotations(Map<Annotation, Class> annMap) {
        for (Map.Entry<Annotation, Class> entry : annMap.entrySet()) {
            addMetaRequestAnnotation(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 添加元请求注释
     * @param annotation 注解
     * @param interceptorClass 拦截器类
     */
    private void addMetaRequestAnnotation(Annotation annotation, Class interceptorClass) {
        Class<? extends Annotation> annType = annotation.annotationType();
        RequestAttributes requestAttributesAnn = annType.getAnnotation(RequestAttributes.class);
        if (requestAttributesAnn != null) {
            Map<String, Object> attrTemplates = ReflectUtils.getAttributesFromAnnotation(annotation);
            for (String key : attrTemplates.keySet()) {
                Object value = attrTemplates.get(key);
                if (value instanceof CharSequence) {
                    MappingTemplate template = makeTemplate(value.toString());
                    attrTemplates.put(key, template);
                } else if (String[].class.isAssignableFrom(value.getClass())) {
                    String[] stringArray = (String[]) value;
                    int len = stringArray.length;
                    MappingTemplate[] templates = new MappingTemplate[stringArray.length];
                    for (int i = 0; i < len; i++) {
                        String item = stringArray[i];
                        MappingTemplate template = makeTemplate(item);
                        templates[i] = template;
                    }
                    attrTemplates.put(key, templates);
                }
            }

            InterceptorAttributes attributes = new InterceptorAttributes(interceptorClass, attrTemplates);
            if (interceptorAttributesList == null) {
                interceptorAttributesList = new LinkedList<>();
            }
            interceptorAttributesList.add(attributes);
        }
        Interceptor interceptor = addInterceptor(interceptorClass);
        if (interceptor instanceof MethodAnnotationLifeCycle) {
            MethodAnnotationLifeCycle lifeCycle = (MethodAnnotationLifeCycle) interceptor;
            lifeCycle.onMethodInitialized(this, annotation);
        }
    }


    public void setMetaRequest(MetaRequest metaRequest) {
        if (metaRequest != null && this.metaRequest != null) {
            throw new ForestRuntimeException("[Forest] annotation \""
                    + metaRequest.getRequestAnnotation().annotationType().getName() + "\" can not be added on method \""
                    + method.getName() + "\", because a similar annotation \""
                    + metaRequest.getRequestAnnotation().annotationType().getName() + "\" has already been attached to this method.");
        }
        this.metaRequest = metaRequest;
    }

    /**
     * 获取Forest方法对应的Java原生方法
     * @return Java原生方法，{@link java.lang.reflect.Method}类实例
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取方法名
     * @return 方法名字符串
     */
    public String getMethodName() {
        return method.getName();
    }

    /**
     * 获取元请求信息
     * @return 元请求对象，{@link MetaRequest}类实例
     */
    public MetaRequest getMetaRequest() {
        return metaRequest;
    }


    /**
     * 处理方法上的注解列表
     */
    private void processMethodAnnotations() {
        Annotation[] annotations = method.getAnnotations();
        Map<Annotation, Class> requestAnns = new LinkedHashMap();
        Map<Annotation, Class> methodAnns = new LinkedHashMap<>();

        // 对注解分类
        for (int i = 0; i < annotations.length; i++) {
            Annotation ann = annotations[i];
            Class interceptorClass = getAnnotationLifeCycleClass(ann);
            if (interceptorClass == null) {
                continue;
            }
            if (RequestLifeCycle.class.isAssignableFrom(interceptorClass)) {
                requestAnns.put(ann, interceptorClass);
            } else {
                methodAnns.put(ann, interceptorClass);
            }
        }

        // 先添加请求类注解
        addMetaRequestAnnotations(requestAnns);

        // 再添加普通方法类注解
        addMetaRequestAnnotations(methodAnns);

        // 处理请求元信息
        if (this.metaRequest != null) {
            processMetaRequest(this.metaRequest);
        }

        returnClass = method.getReturnType();
    }

    private void processMetaRequest(MetaRequest metaRequest) {
        Class[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnns = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();

        parameterTemplateArray = new MappingParameter[paramTypes.length];
        processParameters(parameters, genericParamTypes, paramAnns);

        urlTemplate = makeTemplate(metaRequest.getUrl());
        typeTemplate = makeTemplate(metaRequest.getType());
        dataTypeTemplate = makeTemplate(metaRequest.getDataType());
        if (StringUtils.isNotEmpty(metaRequest.getContentType())) {
            contentTypeTemplate = makeTemplate(metaRequest.getContentType());
        }
        if (StringUtils.isNotEmpty(metaRequest.getUserAgent())) {
            userAgentTemplate = makeTemplate(metaRequest.getUserAgent());
        }
        sslKeyStoreId = makeTemplate(metaRequest.getKeyStore());
        if (StringUtils.isNotEmpty(metaRequest.getContentEncoding())) {
            encodeTemplate = makeTemplate(metaRequest.getContentEncoding());
        }
        charsetTemplate = makeTemplate(metaRequest.getCharset());
        sslProtocolTemplate = makeTemplate(metaRequest.getSslProtocol());
        progressStep = metaRequest.getProgressStep();
        async = metaRequest.isAsync();
        retryerClass = metaRequest.getRetryer();
        Class decoderClass = metaRequest.getDecoder();
        String[] dataArray = metaRequest.getData();
        String[] headerArray = metaRequest.getHeaders();
        int tout = metaRequest.getTimeout();
        if (tout > 0) {
            timeout = tout;
        }
        Integer rtnum = metaRequest.getRetryCount();
        if (rtnum != null && rtnum >= 0) {
            retryCount = rtnum;
        }
        maxRetryInterval = metaRequest.getMaxRetryInterval();
        logEnabled = configuration.isLogEnabled();
        if (!logEnabled) {
            logEnabled = metaRequest.isLogEnabled();
        }
        logRequest = configuration.isLogRequest();
        logResponseStatus = configuration.isLogResponseStatus();
        logResponseContent = configuration.isLogResponseContent();

        LogConfiguration metaLogConfiguration = metaRequest.getLogConfiguration();
        if (metaLogConfiguration == null && baseLogConfiguration != null) {
            metaLogConfiguration = baseLogConfiguration;
        }
        if (metaLogConfiguration != null) {
            logEnabled = metaLogConfiguration.isLogEnabled();
            logRequest = metaLogConfiguration.isLogRequest();
            logResponseStatus = metaLogConfiguration.isLogResponseStatus();
            logResponseContent = metaLogConfiguration.isLogResponseContent();
            logHandler = metaLogConfiguration.getLogHandler();
            if (logHandler == null && baseLogConfiguration != null) {
                logHandler = baseLogConfiguration.getLogHandler();
            }
        }
        if (logHandler == null && configuration.getLogHandler() != null) {
            logHandler = configuration.getLogHandler();
        }
        if (logHandler == null) {
            logHandler = new DefaultLogHandler();
        }

        logConfiguration = new LogConfiguration();
        logConfiguration.setLogEnabled(logEnabled);
        logConfiguration.setLogRequest(logRequest);
        logConfiguration.setLogResponseStatus(logResponseStatus);
        logConfiguration.setLogResponseContent(logResponseContent);
        logConfiguration.setLogHandler(logHandler);


        dataTemplateArray = new MappingTemplate[dataArray.length];
        for (int j = 0; j < dataArray.length; j++) {
            String data = dataArray[j];
            MappingTemplate dataTemplate = makeTemplate(data);
            dataTemplateArray[j] = dataTemplate;
        }

        headerTemplateArray = new MappingTemplate[headerArray.length];
        for (int j = 0; j < headerArray.length; j++) {
            String header = headerArray[j];
            MappingTemplate headerTemplate = makeTemplate(header);
            headerTemplateArray[j] = headerTemplate;
        }

        Class[] interceptorClasses = metaRequest.getInterceptor();
        if (interceptorClasses != null && interceptorClasses.length > 0) {
            for (int cidx = 0, len = interceptorClasses.length; cidx < len; cidx++) {
                Class interceptorClass = interceptorClasses[cidx];
                addInterceptor(interceptorClass);
            }
        }


        if (decoderClass != null && ForestConverter.class.isAssignableFrom(decoderClass)) {
            try {
                this.decoder = (ForestConverter) decoderClass.newInstance();
            } catch (InstantiationException e) {
                throw new ForestRuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new ForestRuntimeException(e);
            }
        }

    }

    /**
     * 处理参数列表
     * @param parameters 参数数组，{@link Parameter}类数组实例
     * @param genericParamTypes 参数类型数组
     * @param paramAnns 参数注解的二维数组
     */
    private void processParameters(Parameter[] parameters, Type[] genericParamTypes, Annotation[][] paramAnns) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class paramType = param.getType();
            Annotation[] anns = paramAnns[i];
            MappingParameter parameter = new MappingParameter(paramType);
            parameter.setIndex(i);
            parameter.setName(param.getName());
            parameterTemplateArray[i] = parameter;
            if (OnSuccess.class.isAssignableFrom(paramType)) {
                onSuccessParameter = parameter;
                Type genType = genericParamTypes[i];
                onSuccessClassGenericType = getGenericClassOrType(genType, 0);
            } else if (OnError.class.isAssignableFrom(paramType)) {
                onErrorParameter = parameter;
            } else if (OnProgress.class.isAssignableFrom(paramType)) {
                onProgressParameter = parameter;
            } else if (OnSaveCookie.class.isAssignableFrom(paramType)) {
                onSaveCookieParameter = parameter;
            } else if (OnLoadCookie.class.isAssignableFrom(paramType)) {
                onLoadCookieParameter = parameter;
            }
            processParameterAnnotation(parameter, anns);
        }
    }

    /**
     * 处理参数的注解
     * @param parameter 方法参数-字符串模板解析对象，{@link MappingParameter}类实例
     * @param anns 方法参数注解的二维数组
     */
    private void processParameterAnnotation(MappingParameter parameter, Annotation[] anns) {
        for (int i = 0; i < anns.length; i++) {
            Annotation ann = anns[i];
            Class annType = ann.annotationType();
            ParamLifeCycle paramLifeCycleAnn = (ParamLifeCycle) annType.getAnnotation(ParamLifeCycle.class);
            if (paramLifeCycleAnn != null) {
                Class<? extends ParameterAnnotationLifeCycle> interceptorClass = paramLifeCycleAnn.value();
                if (!Interceptor.class.isAssignableFrom(interceptorClass)) {
                    throw new ForestInterceptorDefineException(interceptorClass);
                }
                ParameterAnnotationLifeCycle lifeCycle = addInterceptor(interceptorClass);
                lifeCycle.onParameterInitialized(this, parameter, ann);
            }
        }
    }

    /**
     * 添加命名参数
     * @param parameter 方法参数-字符串模板解析对象，{@link MappingParameter}类实例
     */
    public void addNamedParameter(MappingParameter parameter) {
        Integer index = parameter.getIndex();
        if (index != null && forestParameters[index] != null) {
            return;
        }
        namedParameters.add(parameter);
        if (index != null) {
            forestParameters[index] = parameter;
        }
    }

    /**
     * 添加变量
     * @param name 变量名
     * @param variable 变量对象，{@link MappingVariable}类实例
     */
    public void addVariable(String name, MappingVariable variable) {
        variables.put(name, variable);
    }

    /**
     * 添加Forest文件上传用的Mutlipart工厂
     * @param multipartFactory Forest文件上传用的Mutlipart工厂，{@link ForestMultipartFactory}类实例
     */
    public void addMultipartFactory(ForestMultipartFactory multipartFactory) {
        multipartFactories.add(multipartFactory);
    }

    /**
     * 处理参数的过滤器
     * @param parameter 方法参数-字符串模板解析对象，{@link MappingParameter}类实例
     * @param filterName 过滤器名称
     */
    public void processParameterFilter(MappingParameter parameter, String filterName) {
        if (StringUtils.isNotEmpty(filterName)) {
            String[] filterNameArray = filterName.split(",");
            for (String name : filterNameArray) {
                Filter filter = configuration.newFilterInstance(name);
                parameter.addFilter(filter);
            }
        }
    }

    /**
     * 给请求设置重试策略
     * @param retryerClass 重试策略类型
     * @param request Forest请求对象，{@link ForestRequest}类实例
     */
    private void setRetryerToRequest(Class retryerClass, ForestRequest request) {
        try {
            Constructor constructor = retryerClass.getConstructor(ForestRequest.class);
            Retryer retryer = (Retryer) constructor.newInstance(request);
            request.setRetryer(retryer);
        } catch (NoSuchMethodException e) {
            throw new ForestRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new ForestRuntimeException(e);
        } catch (InstantiationException e) {
            throw new ForestRuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new ForestRuntimeException(e);
        }
    }

    /**
     * 获得最终的请求类型
     * @param args 调用本对象对应方法时传入的参数数组
     * @return 请求类型，{@link ForestRequestType}枚举实例
     */
    private ForestRequestType type(Object[] args) {
        String renderedType = typeTemplate.render(args);
        if (StringUtils.isBlank(renderedType)) {
            String typeFromName = methodNameItems[0];
            ForestRequestType type = ForestRequestType.findType(typeFromName);
            if (type != null) {
                return type;
            }
            return ForestRequestType.GET;
        }
        ForestRequestType type = ForestRequestType.findType(renderedType);
        if (type != null) {
            return type;
        }
        throw new ForestRuntimeException("Http request type \"" + renderedType + "\" is not be supported.");
    }

    /**
     * 创建请求
     * @param args 调用本对象对应方法时传入的参数数组
     * @return Forest请求对象，{@link ForestRequest}类实例
     */
    private ForestRequest makeRequest(Object[] args) {
        MetaRequest baseMetaRequest = interfaceProxyHandler.getBaseMetaRequest();
        String baseUrl = null;
        if (baseUrlTemplate != null) {
            baseUrl = StringUtils.trimBegin(baseUrlTemplate.render(args));
        }
        if (urlTemplate == null) {
            throw new ForestRuntimeException("request URL is empty");
        }
        String renderedUrl = urlTemplate.render(args);
        ForestRequestType type = type(args);
        String baseContentEncoding = null;
        if (baseEncodeTemplate != null) {
            baseContentEncoding = baseEncodeTemplate.render(args);
        }
        String contentEncoding = null;
        if (encodeTemplate != null) {
            contentEncoding = encodeTemplate.render(args);
        }

        String baseContentType = null;
        if (baseContentTypeTemplate != null) {
            baseContentType = baseContentTypeTemplate.render(args);
        }
        String baseUserAgent = null;
        if (baseUserAgentTemplate != null) {
            baseUserAgent = baseUserAgentTemplate.render(args);
        }
        String charset = null;
        String renderedCharset = charsetTemplate.render(args);
        if (StringUtils.isNotBlank(renderedCharset)) {
            charset = renderedCharset;
        } else if (baseCharsetTemplate != null) {
            charset = baseCharsetTemplate.render(args);
        } else if (StringUtils.isNotBlank(configuration.getCharset())) {
            charset = configuration.getCharset();
        } else {
            charset = "UTF-8";
        }

        String sslProtocol = null;
        String renderedSslProtocol = sslProtocolTemplate.render(args);
        if (StringUtils.isNotBlank(renderedSslProtocol)) {
            sslProtocol = renderedSslProtocol;
        } else if (baseSslProtocolTemplate != null) {
            sslProtocol = baseSslProtocolTemplate.render(args);
        } else {
            sslProtocol = configuration.getSslProtocol();
        }

        String renderedContentType = null;
        if (contentTypeTemplate != null) {
            renderedContentType = contentTypeTemplate.render(args).trim();
        }
        String renderedUserAgent = null;
        if (userAgentTemplate != null) {
            renderedUserAgent = userAgentTemplate.render(args).trim();
        }
        List<RequestNameValue> nameValueList = new ArrayList<>();
        String [] headerArray = baseMetaRequest.getHeaders();
        MappingTemplate[] baseHeaders = null;
        if (headerArray != null && headerArray.length > 0) {
            baseHeaders = new MappingTemplate[headerArray.length];
            for (int j = 0; j < baseHeaders.length; j++) {
                MappingTemplate header = new MappingTemplate(
                        headerArray[j], this, configuration.getProperties(), forestParameters);
                baseHeaders[j] = header;
            }
        }


        renderedUrl = URLUtils.getValidURL(baseUrl, renderedUrl);


        // createExecutor and initialize http instance
        ForestRequest<T> request = new ForestRequest(configuration, this, args);
        request.setUrl(renderedUrl)
                .setType(type)
                .setCharset(charset)
                .setSslProtocol(sslProtocol)
                .setLogConfiguration(logConfiguration)
                .setAsync(async);

        if (StringUtils.isNotEmpty(renderedContentType)) {
            request.setContentType(renderedContentType);
        }

        if (StringUtils.isNotEmpty(contentEncoding)) {
            request.setContentEncoding(contentEncoding);
        }

        if (StringUtils.isNotEmpty(renderedUserAgent)) {
            request.setUserAgent(renderedUserAgent);
        }

        for (int i = 0; i < namedParameters.size(); i++) {
            MappingParameter parameter = namedParameters.get(i);
            if (parameter.isObjectProperties()) {
                int target = parameter.isUnknownTarget() ? type.getDefaultParamTarget() : parameter.getTarget();
                Object obj = args[parameter.getIndex()];
                if (obj == null && StringUtils.isNotEmpty(parameter.getDefaultValue())) {
                    obj = parameter.getConvertedDefaultValue(configuration.getJsonConverter());
                }
                if (parameter.isJsonParam()) {
                    String  json = "";
                    if (obj != null) {
                        ForestJsonConverter jsonConverter = configuration.getJsonConverter();
                        obj = parameter.getFilterChain().doFilter(configuration, obj);
                        json = jsonConverter.encodeToString(obj);
                    }
                    if (MappingParameter.isHeader(target)) {
                        request.addHeader(new RequestNameValue(parameter.getJsonParamName(), json, target)
                                .setDefaultValue(parameter.getDefaultValue()));
                    } else {
                        nameValueList.add(new RequestNameValue(parameter.getJsonParamName(), json, target, parameter.getPartContentType())
                                .setDefaultValue(parameter.getDefaultValue()));
                    }
                }
                else if (!parameter.getFilterChain().isEmpty()) {
                    obj = parameter.getFilterChain().doFilter(configuration, obj);
                    if (obj == null && StringUtils.isNotEmpty(parameter.getDefaultValue())) {
                        obj = parameter.getDefaultValue();
                    }
                    if (obj != null) {
                        if (MappingParameter.isHeader(target)) {
                            request.addHeader(new RequestNameValue(null, obj, target));
                        } else if (MappingParameter.isQuery(target)) {
                            request.addQuery(obj.toString(), null,
                                    parameter.isUrlEncode(), parameter.getCharset());
                        } else if (MappingParameter.isBody(target)) {
                            ForestRequestBody body = RequestBodyBuilder
                                    .type(obj.getClass())
                                    .build(obj, parameter.getDefaultValue());
                            request.addBody(body);
                        } else {
                            nameValueList.add(new RequestNameValue(obj.toString(), target, parameter.getPartContentType())
                                    .setDefaultValue(parameter.getDefaultValue()));
                        }
                    }
                }
                else if (obj instanceof CharSequence) {
                    if (MappingParameter.isQuery(target)) {
                        request.addQuery(ForestQueryParameter.createSimpleQueryParameter(obj)
                                .setDefaultValue(parameter.getDefaultValue()));
                    } else if (MappingParameter.isBody(target)) {
                        request.addBody(new StringRequestBody(obj.toString())
                                .setDefaultValue(parameter.getDefaultValue()));
                    }
                }
                else if (obj instanceof Map) {
                    Map map = (Map) obj;
                    for (Object key : map.keySet()) {
                        if (key instanceof CharSequence) {
                            Object value = map.get(key);
                            if (MappingParameter.isHeader(target)) {
                                request.addHeader(new RequestNameValue(String.valueOf(key), value, target));
                            } else if (MappingParameter.isBody(target)) {
                                request.addBody(String.valueOf(key), parameter.getPartContentType(), value);
                            } else if (MappingParameter.isQuery(target)) {
                                request.addQuery(String.valueOf(key), value,
                                        parameter.isUrlEncode(), parameter.getCharset());
                            }
                        }
                    }
                }
                else if (obj instanceof Iterable
                        || (obj != null
                        && (obj.getClass().isArray()
                        || ReflectUtils.isPrimaryType(obj.getClass())))) {
                    if (MappingParameter.isQuery(target)) {
                        if (parameter.isJsonParam()) {
                            request.addQuery(parameter.getName(), obj,
                                    parameter.isUrlEncode(), parameter.getCharset());
                        } else {
                            if (obj instanceof Iterable) {
                                for (Object subItem : (Iterable) obj) {
                                    if (subItem instanceof ForestQueryParameter) {
                                        request.addQuery((ForestQueryParameter) subItem);
                                    } else {
                                        request.addQuery(ForestQueryParameter.createSimpleQueryParameter(subItem));
                                    }
                                }
                            } else if (obj.getClass().isArray()) {
                                if (obj instanceof ForestQueryParameter[]) {
                                    request.addQuery((ForestQueryParameter[]) obj);
                                }
                            }
                        }
                    } else if (MappingParameter.isBody(target)) {
                        ForestRequestBody body = RequestBodyBuilder
                                .type(obj.getClass())
                                .build(obj, parameter.getDefaultValue());
                        request.addBody(body);
                    }
                }
                else if (MappingParameter.isBody(target)) {
                    ForestRequestBody body = RequestBodyBuilder
                            .type(obj.getClass())
                            .build(obj, parameter.getDefaultValue());
                    request.addBody(body);
                } else {
                    try {
                        List<RequestNameValue> list = getNameValueListFromObjectWithJSON(parameter, configuration, obj, type);
                        for (RequestNameValue nameValue : list) {
                            if (nameValue.isInHeader()) {
                                request.addHeader(nameValue);
                            } else {
                                nameValueList.add(nameValue);
                            }
                        }
                    } catch (Throwable th) {
                        throw new ForestRuntimeException(th);
                    }
                }
            }
            else if (parameter.getIndex() != null) {
                int target = parameter.isUnknownTarget() ? type.getDefaultParamTarget() : parameter.getTarget();
                RequestNameValue nameValue = new RequestNameValue(parameter.getName(), target, parameter.getPartContentType())
                        .setDefaultValue(parameter.getDefaultValue());
                Object obj = args[parameter.getIndex()];
                if (obj == null && StringUtils.isNotEmpty(nameValue.getDefaultValue())) {
                    obj = parameter.getConvertedDefaultValue(configuration.getJsonConverter());
                }
                if (obj != null) {
                    if (MappingParameter.isQuery(target) &&
                            obj.getClass().isArray() &&
                            !(obj instanceof byte[]) &&
                            !(obj instanceof Byte[])) {
                        int len = Array.getLength(obj);
                        for (int idx = 0; idx < len; idx++) {
                            Object arrayItem = Array.get(obj, idx);
                            ForestQueryParameter queryParameter = new ForestQueryParameter(
                                    parameter.getName(), arrayItem,
                                    parameter.isUrlEncode(), parameter.getCharset());
                            request.addQuery(queryParameter);
                        }
                    } else {
                        nameValue.setValue(obj);
                        if (MappingParameter.isHeader(target)) {
                            request.addHeader(nameValue);
                        } else if (MappingParameter.isQuery(target)) {
                            if (!parameter.isJsonParam() && obj instanceof Iterable) {
                                int index = 0;
                                MappingTemplate template = makeTemplate(parameter.getName());
                                VariableScope parentScope = template.getVariableScope();
                                for (Object subItem : (Iterable) obj) {
                                    SubVariableScope scope = new SubVariableScope(parentScope);
                                    scope.addVariableValue("_it", subItem);
                                    scope.addVariableValue("_index", index++);
                                    template.setVariableScope(scope);
                                    String name = template.render(args);
                                    request.addQuery(
                                            name, subItem,
                                            parameter.isUrlEncode(), parameter.getCharset());
                                }
                            } else {
                                request.addQuery(
                                        parameter.getName(), obj,
                                        parameter.isUrlEncode(), parameter.getCharset());
                            }
                        } else {
                            MappingTemplate template = makeTemplate(nameValue.getName());
                            if (obj instanceof Iterable && template.hasIterateVariable()) {
                                int index = 0;
                                VariableScope parentScope = template.getVariableScope();
                                for (Object subItem : (Iterable) obj) {
                                    SubVariableScope scope = new SubVariableScope(parentScope);
                                    template.setVariableScope(scope);
                                    scope.addVariableValue("_it", subItem);
                                    scope.addVariableValue("_index", index++);
                                    String name = template.render(args);
                                    nameValueList.add(
                                            new RequestNameValue(name, subItem, target, parameter.getPartContentType())
                                                    .setDefaultValue(parameter.getDefaultValue()));
                                }
                            } else {
                                nameValueList.add(nameValue);
                            }
                        }
                    }
                }
            }
        }

        if (request.getContentType() == null) {
            if (StringUtils.isNotEmpty(baseContentType)) {
                request.setContentType(baseContentType);
            }
        }

        if (request.getContentEncoding() == null) {
            if (StringUtils.isNotEmpty(baseContentEncoding)) {
                request.setContentEncoding(baseContentEncoding);
            }
        }

        if (request.getUserAgent() == null) {
            if (StringUtils.isNotEmpty(baseUserAgent)) {
                request.setUserAgent(baseUserAgent);
            }
        }

        List<ForestMultipart> multiparts = new ArrayList<>(multipartFactories.size());

        if (!multipartFactories.isEmpty() && request.getContentType() == null) {
            UUID uuid = UUID.randomUUID();
            String boundary = ByteString.encodeUtf8(uuid.toString()).utf8();
            request.setContentType(ContentType.MULTIPART_FORM_DATA + ";boundary=" + boundary);
        }

        for (int i = 0; i < multipartFactories.size(); i++) {
            ForestMultipartFactory factory = multipartFactories.get(i);
            MappingTemplate nameTemplate = factory.getNameTemplate();
            MappingTemplate fileNameTemplate = factory.getFileNameTemplate();
            int index = factory.getIndex();
            Object data = args[index];
            factory.addMultipart(nameTemplate, fileNameTemplate, data, multiparts, args);
        }

        request.setMultiparts(multiparts);
        // setup ssl keystore
        if (sslKeyStoreId != null) {
            SSLKeyStore sslKeyStore = null;
            String keyStoreId = sslKeyStoreId.render(args);
            if (StringUtils.isNotEmpty(keyStoreId)) {
                sslKeyStore = configuration.getKeyStore(keyStoreId);
                request.setKeyStore(sslKeyStore);
            }
        }

        if (decoder != null) {
            request.setDecoder(decoder);
        }
        if (progressStep >= 0) {
            request.setProgressStep(progressStep);
        }
        if (configuration.getDefaultParameters() != null) {
            request.addNameValue(configuration.getDefaultParameters());
        }
        if (baseHeaders != null && baseHeaders.length > 0) {
            for (MappingTemplate baseHeader : baseHeaders) {
                String headerText = baseHeader.render(args);
                String[] headerNameValue = headerText.split(":", 2);
                if (headerNameValue.length > 1) {
                    String name = headerNameValue[0].trim();
                    if (request.getHeader(name) == null) {
                        request.addHeader(name, headerNameValue[1].trim());
                    }
                }
            }
        }
        if (configuration.getDefaultHeaders() != null) {
            request.addHeaders(configuration.getDefaultHeaders());
        }

        List<RequestNameValue> dataNameValueList = new ArrayList<>();
        renderedContentType = request.getContentType();
        if (renderedContentType == null || renderedContentType.equalsIgnoreCase(ContentType.APPLICATION_X_WWW_FORM_URLENCODED)) {
            for (int i = 0; i < dataTemplateArray.length; i++) {
                MappingTemplate dataTemplate = dataTemplateArray[i];
                String data = dataTemplate.render(args);
                String[] paramArray = data.split("&");
                for (int j = 0; j < paramArray.length; j++) {
                    String dataParam = paramArray[j];
                    String[] dataNameValue = dataParam.split("=", 2);
                    if (dataNameValue.length > 0) {
                        String name = dataNameValue[0].trim();
                        RequestNameValue nameValue = new RequestNameValue(name, type.getDefaultParamTarget());
                        if (dataNameValue.length == 2) {
                            nameValue.setValue(dataNameValue[1].trim());
                        }
                        nameValueList.add(nameValue);
                        dataNameValueList.add(nameValue);
                    }
                }
            }
        } else {
            for (int i = 0; i < dataTemplateArray.length; i++) {
                MappingTemplate dataTemplate = dataTemplateArray[i];
                String data = dataTemplate.render(args);
                request.addBody(data);
            }
        }
        request.addNameValue(nameValueList);

        for (int i = 0; i < headerTemplateArray.length; i++) {
            MappingTemplate headerTemplate = headerTemplateArray[i];
            String header = headerTemplate.render(args);
            String[] headNameValue = header.split(":", 2);
            if (headNameValue.length > 0) {
                String name = headNameValue[0].trim();
                RequestNameValue nameValue = new RequestNameValue(name, TARGET_HEADER);
                if (headNameValue.length == 2) {
                    nameValue.setValue(headNameValue[1].trim());
                }
                request.addHeader(nameValue);
            }
        }

        if (timeout != null) {
            request.setTimeout(timeout);
        } else if (baseTimeout != null) {
            request.setTimeout(baseTimeout);
        } else if (configuration.getTimeout() != null) {
            request.setTimeout(configuration.getTimeout());
        }

        if (retryCount != null) {
            request.setRetryCount(retryCount);
        } else if (baseRetryCount != null) {
            request.setRetryCount(baseRetryCount);
        } else if (configuration.getRetryCount() != null) {
            request.setRetryCount(configuration.getRetryCount());
        }

        if (maxRetryInterval >= 0) {
            request.setMaxRetryInterval(maxRetryInterval);
        } else if (baseMaxRetryInterval != null) {
            request.setMaxRetryInterval(baseMaxRetryInterval);
        } else if (configuration.getMaxRetryInterval() >= 0) {
            request.setMaxRetryInterval(configuration.getMaxRetryInterval());
        }

        Class globalRetryerClass = configuration.getRetryer();

        if (retryerClass != null && Retryer.class.isAssignableFrom(retryerClass)) {
            setRetryerToRequest(retryerClass, request);
        } else if (baseRetryerClass != null && Retryer.class.isAssignableFrom(baseRetryerClass)) {
            setRetryerToRequest(baseRetryerClass, request);
        } else if (globalRetryerClass != null && Retryer.class.isAssignableFrom(globalRetryerClass)) {
            setRetryerToRequest(globalRetryerClass, request);
        }

        if (onSuccessParameter != null) {
            OnSuccess<?> onSuccessCallback = (OnSuccess<?>) args[onSuccessParameter.getIndex()];
            request.setOnSuccess(onSuccessCallback);
        }
        if (onErrorParameter != null) {
            OnError onErrorCallback = (OnError) args[onErrorParameter.getIndex()];
            request.setOnError(onErrorCallback);
        }
        if (onProgressParameter != null) {
            OnProgress onProgressCallback = (OnProgress) args[onProgressParameter.getIndex()];
            request.setOnProgress(onProgressCallback);
        }

        if (onSaveCookieParameter != null) {
            OnSaveCookie onSaveCookieCallback = (OnSaveCookie) args[onSaveCookieParameter.getIndex()];
            request.setOnSaveCookie(onSaveCookieCallback);
        }

        if (onLoadCookieParameter != null) {
            OnLoadCookie onLoadCookieCallback = (OnLoadCookie) args[onLoadCookieParameter.getIndex()];
            request.setOnLoadCookie(onLoadCookieCallback);
        }

        String dataType = dataTypeTemplate.render(args);
        if (StringUtils.isEmpty(dataType)) {
            request.setDataType(ForestDataType.TEXT);
        } else {
            dataType = dataType.toUpperCase();
            ForestDataType forestDataType = ForestDataType.findByName(dataType);
            request.setDataType(forestDataType);
        }

        if (interceptorAttributesList != null && interceptorAttributesList.size() > 0) {
            for (InterceptorAttributes attributes : interceptorAttributesList) {
                request.addInterceptorAttributes(attributes.getInterceptorClass(), attributes);
                request.getInterceptorAttributes(attributes.getInterceptorClass()).render(args);
            }
        }

        if (globalInterceptorList != null && globalInterceptorList.size() > 0) {
            for (Interceptor item : globalInterceptorList) {
                request.addInterceptor(item);
            }
        }

        if (baseInterceptorList != null && baseInterceptorList.size() > 0) {
            for (Interceptor item : baseInterceptorList) {
                request.addInterceptor(item);
            }
        }

        if (interceptorList != null && interceptorList.size() > 0) {
            for (Interceptor item : interceptorList) {
                request.addInterceptor(item);
            }
        }
        return request;
    }


    private List<RequestNameValue> getNameValueListFromObjectWithJSON(MappingParameter parameter, ForestConfiguration configuration, Object obj, ForestRequestType type) {
        Map<String, Object> propMap = ReflectUtils.convertObjectToMap(obj, configuration);
        List<RequestNameValue> nameValueList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : propMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                RequestNameValue nameValue = new RequestNameValue(name ,value,
                        parameter.isUnknownTarget() ? type.getDefaultParamTarget() : parameter.getTarget(),
                        parameter.getPartContentType());
                nameValueList.add(nameValue);
            }
        }
        return nameValueList;
    }

    /**
     * 调用方法
     * @param args 调用本对象对应方法时传入的参数数组
     * @return 调用本对象对应方法结束后返回的值，任意类型的对象实例
     */
    public Object invoke(Object[] args) {
        ForestRequest request = makeRequest(args);
        MethodLifeCycleHandler<T> lifeCycleHandler = new MethodLifeCycleHandler<>(
                this, onSuccessClassGenericType);
        request.setBackend(configuration.getBackend())
                .setLifeCycleHandler(lifeCycleHandler);
        lifeCycleHandler.handleInvokeMethod(request, this, args);
        // 如果返回类型为ForestRequest，直接返回请求对象
        if (ForestRequest.class.isAssignableFrom(returnClass)) {
            Type retType = getReturnType();
            if (retType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) retType;
                Type[] genTypes = parameterizedType.getActualTypeArguments();
                if (genTypes.length > 0) {
                    Type targetType = genTypes[0];
                    returnClass = ReflectUtils.getClassByType(targetType);
                    returnType = targetType;
                } else {
                    returnClass = String.class;
                    returnType = String.class;
                }
            }
            return request;
        }
        return request.execute();
    }


    /**
     * 获取泛型类型
     * @param genType 带泛型参数的类型，{@link Type}接口实例
     * @param index 泛型参数下标
     * @return 泛型参数中的类型，{@link Type}接口实例
     */
    private static Type getGenericClassOrType(Type genType, final int index) {

        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            return Object.class;
        }
        if (params[index] instanceof ParameterizedType) {
            return params[index];
        }
        if (!(params[index] instanceof Class)) {
            return Object.class;
        }
        return params[index];
    }

    /**
     * 获取方法返回值类型
     * @return 方法返回值类型，{@link Type}接口实例
     */
    public Type getReturnType() {
        if (returnType != null) {
            return returnType;
        }
        Type type = method.getGenericReturnType();
        return type;
    }

    /**
     * 获取请求结果类型
     * @return 请求结果类型，{@link Type}接口实例
     */
    public Type getResultType() {
        Type type = getReturnType();
        if (type == null) {
            return Void.class;
        }
        Class clazz = ReflectUtils.getClassByType(type);
        if (ForestResponse.class.isAssignableFrom(clazz)) {
            if (type instanceof ParameterizedType) {
                Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                if (types.length > 0) {
                    return types[0];
                }
            }
        }
        return type;
    }
}
