package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询同一个用户历史订单信息
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQueryOrdersByUserId(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单信息
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.orders where id = #{id}")
    Orders getBtId(Long id);

    /**
     * 查询所有订单信息
     * @return
     */
    @Select("select * from sky_take_out.orders")
    List<Orders> selectAll();

    /**
     * 根据状态查询在...时间前的订单
     * @param status
     * @param time
     * @return
     */
    @Select("select * from sky_take_out.orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOderTime(Integer status, LocalDateTime time);
}
