package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
* 这段代码的作用是将一个AliOSSUtils对象里面封装好AliOSSProperties里面引进的数据
* 将这个对象注入到IOC容器中
* 采用配置类@Bean的注入方式
* */

@Slf4j
@Configuration
public class AliOSSConfiguration {
    @Bean
    //这里需要注意，@Bean修饰的方法，传参数 eg:AliOssProperties aliOssProperties
    //spring会直接注入进来aliOssProperties对象
    //也就是说默认执行了@Autowired注入aliOssProperties的操作
    @ConditionalOnMissingBean//检查容器中是否已经有这个类型的Bean对象，没有才会创建
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云文件上传对象:{}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint()
        ,aliOssProperties.getAccessKeyId()
        ,aliOssProperties.getAccessKeySecret()
        ,aliOssProperties.getBucketName());
    }
}
