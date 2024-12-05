package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component//注入SpringBoot容器 使得该切面合法可被检测到
@Slf4j
public class AutoFillAspect {
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcut(){}

    @Before("autoFillPointcut()")
    public void autoFillBefore(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("autoFillBefore方法执行了~~~~");
        //1.判断insert or update
        //这里是采用反射的方式获取到了被@AutoFill标记的方法上value的值
        //这里采用MethodSignature而不是Signature接收 是因为MethodSignature可以明确获取到注释上的value
        //(Signature在上按ctrl+alt+b获取到子接口--MethodSignature extends Signature)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        //2.获取到被拦截方法的参数
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            log.info("AutoFillAspect拦截到的方法参数为空");
            return;
        }
        //这里取args[0]是因为约定我们需要获取的对象下标为0
        //使用Object接收是因为不同mapper类的不同方法 参数可能不同
        Object entity = args[0];

        //3.准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        //这个BaseContext里面的数据是在拦截器校验jwt时，解析出员工id后存入的
        Long currentId = BaseContext.getCurrentId();

        //4.根据不同的注解进行赋值
        if(operationType == OperationType.INSERT){
            //这里为什么不直接使用entity.setCreateTime() 而是使用反射 获取到setCreateTime方法再使用
            //是因为java是强类型语言，方法运行前不知道是什么类的对象，不能直接使用setCreateTime
            //需要采用这样反射的方式获取
            Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
            Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
            Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
            setCreateTime.invoke(entity,now);
            setCreateUser.invoke(entity,currentId);
            setUpdateTime.invoke(entity,now);
            setUpdateUser.invoke(entity,currentId);
        } else if(operationType == OperationType.UPDATE){
            Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
            setUpdateTime.invoke(entity,now);
            setUpdateUser.invoke(entity,currentId);
        }

    }
}



























