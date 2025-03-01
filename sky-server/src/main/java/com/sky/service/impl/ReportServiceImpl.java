package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计接口
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        //获取begin-end之间的List集合dateList
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //将dateList拼接转换为','分割的字符串
        StringBuilder stringBuilder = new StringBuilder();
        for (LocalDate date : dateList) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(date.toString());
        }
        String dateListString = stringBuilder.toString();
        //计算每天营业额集合turnoverList
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = reportMapper.getDateSum(map);
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }
        //这个是org.apache.commons.lang3.StringUtils的依赖 可以实现和上面循环一样的拼接效果
        String turnoverListString = StringUtils.join(turnoverList, ",");
        return new TurnoverReportVO(dateListString, turnoverListString);
    }

    /**
     * 用户统计接口
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        //获取begin-end之间的List集合dateList
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            Integer totalNum = reportMapper.getUserCountByCreateTime(map);
            map.put("begin", beginTime);
            Integer newNum = reportMapper.getUserCountByCreateTime(map);
            newUserList.add(newNum);
            totalUserList.add(totalNum);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 订单统计接口
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = reportMapper.getOrderCount(beginTime,endTime,null);
            Integer validOrderCount = reportMapper.getOrderCount(beginTime,endTime,Orders.COMPLETED);
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }
        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);
        Double orderCompletionRate = 0.0;
        if(totalOrderCount > 0){
            orderCompletionRate = (double)validOrderCount / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .build();
    }

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<GoodsSalesDTO> list = reportMapper.getSalesTop10(beginTime,endTime,Orders.COMPLETED);
        List<String> nameList = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

    /**
     * 导出Excel报表接口
     * @param response
     */
    @Override
    public void exportBusinessResponse(HttpServletResponse response) {
        //1.查询30天的运营数据 补全表前半部分(今天结束时间往前共30天)
        LocalDate endDay = LocalDate.now();
        LocalDate beginDay = endDay.minusDays(29);
        LocalDateTime begin = LocalDateTime.of(beginDay, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(endDay, LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);
        //2.创建excel(使用resource/template下的模板excel)并将前半数据放在excel中
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue(begin+" "+end);
            XSSFRow row3 = sheet.getRow(3);
            row3.getCell(2).setCellValue(businessData.getTurnover());
            row3.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row3.getCell(6).setCellValue(businessData.getNewUsers());
            XSSFRow row4 = sheet.getRow(4);
            row4.getCell(2).setCellValue(businessData.getValidOrderCount());
            row4.getCell(4).setCellValue(businessData.getUnitPrice());
            //3.查询获取后半数据并插入excel中
            for(int i = 0;i < 30;i++){
                LocalDate day = beginDay.plusDays(i);
                LocalDateTime beginTime = LocalDateTime.of(day, LocalTime.MIN);
                LocalDateTime endTime = LocalDateTime.of(day, LocalTime.MAX);
                BusinessDataVO tbd = workspaceService.getBusinessData(beginTime, endTime);
                XSSFRow row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(day.toString());
                row.getCell(2).setCellValue(tbd.getTurnover());
                row.getCell(3).setCellValue(tbd.getValidOrderCount());
                row.getCell(4).setCellValue(tbd.getOrderCompletionRate());
                row.getCell(5).setCellValue(tbd.getUnitPrice());
                row.getCell(6).setCellValue(tbd.getNewUsers());

            }
            //4.将excel传送到浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //强制将缓冲区中的数据立即发送到客户端，而不是等待缓冲区满或流关闭时才发送
            out.flush();
            out.close();
            excel.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}

















