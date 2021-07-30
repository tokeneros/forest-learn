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

package com.dtflys.forest.annotation;

import com.dtflys.forest.lifecycles.parameter.VariableLifeCycle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 变量定义注解
 * 不再建议使用此注解，变量定义请使用 <code>com.dtflys.forest.annotation.Var</code>
 * <p>该注解只能修饰方法参数，被修饰的参数表示为一个可被模板字符串引用的变量，不会直接通过请求传输到服务端。</p>
 * 通过该注解定义变量的方式如下：<br><br>
 *
 *     <pre class="code">
 *         &#064;Get(url = "http://localhost:8080/user/${userId}")
 *         UserInfo getUser(@DataVariable("userId") String id);
 *     </pre>
 *
 *  <p>如上所示，id参数会先被定义成一个名为"userId"的变量，然后通过@Get注解的url属性中的 "...${userId}" 部分所引用，
 *  如果此方法被这样调用 getUser("U001") , 那么最终产生的url为 http://localhost:8080/user/U001<br>
 *  因为此时传入参数id为字符串"U001"，即给变量"userId"同样赋值为"U001"，最后url中的"${userId}"部分引用了该变量</p>
 *
 * @author gongjun
 * @since 2016-05-24
 */
@Documented
@ParamLifeCycle(VariableLifeCycle.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface DataVariable {

    /**
     * The name of the variable. The variable name can be referenced in some places (ex. request URL, Body, Headers)
     */
    String value() default "";

    /**
     * The filters will do some processing for the variable value before sending request.
     */
    String filter() default "";
}
