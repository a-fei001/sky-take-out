package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper {

    void updateCategory(Category category);

    Page<Category> pageQuery(String name, Integer type);
}
