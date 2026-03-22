package com.tennis_court_booking.ai.tools;

import com.tennis_court_booking.pojo.entity.Court;
import com.tennis_court_booking.service.CourtService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourtTool {

    @Autowired
    private CourtService courtService;

    @Tool("查询所有场地列表")
    public List<Court> listCourts() {
        List<Court> courts = courtService.findAll();
        // 按 id 升序，便于用户理解
        courts.sort(Comparator.comparing(Court::getId));
        return courts;
    }

    @Tool("根据场地ID查询场地信息")
    public Court getCourtById(@P("场地ID") Integer courtId) {
        if (courtId == null) {
            return null;
        }
        return courtService.getCourt(courtId);
    }

    @Tool("根据关键词模糊筛选场地（名称/地址）")
    public List<Court> searchCourts(@P("关键词") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listCourts();
        }
        String k = keyword.trim();
        return courtService.searchCourts( k);
    }
}

