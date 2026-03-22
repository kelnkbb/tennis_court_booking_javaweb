// com/tennis_court_booking/pojo/vo/BookingVO.java
package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class BookingVO {

    private Integer id;
    private String bookingNo;
    private Integer courtId;
    private String courtName;
    private Integer userId;
    private String userName;
    private String userPhone;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal duration;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String contactName;
    private String contactPhone;
    private String remark;
    private Integer status;
    private String statusText;
    /** 0无 1待审取消 2驳回 */
    private Integer cancelRequestStatus;
    /** 0无 1待审付款 2驳回 */
    private Integer paymentVerifyStatus;
    private String paymentChannel;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}