package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO selectSetmealById(Long id);

    void insert(SetmealDTO setmealDTO);

    void deleteBatch(List<Long> ids);

    void update(SetmealDTO setmealDTO);
}
