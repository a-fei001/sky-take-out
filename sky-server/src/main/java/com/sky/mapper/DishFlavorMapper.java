package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    void saveBatch(List<DishFlavor> dishFlavors);

    void deleteBatchByDishId(List<Long> dishIds);

    @Select("select * from sky_take_out.dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> selectByDIshId(Long dishId);

    void batchUpdate(List<DishFlavor> flavors);
}
