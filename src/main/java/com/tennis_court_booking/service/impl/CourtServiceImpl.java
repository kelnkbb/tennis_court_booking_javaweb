package com.tennis_court_booking.service.impl;

import com.tennis_court_booking.mapper.CourtMapper;
import com.tennis_court_booking.pojo.entity.Court;
import com.tennis_court_booking.service.CourtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CourtServiceImpl implements CourtService {
    @Autowired
    private CourtMapper courtMapper;

    @Override
    public List<Court> findAll() {
        return courtMapper.findAll();
    }

    @Override
    public void deleteById(Integer id) {
        courtMapper.deleteById(id);
    }

    @Override
    public Court addCourt(Court court) {
        System.out.println("Service层接收到的court: " + court);

        // 如果状态没设置，默认设为1（营业中）
        if (court.getStatus() == null) {
            court.setStatus(1);
        }

        courtMapper.addCourt(court);
        System.out.println("插入成功，生成ID: " + court.getId());

        return court;
    }

    @Override
    public Court getCourt(Integer id) {
        return courtMapper.getCourt(id);
    }

    @Override
    public Court updateCourt(Court court) {
        System.out.println("Service层更新court: " + court);

        // 可以在这里添加更新时间字段（如果数据库表有update_time字段）
        // court.setUpdateTime(LocalDateTime.now());

        courtMapper.updateCourt(court);
        System.out.println("更新成功，ID: " + court.getId());

        return court;
    }
}