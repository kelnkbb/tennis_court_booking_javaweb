package com.tennis_court_booking.service.impl;

import com.tennis_court_booking.cache.CourtMultilevelCacheManager;
import com.tennis_court_booking.mapper.CourtMapper;
import com.tennis_court_booking.pojo.entity.Court;
import com.tennis_court_booking.service.CourtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CourtServiceImpl implements CourtService {
    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private CourtMultilevelCacheManager courtCache;

    @Override
    public List<Court> findAll() {
        return courtCache.findAllCourts(() -> courtMapper.findAll());
    }

    @Override
    public void deleteById(Integer id) {
        courtMapper.deleteById(id);
        courtCache.removeHeatAfterDelete(id);
        courtCache.evictAfterCourtMutation(id);
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

        courtCache.evictAfterCourtMutation(court.getId());
        return court;
    }

    @Override
    public Court getCourt(Integer id) {
        return courtCache.getCourt(id, () -> courtMapper.getCourt(id));
    }

    @Override
    public List<Court> searchCourts(String keyword) {
        return courtMapper.searchCourts(keyword);
    }

    @Override
    public Court updateCourt(Court court) {
        System.out.println("Service层更新court: " + court);

        courtMapper.updateCourt(court);
        System.out.println("更新成功，ID: " + court.getId());

        courtCache.evictAfterCourtMutation(court.getId());
        return court;
    }

    @Override
    public List<Court> hotCourts(int topN) {
        int n = Math.min(Math.max(topN, 1), 50);
        List<Integer> ids = courtCache.getHotCourtIds(n);
        if (ids.isEmpty()) {
            return findAll().stream().limit(n).toList();
        }
        List<Court> out = new ArrayList<>();
        for (Integer id : ids) {
            Court c = getCourt(id);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    @Override
    public boolean isHotCourt(Integer courtId) {
        return courtCache.isHotCourt(courtId);
    }
}