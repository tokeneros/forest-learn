package com.dtflys.forest.schema;

import com.dtflys.forest.logging.ForestLogHandler;
import com.dtflys.forest.ssl.SpringSSLKeyStore;
import com.dtflys.forest.utils.ClientFactoryBeanUtils;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.ssl.SSLKeyStore;
import com.dtflys.forest.utils.ForestDataType;
import com.dtflys.forest.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-04-21 14:49
 */
public class ForestConfigurationBeanDefinitionParser implements BeanDefinitionParser {
    private static Logger log = LoggerFactory.getLogger(ForestConfigurationBeanDefinitionParser.class);

    private final static Class configurationBeanClass = ForestConfiguration.class;
    private final static Class sslKeyStoreBeanClass = SpringSSLKeyStore.class;


    public ForestConfigurationBeanDefinitionParser() {
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();

        beanDefinition.setBeanClass(configurationBeanClass);
        beanDefinition.setLazyInit(false);
        beanDefinition.setFactoryMethodName("configuration");
        String id = element.getAttribute("id");
        id = ClientFactoryBeanUtils.getBeanId(id, configurationBeanClass, parserContext);
        if (id != null && id.length() > 0) {
            if (parserContext.getRegistry().containsBeanDefinition(id))  {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
        }

        Method[] methods = configurationBeanClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 0 || paramTypes.length > 1) {
                continue;
            }
            Class paramType = paramTypes[0];
            if (Collections.class.isAssignableFrom(paramType)
                    || Map.class.isAssignableFrom(paramType)) {
                continue;
            }
            if (methodName.length() >= 3 && methodName.startsWith("set")) {
                String attributeName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                String attributeValue = element.getAttribute(attributeName);
                if (StringUtils.isNotEmpty(attributeValue)) {
                    if ("backend".equals(attributeName)) {
                        beanDefinition.getPropertyValues().addPropertyValue("backendName", attributeValue);
                    }
                    else if ("logHandler".equals(attributeName)) {
                        try {
                            Class clazz = Class.forName(attributeValue);
                            if (!ForestLogHandler.class.isAssignableFrom(clazz)) {
                                throw new ForestRuntimeException("property 'logHandler' must be a class extending from com.dtflys.forest.logging.ForestLogHandler");
                            }
                            ForestLogHandler handler = (ForestLogHandler) clazz.newInstance();
                            beanDefinition.getPropertyValues().addPropertyValue("logHandler", handler);
                        } catch (ClassNotFoundException e) {
                            throw new ForestRuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new ForestRuntimeException(e);
                        } catch (InstantiationException e) {
                            throw new ForestRuntimeException(e);
                        }
                    }
                    else {
                        beanDefinition.getPropertyValues().addPropertyValue(attributeName, attributeValue);
                    }
                }
            }
        }
        parseChildren(element.getChildNodes(), beanDefinition);
        log.info("[Forest] Created Forest Configuration Bean: " + beanDefinition);
        return beanDefinition;

    }


    public void parseChildren(NodeList nodeList, RootBeanDefinition beanDefinition) {
        int nodesLength = nodeList.getLength();
        if (nodesLength > 0) {
            ManagedMap<String, Object> varMap = new ManagedMap<String, Object>();
            ManagedMap<String, BeanDefinition> sslKeyStoreMap = new ManagedMap<>();
            ManagedMap<ForestDataType, BeanDefinition> converterMap = new ManagedMap<>();
            for (int i = 0; i < nodesLength; i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element elem = (Element) node;
                    String elemName = elem.getLocalName();
                    if (elemName.equals("var")) {
                        parseVariable(elem, varMap);
                    } else if (elemName.equals("ssl-keystore")) {
                        parseSSLKeyStore(elem, sslKeyStoreMap);
                    } else if (elemName.equals("converter")) {
                        parseConverter(elem, converterMap);
                    }
                }
            }
            beanDefinition.getPropertyValues().addPropertyValue("variables", varMap);
            beanDefinition.getPropertyValues().addPropertyValue("sslKeyStores", sslKeyStoreMap);
            beanDefinition.getPropertyValues().addPropertyValue("converterMap", converterMap);
        }
    }


    public void parseVariable(Element elem, ManagedMap<String, Object> varMap) {
        String name = elem.getAttribute("name");
        String value = elem.getAttribute("value");
        varMap.put(name, value);
    }

    public void parseSSLKeyStore(Element elem, ManagedMap<String, BeanDefinition> sslKeyStoreMap) {
        String id = elem.getAttribute("id");
        String filePath = elem.getAttribute("file");
        String keystoreType = elem.getAttribute("type");
        String keystorePass = elem.getAttribute("keystorePass");
        String certPass = elem.getAttribute("certPass");
        String protocolsStr = elem.getAttribute("protocols");
        String cipherSuitesStr = elem.getAttribute("cipher-suites");
        String sslSocketFactoryBuilder = elem.getAttribute("sslSocketFactoryBuilder");

        if (StringUtils.isEmpty(keystoreType)) {
            keystoreType = SSLKeyStore.DEFAULT_KEYSTORE_TYPE;
        }
        if (StringUtils.isEmpty(filePath)) {
            throw new ForestRuntimeException(
                    "The file of SSL KeyStore \"" + id + "\" is empty!");
        }
        BeanDefinition beanDefinition = createSSLKeyStoreBean(
                id, keystoreType, filePath, keystorePass, certPass, protocolsStr, cipherSuitesStr, sslSocketFactoryBuilder);
        sslKeyStoreMap.put(id, beanDefinition);
    }




    public static BeanDefinition createSSLKeyStoreBean(String id,
                                                       String keystoreType,
                                                       String filePath,
                                                       String keystorePass,
                                                       String certPass,
                                                       String protocolsStr,
                                                       String cipherSuitesStr,
                                                       String sslSocketFactoryBuilder) {
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(sslKeyStoreBeanClass.getName());
        ConstructorArgumentValues beanDefValues = beanDefinition.getConstructorArgumentValues();
        beanDefValues.addGenericArgumentValue(id);
        beanDefValues.addGenericArgumentValue(keystoreType);
        beanDefValues.addGenericArgumentValue(filePath);
        beanDefValues.addGenericArgumentValue(keystorePass);
        beanDefValues.addGenericArgumentValue(certPass);
        beanDefValues.addGenericArgumentValue(sslSocketFactoryBuilder);
        if (StringUtils.isNotEmpty(protocolsStr)) {
            String[] strs = protocolsStr.split("[ /t]*,[ /t]*");
            String[] protocols = new String[strs.length];
            for (int i = 0; i < strs.length; i++) {
                protocols[i] = strs[i].trim();
            }
            beanDefinition.getPropertyValues().add("protocols", protocols);
        }
        if (StringUtils.isNotEmpty(cipherSuitesStr)) {
            String[] strs = cipherSuitesStr.split("[ /t]*,[ /t]*");
            String[] cipherSuites = new String[strs.length];
            for (int i = 0; i < strs.length; i++) {
                cipherSuites[i] = strs[i].trim();
            }
            beanDefinition.getPropertyValues().add("cipherSuites", cipherSuites);
        }
        return beanDefinition;
    }

    private static void parseConverter(Element elem, ManagedMap<ForestDataType, BeanDefinition> converterMap) {
        String dataTypeName = elem.getAttribute("dataType");
        ForestDataType dataType = ForestDataType.findOrCreateDataType(dataTypeName);
        if (dataType == null) {
            throw new ForestRuntimeException("Cannot find data type named '" + dataTypeName + "'");
        }
        String className = elem.getAttribute("class");
        BeanDefinition definition = createConverterBean(className);
        NodeList nodeList = elem.getChildNodes();
        int nodeLength = nodeList.getLength();
        if (nodeLength > 0) {
            for (int i = 0; i < nodeLength; i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element childElem = (Element) node;
                    String childElemName = childElem.getLocalName();
                    if (childElemName.equals("parameter")) {
                        String paramName = childElem.getAttribute("name");
                        String paramValue = childElem.getAttribute("value");
                        definition.getPropertyValues().addPropertyValue(paramName, paramValue);
                    }
                }
            }
        }
        converterMap.put(dataType, definition);
    }

    public static BeanDefinition createConverterBean(String className) {
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(className);
        return beanDefinition;
    }
}
