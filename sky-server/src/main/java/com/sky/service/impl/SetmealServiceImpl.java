package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.IdsIsNullOrIsEmpty;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    private static final Logger log = LoggerFactory.getLogger(SetmealServiceImpl.class);
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public SetmealVO selectSetmealById(Long id) {
        SetmealVO setmealVO = setmealMapper.selectSetmealById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectBatchesBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(SetmealDTO setmealDTO) {
        //setmeal库的插入
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        //setmeal_dish库的插入
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        log.info("获取到套餐id:{}",setmeal.getId());
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmeal.getId());
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        if(ids == null || ids.isEmpty()){
            throw new IdsIsNullOrIsEmpty("请选择要删除的套餐");
        }
        //status为1 正在销售 无法删除
        for (Long id : ids) {
            SetmealVO setmealVO = setmealMapper.selectSetmealById(id);
            if (setmealVO.getStatus().equals(1)) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //删除套餐表中数据
        setmealMapper.deleteBatch(ids);
        //删除setmeal_dish表中数据
        setmealDishMapper.deleteBatchBySetmealId(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SetmealDTO setmealDTO) {
        //修改setmeal
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        log.info("修改的套餐的id:{}",setmeal.getId());

        //删除setmeal_dish中原有的数据
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(setmeal.getId());
        setmealDishMapper.deleteBatchBySetmealId(ids);

        //向set_meal中插入新的数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmeal.getId());
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }
}
