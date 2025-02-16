package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @AutoFill(value = OperationType.UPDATE)
    void updateCategory(Category category);

    Page<Category> pageQuery(String name, Integer type);

    @AutoFill(value = OperationType.INSERT)
    @Insert("INSERT INTO sky_take_out.category (type, name, sort, status, create_time, update_time, create_user, update_user) " +
            "VALUES (#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void save(Category category);

    @Delete("DELETE FROM sky_take_out.category WHERE id = #{id}")
    void delete(Long id);

    //修改成动态sql是因为要使用if标签来实现：不传递type的值时，查询所有分类
    //@Select("select * from sky_take_out.category where type = #{type}")
    List<Category> selectByType(Integer type);

//    修改菜品-根据id查询菜品-DishServiceImpl-selectById 使用到了 测试发现 这个其实是多余的
//    @Select(("select name from sky_take_out.category where id = #{categoryId}"))
//    String selectNameById(Long categoryId);
}
