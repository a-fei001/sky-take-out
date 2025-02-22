package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {
    @Autowired
    OrderMapper orderMapper;

    /**
     * 每一分钟执行一次
     * 将超时15分钟未支付的订单取消
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeOutOrder(){
        log.info("订单超时：{}", LocalDateTime.now());
        Integer status = Orders.PENDING_PAYMENT;
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> list = orderMapper.getByStatusAndOderTime(status,time);
        if(list != null && !list.isEmpty()){
            for(Orders order : list){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时~");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 每天晚上1点执行一次
     * 将昨天还在 派送中 改为 完成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理派送中的订单：{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        Integer status = Orders.DELIVERY_IN_PROGRESS;
        List<Orders> list = orderMapper.getByStatusAndOderTime(status,time);
        if(list != null && !list.isEmpty()){
            for(Orders order : list){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}












