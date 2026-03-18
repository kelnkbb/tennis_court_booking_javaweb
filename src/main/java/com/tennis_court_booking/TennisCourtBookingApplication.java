package com.tennis_court_booking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tennis_court_booking.mapper")
public class TennisCourtBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TennisCourtBookingApplication.class, args);
    }
}