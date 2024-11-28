package com.sky.service;


import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

public interface CategoryService {

    /**
     * 修改分类.
     *
     * @param categoryDTO
     */
    void updateCategory(CategoryDTO categoryDTO);

    /**
     * 分类分页查询.
     *
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 启用/禁用 分类
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status,Long id);

    /**
     * 新增分类.
     *
     * @param categoryDTO
     */
    void save(CategoryDTO categoryDTO);

    /**
     * 根据id删除分类.
     *
     * @param id
     */
    void delete(Long id);

    /**
     * 根据类型查询分类.
     *
     * @param type
     * @return
     */
    List<Category> selectByType(Integer type);
}
