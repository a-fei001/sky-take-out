package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Override
    //方法涉及到两个数据库的操作，开启事务（使用这个注解需要在启动类上加上@EnableTransactionManagement）
    @Transactional(rollbackFor = Exception.class)
    public void saveWithFlavor(DishDTO dishDTO) {
        //1.插入菜品数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.save(dish);

        //2.插入菜品口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //用户可能没有添加口味
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor f : flavors) {
                //经过DishMapper.xml中的<insert>上的配置 才能传来id值
                f.setDishId(dish.getId());
            }
            dishFlavorMapper.saveBatch(flavors);
        }
    }
}

















