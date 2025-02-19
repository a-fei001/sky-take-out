package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@Slf4j
@Api(tags =  "店铺相关接口")
@RequestMapping("/user/shop")
public class ShopController {
    public final static String KEY = "SHOP_STATUS";

    //在RedisConfiguration中注入 用来控制Redis中的数据
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result getStatus(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Integer shopStatus = (Integer) valueOperations.get(KEY);
        log.info("获取到店铺营业状态为:{}",shopStatus == 1 ? "营业中" : "打样中");
        return Result.success(shopStatus);
    }
}
