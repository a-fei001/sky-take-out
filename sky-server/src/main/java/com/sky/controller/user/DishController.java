package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("根据分类id{}查询菜品", categoryId);

        //redis中查询 判断redis缓存中是否已经存在数据
        String key = "dish_"+categoryId;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        List<DishVO> dishVOS = (List<DishVO>) valueOperations.get(key);
        if (dishVOS != null && dishVOS.size() > 0) {
            log.info("返回redis中的数据");
            //存在->直接返回redis中的数据
            return Result.success(dishVOS);
        }

        //不存在->数据库中查询
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        List<DishVO> list = dishService.listWithFlavor(dish);
        log.info("返回mysql中的数据");

        //数据库中查询完成，在redis中添加上数据
        redisTemplate.opsForValue().set(key,list);
        return Result.success(list);
    }

}
