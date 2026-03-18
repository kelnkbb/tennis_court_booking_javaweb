package com.tennis_court_booking.pojo.vo;

import lombok.Data;

@Data
public class ChangePasswordVO {
    private String oldPassword;
    private String newPassword;
}