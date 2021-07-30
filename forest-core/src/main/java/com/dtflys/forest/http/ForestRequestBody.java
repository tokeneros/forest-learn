package com.dtflys.forest.http;

import com.dtflys.forest.http.body.NameValueRequestBody;
import com.dtflys.forest.http.body.ObjectRequestBody;
import com.dtflys.forest.http.body.StringRequestBody;

/**
 * Forest请求体
 * <p>该类为Forest请求中所有类型请求体项的父类</p>
 * <p>该类为抽象类，主要有以下3个子类: </p>
 * <p>    {@link StringRequestBody}    字符串请求体</p>
 * <p>    {@link ObjectRequestBody}    对象请求体</p>
 * <p>    {@link NameValueRequestBody} 键值对请求体</p>
 *
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2020-09-22 17:30
 */
public abstract class ForestRequestBody {

    /**
     * 请求体类型枚举类
     */
    public enum BodyType {

        /**
         * 字符串请求体类型
         */
        TEXT,

        /**
         * 键值对请求体类型
         */
        FORM_URL_ENCODED,

        /**
         * JSON格式
         */
        JSON,

        /**
         * XML格式
         */
        XML,

        /**
         * 文件请求体类型
         */
        BINARY,

        /**
         * 流请求体类型
         */
        INPUT_STREAM,

        /**
         * 文件类型
         */
        MULTIPART,
    }

    /**
     * 默认值
     */
    private String defaultValue;



    public String getDefaultValue() {
        return defaultValue;
    }

    public ForestRequestBody setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public abstract byte[] getByteArray();
}
