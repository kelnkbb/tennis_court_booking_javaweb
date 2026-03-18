package com.tennis_court_booking;

import com.tennis_court_booking.mapper.UserMapper;
import com.tennis_court_booking.pojo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;

@SpringBootTest
public class MapperTest {

    @Autowired
    private UserMapper userMapper;


}