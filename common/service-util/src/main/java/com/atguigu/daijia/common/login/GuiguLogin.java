package com.atguigu.daijia.common.login;


//登录校验的自定义注解

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//这个注解用在方法上 后面作为用AOP的基准点
@Target(ElementType.METHOD)
//作用范围 RUNTIME
@Retention(RetentionPolicy.RUNTIME)
public @interface GuiguLogin {
}
