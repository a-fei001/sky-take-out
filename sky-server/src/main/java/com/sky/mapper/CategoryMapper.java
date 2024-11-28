package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    void updateCategory(Category category);

    Page<Category> pageQuery(String name, Integer type);

    @Insert("INSERT INTO sky_take_out.category (type, name, sort, status, create_time, update_time, create_user, update_user) " +
            "VALUES (#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void save(Category category);

    @Delete("DELETE FROM sky_take_out.category WHERE id = #{id}")
    void delete(Long id);

    @Select("select * from sky_take_out.category where type = #{type}")
    List<Category> selectByType(Integer type);
}
