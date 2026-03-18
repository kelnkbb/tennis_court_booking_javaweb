package com.tennis_court_booking.service;

import com.tennis_court_booking.pojo.entity.Court;
import java.util.List;

public interface CourtService {
    /**
     * 查询所有court
     */
    List<Court> findAll();

    /**
     * 根据ID删除court
     */
    void deleteById(Integer id);

    /**
     * 添加court
     */
    Court addCourt(Court court);

    /**
     * 根据ID查询court
     */
    Court getCourt(Integer id);

    /**
     * 更新court
     */
    Court updateCourt(Court court);
}