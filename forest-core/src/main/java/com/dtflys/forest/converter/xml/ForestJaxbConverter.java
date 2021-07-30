package com.dtflys.forest.converter.xml;

import com.dtflys.forest.exceptions.ForestConvertException;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.utils.ForestDataType;
import com.dtflys.forest.utils.ReflectUtils;
import com.dtflys.forest.utils.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 基于JAXB实现的XML转换器
 * @author gongjun
 * @since 2016-07-12
 */
public class ForestJaxbConverter implements ForestXmlConverter {

    @Override
    public String encodeToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof CharSequence) {
            return obj.toString();
        }
        if (obj instanceof Map || obj instanceof List) {
            throw new ForestRuntimeException("[Forest] JAXB XML converter dose not support translating instance of java.util.Map or java.util.List");
        }
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
            StringWriter writer = new StringWriter();
            createMarshaller(jaxbContext, "UTF-8").marshal(obj, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new ForestConvertException("xml", e);
        }

    }

    @Override
    public <T> T convertToJavaObject(String source, Class<T> targetType) {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(targetType);
            StringReader reader = new StringReader(source);
            return (T) createUnmarshaller(jaxbContext).unmarshal(reader);
        } catch (JAXBException e) {
            throw new ForestConvertException("xml", e);
        }

    }


    @Override
    public <T> T convertToJavaObject(String source, Type targetType) {
        Class clazz = ReflectUtils.getClassByType(targetType);
        return (T) convertToJavaObject(source, clazz);
    }


    public Marshaller createMarshaller(JAXBContext jaxbContext, String encoding) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            if (StringUtils.isNotEmpty(encoding)) {
                marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            }
            return marshaller;
        } catch (JAXBException e) {
            throw new ForestRuntimeException(e);
        }
    }

    public Unmarshaller createUnmarshaller(JAXBContext jaxbContext) {
        try {
            return jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ForestDataType getDataType() {
        return ForestDataType.XML;
    }

}
