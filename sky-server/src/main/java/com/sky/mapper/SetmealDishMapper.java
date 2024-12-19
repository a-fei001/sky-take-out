package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    Integer selectCountBatchByDishId(List<Long> dishIds);

    @Select("select * from sky_take_out.setmeal_dish where setmeal_id=#{setmealId}")
    List<SetmealDish> selectBatchesBySetmealId(Long setmealId);

    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBatchBySetmealId(List<Long> sIds);
}
