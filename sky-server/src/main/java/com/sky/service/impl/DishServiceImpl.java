package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    private static final Logger log = LoggerFactory.getLogger(DishServiceImpl.class);
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        //1.status状态正在出售不能删
        List<Dish> dishs = dishMapper.selectBatchById(ids);
        for (Dish dish : dishs) {
            if(dish.getStatus().equals(StatusConstant.ENABLE)){
                //抛出自定义异常 无法删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //2.setmeal_dish关联不能删
        Integer num = setmealDishMapper.selectCountBatchByDishId(ids);
        if(num != null && num > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //3.删除dish表的数据
        dishMapper.deleteBatch(ids);

        //4.删除dish_flavor中关联的数据
        dishFlavorMapper.deleteBatchByDishId(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishVO selectById(Long id) {
        DishVO dishVO = dishMapper.selectById(id);
//        这里查询这个是因为DishVO里面包含这个属性 但是这个接口虽然使用DishVO传输数据
//        却并不需要这个属性 所以这个是多余的
//        //根据categoryId查询categoryName
//        Long categoryId = dishVO.getCategoryId();
//        dishVO.setCategoryName(categoryMapper.selectNameById(categoryId));
        //根据id(dishId)查询flavors集合
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectByDIshId(id);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    public void updateStatus(Integer status, Integer id) {
        LocalDateTime updateTime = LocalDateTime.now();
        Long updateUser = BaseContext.getCurrentId();
        dishMapper.updateStatus(status,id,updateUser,updateTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DishDTO dishDTO) {
        //修改dish
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //修改flavor-->删除原有的-->插入新的
        //删掉原有的--调用以前写过的deleteBatchByDishId方法
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(dish.getId());
        dishFlavorMapper.deleteBatchByDishId(ids);
        //插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //直接复制粘贴新增菜品位置的代码即可
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor f : flavors) {
                //经过DishMapper.xml中的<insert>上的配置 才能传来id值
                f.setDishId(dish.getId());
            }
            dishFlavorMapper.saveBatch(flavors);
        }
    }
}

















