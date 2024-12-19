package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */

    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    @AutoFill(value = OperationType.INSERT)
    void save(Dish dish);

    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    List<Dish> selectBatchById(List<Long> ids);

    void deleteBatch(List<Long> ids);

    @Select("select * from dish where id = #{id}")
    DishVO selectById(Long id);

    //@AutoFill(OperationType.UPDATE)
    @Update("update dish set status = #{status},update_user = #{updateUser},update_time = #{updateTime} where id = #{id}")
    void updateStatus(Integer status, Integer id, Long updateUser, LocalDateTime updateTime);

    //你是不是应该用Dish封装一下？
    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);

    @Select("select * from sky_take_out.dish where category_id = #{categoryId}")
    List<Dish> selectByCategoryId(Long categoryId);
}
