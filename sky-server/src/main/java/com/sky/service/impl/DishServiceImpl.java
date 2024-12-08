package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    private static final Logger log = LoggerFactory.getLogger(DishServiceImpl.class);
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

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        //这里DishVO的属性和sql查询的属性不完全一致 二者存在交集
        //mybatis底层自动将交集部分数据接收完成，sql多出来的属性无法被接收，DishVO多出来的属性没有数据接收，值为null
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }


}

















