package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//本注解的作用是在SpringAOP切面中通过自定义注解标注 实现切面表达式(@annotation(……))
//(这个切面功能是update insert方法中统一进行updateTime createTime...属性的赋值 减少代码的重复)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //OperationType是枚举类
    //这么写 加注释时实现 @AutoFill(value = OperationType.UPDATE)
    //实现区分update和insert方法的作用
    OperationType value();
}
