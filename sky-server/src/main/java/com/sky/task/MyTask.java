package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class MyTask {
//    //展示SpringTask最基本用法
//    @Scheduled(cron = "*/10 * * * * *")
//    public void executeTask(){
//        log.info("定时任务开始执行：{}",new Date());
//    }
}
