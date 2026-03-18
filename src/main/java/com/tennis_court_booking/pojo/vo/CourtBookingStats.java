// com/tennis_court_booking/pojo/vo/CourtBookingStats.java
package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtBookingStats {
    private Integer courtId;
    private String courtName;
    private Integer bookingCount;
    private Integer totalHours;
    private BigDecimal totalAmount;
}