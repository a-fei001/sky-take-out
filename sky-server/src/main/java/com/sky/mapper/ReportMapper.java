package com.sky.mapper;

import com.sky.dto.GoodsSalesDTO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    /**
     * 查询订单时间在begin和end之间的已完成的订单总金额
     * @param map
     * @return
     */
    Double getDateSum(Map map);

    /**
     * 根据用户创建时间查询用户数量
     * @param map
     * @return
     */
    Integer getUserCountByCreateTime(Map map);

    /**
     * 根据时间和状态查找订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status);

    /**
     * 查询一段时间内销量排名top10
     * @param begin
     * @param end
     * @param status
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end, Integer status);
}





