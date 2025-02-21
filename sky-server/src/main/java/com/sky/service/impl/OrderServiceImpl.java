package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1.异常判断（虽然前端大概率判断过，但是这里重写一遍来防止绕过前端直接发送的恶意请求）
        //- 地址栏为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //- 购物车为空
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //2.orders表的插入操作
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orderMapper.insert(orders);
        //3.order_detail表的插入操作
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        //4.清空用户购物车
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
        //5.返回OrderSubmitVO数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult selectHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),
                ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQueryOrdersByUserId(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        if(orders != null && !orders.isEmpty()){
            for (Orders order : orders) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                Long orderId = order.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getBatchByOrderId(orderId);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(orders.getTotal(),list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO select(Long id) {
        Orders orders = orderMapper.getBtId(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getBatchByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     * @throws Exception
     */
    @Override
    public void cancelById(Long id) throws Exception {
        Orders order = orderMapper.getBtId(id);
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(order.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders newOrder = new Orders();
        BeanUtils.copyProperties(order, newOrder);
        if(order.getPayStatus().equals(Orders.TO_BE_CONFIRMED)){
            weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal((0.01)));
            newOrder.setPayStatus(Orders.REFUND);
        }
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setCancelTime(LocalDateTime.now());
        newOrder.setCancelReason("用户取消");
        orderMapper.update(newOrder);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //查询订单详细信息
        List<OrderDetail> orderDetails = orderDetailMapper.getBatchByOrderId(id);
        //将订单详细信息解析并添加进购物车
        List<ShoppingCart> list = orderDetails.stream().map(orderDetail -> {
            //shoppingCart对象必须要创建在map内部
            //否则生成List<ShoppingCart>数组添加的全是List<OrderDetails>最后一个元素的数据
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(list);
    }

    /**
     * 管理端订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult adminSelect(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQueryOrdersByUserId(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        for (Orders order : page) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);

            List<OrderDetail> orderDetails = orderDetailMapper.getBatchByOrderId(order.getId());
            StringBuilder orderDishes = new StringBuilder();
            for (OrderDetail orderDetail : orderDetails) {
                if(orderDetail.getDishId() != null){
                    Long dishId = orderDetail.getDishId();
                    Integer number = orderDetail.getNumber();
                    DishVO dishVO = dishMapper.selectById(dishId);
                    String name = dishVO.getName();
                    orderDishes.append(name).append("* ").append(number).append("; ");
                }
                if(orderDetail.getSetmealId() != null){
                    Long setmealId = orderDetail.getSetmealId();
                    Integer number = orderDetail.getNumber();
                    SetmealVO setmealVO = setmealMapper.selectSetmealById(setmealId);
                    String name = setmealVO.getName();
                    orderDishes.append(name).append("* ").append(number).append("; ");
                }
            }
            String od = orderDishes.toString();
            orderVO.setOrderDishes(od);
            list.add(orderVO);
        }
        return new PageResult(page.getTotal(),list);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO adminStatistics() {
        // 查询所有订单
        List<Orders> list = orderMapper.selectAll();
        // 初始化统计变量
        Integer toBeConfirmed = 0;
        Integer confirmed = 0;
        Integer deliveryInProgress = 0;
        // 遍历订单并统计
        for (Orders order : list) {
            if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
                toBeConfirmed++;
            } else if (order.getStatus().equals(Orders.CONFIRMED)) {
                confirmed++;
            } else if (order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
                deliveryInProgress++;
            }
        }
        // 创建 OrderStatisticsVO 对象并设置统计结果
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param orders
     */
    @Override
    public void adminConfirm(Orders orders) {
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param orders
     * @throws Exception
     */
    @Override
    public void adminRejection(Orders orders) throws Exception {
        Long id = orders.getId();
        Orders order = orderMapper.getBtId(id);
        if(order.getPayStatus().equals(Orders.PAID)){
            String refund = weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal((0.01)));
            log.info("申请退款:{}",refund);
            order.setPayStatus(Orders.REFUND);
        }
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(orders.getCancelReason());
        orderMapper.update(order);
    }


}





























