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
     * 根据关键词模糊筛选court（名称/地址）
     */
    List<Court> searchCourts(String keyword);

    /**
     * 更新court
     */
    Court updateCourt(Court court);

    /**
     * 按访问热度返回 TopN 场地（依赖缓存层维护的 Redis ZSet；不足时回退为全量列表前 N 条）。
     */
    List<Court> hotCourts(int topN);

    /**
     * 是否处于当前 Redis Set 维护的热度前列（与缓存层热门集合一致）。
     */
    boolean isHotCourt(Integer courtId);
}