package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关接口")//knife4j的注解 用来在接口文档中显示
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @ApiOperation(value = "员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        //注意这里 JWT密钥的payout部分加入了员工的Id
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @ApiOperation(value = "员工退出")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 新增员工.
     *
     * @param employeeDTO
     * @return
     */
    @ApiOperation("新增员工")
    @PostMapping
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("save employeeDTO: {}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }

    /**
     * 员工分页查询.
     *
     * @param employeePageQueryDTO
     * @return
     */
    @ApiOperation("员工分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO) {
        log.info("pageQuery employeePageQueryDTO: {}", employeePageQueryDTO);
        //PageResult封装成了符合需求文档返回参数的样子(total+employee[])
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 启用/禁用 员工账号.
     *
     * @param status
     * @param id
     * @return
     */
    @ApiOperation("启用/禁用 员工账号")
    @PostMapping("/status/{status}")//注意这里的路径不能写错
    //这里直接用Result 没有加类型是因为这里data为空 传给Result的类型是规定data的类型的
    //data为空 传不传都可以
    public Result startOrStop(@PathVariable Integer status,Long id) {//spring内置自动类型转换
        log.info("startOrStop id: {} status: {}", id, status);
        employeeService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据id查询员工信息
     *
     * @param id
     * @return
     */
    @ApiOperation("根据id查询员工信息")
    @GetMapping("/{id}")
    public Result<Employee> selectById(@PathVariable Long id) {
        log.info("select id: {}", id);
        Employee employee = employeeService.selectById(id);
        return Result.success(employee);
    }

    /**
     * 编辑员工信息
     *
     * @param employeeDTO
     * @return
     */
    @ApiOperation("编辑员工信息")
    @PutMapping
    public Result update(@RequestBody EmployeeDTO employeeDTO) {
        log.info("update employeeDTO: {}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }

}
