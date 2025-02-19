package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    ShoppingCartMapper shoppingCartMapper;
    @Autowired
    DishMapper dishMapper;
    @Autowired
    SetmealMapper setmealMapper;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        //查询看数据库中是否已经有了这个套餐/菜品
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //有->更新数量+1
        if (list != null && list.size() > 0) {
            ShoppingCart shoppingCart1 = list.get(0);
            shoppingCart1.setNumber(shoppingCart1.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart1);
            return;
        }
        //没有->判断套餐/菜品->插入数据
        if(shoppingCart.getDishId() != null){
            DishVO dishVO = dishMapper.selectById(shoppingCart.getDishId());
            shoppingCart.setName(dishVO.getName());
            shoppingCart.setImage(dishVO.getImage());
            shoppingCart.setAmount(dishVO.getPrice());
        } else {
            Long setmealId = shoppingCart.getSetmealId();
            SetmealVO setmealVO = setmealMapper.selectSetmealById(setmealId);
            shoppingCart.setName(setmealVO.getName());
            shoppingCart.setImage(setmealVO.getImage());
            shoppingCart.setAmount(setmealVO.getPrice());
        }
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCart.setNumber(1);
        shoppingCartMapper.insert(shoppingCart);
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .build();
        return shoppingCartMapper.list(shoppingCart);
    }

    @Override
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    @Override
    public void cleanOne(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        //查找是否有这个商品
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) {
            ShoppingCart shoppingCart1 = list.get(0);
            Integer number = shoppingCart1.getNumber();
            if (number == 1) {
                shoppingCartMapper.deleteById(shoppingCart1.getId());
            } else{
                shoppingCart1.setNumber(shoppingCart1.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart1);
            }
        }
        //这里因为是前端传来的购物车商品数据 基本不存在查不到的情况 所以默认一定有商品
    }
}
