// com/tennis_court_booking/pojo/vo/RegisterVO.java
package com.tennis_court_booking.pojo.vo;

import lombok.Data;

@Data
public class RegisterVO {
    private String username;
    private String password;
    private String confirmPassword;
    private String phone;
    private String email;
    private String realName;
}