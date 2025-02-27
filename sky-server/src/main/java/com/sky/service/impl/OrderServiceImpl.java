package com.sky.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.webSocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.*;
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
    @Autowired
    private WebSocketServer webSocketServer;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String AK;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) throws Exception {
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
        String userAddress = addressBook.getProvinceName()+addressBook.getCityName()
                +addressBook.getDistrictName()+addressBook.getDetail();
        //- 用户和商家的距离太远
        findDistanceException(addressBook,userAddress);

        //2.orders表的插入操作
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        //地址我这里拼接为：省+市+区/县+详细地址
        orders.setAddress(userAddress);
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
     * 判断商家和用户距离是不是太远
     * @param addressBook
     * @param userAddress
     * @throws Exception
     */
    public void findDistanceException(AddressBook addressBook,String userAddress) throws Exception {
        //- 距离判断-超过50公里无法加入订单
        String url = "https://api.map.baidu.com/geocoding/v3?";
        BaiduUtil baiduUtil = new BaiduUtil();
        //调用地址编码-获取包含shopAddress经纬度信息的json字符串
        Map params = new LinkedHashMap<String, String>();
        params.put("address", shopAddress);
        params.put("output", "json");
        params.put("ak", AK);
        String jsonShop = baiduUtil.requestGetAK(url, params);
        //调用地址编码-获取包含userAddress经纬度信息的json字符串
        params.put("address", userAddress);
        String jsonUser = baiduUtil.requestGetAK(url, params);
        //解析jsonUser
        JSONObject userJson = JSONObject.parseObject(jsonUser);
        if(!userJson.getString("status").equals("0")){
            throw new OrderBusinessException("用户地址解析失败");
        }
        JSONObject location = userJson.getJSONObject("result").getJSONObject("location");
        String userLatLng = location.getString("lat")+","+location.getString("lng");
        //解析jsonShop
        JSONObject shopJson = JSONObject.parseObject(jsonShop);
        if (!shopJson.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }
        JSONObject shopLocation = shopJson.getJSONObject("result").getJSONObject("location");
        String shopLatLng = shopLocation.getString("lat") +","+ shopLocation.getString("lng");
        log.info("商家纬度经度：{}", shopLatLng);
        log.info("用户纬度经度：{}", userLatLng);
        //计算用户和餐厅的距离
        String driveUrl = "https://api.map.baidu.com/directionlite/v1/driving?";
        Map map = new LinkedHashMap<String,String>();
        map.put("origin", shopLatLng);
        map.put("destination", userLatLng);
        map.put("ak",AK);
        String jsonDrive = baiduUtil.requestDriveGetAK(driveUrl, map);
        JSONObject driveJson = JSONObject.parseObject(jsonDrive);
        if(!driveJson.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }
        //从 JSON 对象中提取 result
        JSONObject result = driveJson.getJSONObject("result");
        //提取 routes 数组
        JSONArray routes = result.getJSONArray("routes");
        //假设我们只需要第一个 route 的距离
        if (routes.size() > 0) {
            JSONObject firstRoute = routes.getJSONObject(0);
            Integer distance = firstRoute.getInteger("distance"); // 使用 getInt 或 parse Integer
            log.info("距离为：{}",distance);
            if (distance > 50000) {
                throw new OrderBusinessException("超出配送范围");
            }
        } else {
            //处理没有 routes 的情况
            throw new OrderBusinessException("配送路线规划失败");
        }
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

        //订单提醒
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId",orders.getId());
        map.put("content",outTradeNo);
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
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
     * 拒单
     * @param orders
     * @throws Exception
     */
    @Override
    public void adminRejection(Orders orders) throws Exception {
        Long id = orders.getId();
        Orders order = orderMapper.getBtId(id);
        if(order == null || !order.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
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
        order.setRejectionReason(orders.getRejectionReason());
        orderMapper.update(order);
    }

    /**
     * 取消订单
     * @param orders
     */
    @Override
    public void adminCancel(Orders orders) throws Exception {
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

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void adminDelivery(Long id) {
        Orders order = orderMapper.getBtId(id);
        if(order==null || !order.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(order.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void adminComplete(Long id) {
        Orders order = orderMapper.getBtId(id);
        if(order==null || !order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order1 = new Orders();
        order1.setId(id);
        order1.setStatus(Orders.COMPLETED);
        order1.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(order1);
    }


    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders order = orderMapper.getBtId(id);
        if(order==null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //订单提醒
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId",order.getId());
        map.put("content",order.getNumber());
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }


}





























