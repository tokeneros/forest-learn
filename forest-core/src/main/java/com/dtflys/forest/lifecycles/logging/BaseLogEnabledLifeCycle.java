package com.dtflys.forest.lifecycles.logging;

import com.dtflys.forest.annotation.LogEnabled;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.lifecycles.BaseAnnotationLifeCycle;
import com.dtflys.forest.lifecycles.MethodAnnotationLifeCycle;
import com.dtflys.forest.logging.LogConfiguration;
import com.dtflys.forest.proxy.InterfaceProxyHandler;
import com.dtflys.forest.reflection.ForestMethod;
import com.dtflys.forest.reflection.MetaRequest;

/**
 * 基本日志启用生命周期
 */
public class BaseLogEnabledLifeCycle implements BaseAnnotationLifeCycle<LogEnabled, Object> {

    /**
     * 基本日志启用生命周期 实例化
     *
     * @param interfaceProxyHandler 请求接口动态代理处理器
     * @param annotation            该生命周期类所绑定的注解
     */
    @Override
    public void onProxyHandlerInitialized(InterfaceProxyHandler interfaceProxyHandler, LogEnabled annotation) {
        // 获取基础日志配置
        LogConfiguration logConfiguration = interfaceProxyHandler.getBaseLogConfiguration();
        // 如果基础日志配置为空，新建一个基础日志配置
        if (logConfiguration == null) {
            logConfiguration = new LogConfiguration();
            // 设置基础日志配置相关信息，当作缓存，下次继续使用
            interfaceProxyHandler.setBaseLogConfiguration(logConfiguration);
        }
        // 是否打印请求/响应日志
        boolean logEnabled = annotation.value();
        // 是否打印请求日志
        boolean logRequest = annotation.logRequest();
        // 是否打印响应状态日志
        boolean logResponseStatus = annotation.logResponseStatus();
        // 是否打印响应内容日志
        boolean logResponseContent = annotation.logResponseContent();
        // 初始化基础日志配置信息，TODO 这里新建基础日志配置 没有看懂有什么其他含义
        logConfiguration.setLogEnabled(logEnabled);
        logConfiguration.setLogRequest(logRequest);
        logConfiguration.setLogResponseStatus(logResponseStatus);
        logConfiguration.setLogResponseContent(logResponseContent);
    }

}
