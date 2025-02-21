package com.sky.controller.admin;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "订单管理接口")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> adminSelect(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.adminSelect(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> adminStatistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.adminStatistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> adminDetail(@PathVariable Long id) {
        OrderVO select = orderService.select(id);
        return Result.success(select);
    }

    /**
     * 接单
     * @param orders
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result adminConfirm(@RequestBody Orders orders) {
        orderService.adminConfirm(orders);
        return Result.success();
    }

    /**
     * 拒单
     * @param orders
     * @return
     * @throws Exception
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result adminRejection(@RequestBody Orders orders) throws Exception {
        orderService.adminRejection(orders);
        return Result.success();
    }

    /**
     * 取消订单
     * @param orders
     * @return
     * @throws Exception
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result adminCancel(@RequestBody Orders orders) throws Exception {
        orderService.adminCancel(orders);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result adminDelivery(@PathVariable Long id) {
        orderService.adminDelivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result adminComplete(@PathVariable Long id) {
        orderService.adminComplete(id);
        return Result.success();
    }
}
