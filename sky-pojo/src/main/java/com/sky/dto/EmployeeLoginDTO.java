package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "员工登录时传递的数据模型")//knife4j的注解 用来在接口文档中显示--DTO/VO/entity
public class EmployeeLoginDTO implements Serializable {

    @ApiModelProperty("用户名")//knife4j的注解 用来在接口文档中显示--方法
    private String username;

    @ApiModelProperty("密码")
    private String password;

}
