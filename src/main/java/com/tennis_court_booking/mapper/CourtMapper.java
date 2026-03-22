package com.tennis_court_booking.mapper;

import com.tennis_court_booking.pojo.entity.Court;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CourtMapper {
    List<Court> findAll();
    void deleteById(Integer id);
    void addCourt(Court court);
    Court getCourt(Integer id);
    void updateCourt(Court court);  // 添加更新方法

    List<Court> searchCourts(String keyword);
}