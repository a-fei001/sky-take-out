package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获用户名重复异常.
     * @param ex
     * @return
     */
    //错误信息如下：
    //java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'tomorin' for key 'employee.idx_username'
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        //包含这个 说明包含了错误信息
        if (message.contains("Duplicate entry")){
            String[] strs = message.split(" ");
            String username = strs[2];
            return Result.error(username+MessageConstant.ALREADY_EXISTS);
        } else {
            //没有包含这个错误信息 报：未知错误
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
