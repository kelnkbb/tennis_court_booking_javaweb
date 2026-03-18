// com/tennis_court_booking/pojo/vo/LoginResponseVO.java
package com.tennis_court_booking.pojo.vo;

import com.tennis_court_booking.pojo.entity.User;
import lombok.Data;

@Data
public class LoginResponseVO {
    private String token;
    private User user;
    private long expiresIn;
}