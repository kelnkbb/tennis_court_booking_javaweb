package com.tennis_court_booking.controller;

import com.tennis_court_booking.pojo.entity.Court;
import com.tennis_court_booking.pojo.vo.Result;
import com.tennis_court_booking.service.CourtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourtController {

    @Autowired
    private CourtService courtService;

    /**
     * 获取所有场馆列表
     */
    @GetMapping("/courts")
    public Result<List<Court>> getAllCourts() {
        List<Court> courts = courtService.findAll();
        return Result.success(courts);
    }

    /**
     * 热点场地 TopN（基于多级缓存层记录的访问热度 ZSet；新环境无数据时回退为列表前 N 条）。
     */
    @GetMapping("/courts/hot")
    public Result<List<Court>> hotCourts(@RequestParam(defaultValue = "5") int topN) {
        return Result.success(courtService.hotCourts(topN));
    }

    /**
     * 是否位于当前「热门 Set」内（热度前 5），供展示角标等 O(1) 判断。
     */
    @GetMapping("/courts/hot/{id}/member")
    public Result<Boolean> hotCourtMember(@PathVariable Integer id) {
        return Result.success(courtService.isHotCourt(id));
    }

    /**
     * 根据id删除场馆
     */
    @DeleteMapping("/courts/{id}")
    public Result<Void> deleteCourt(@PathVariable Integer id) {
        System.out.println("接收到删除请求，id = " + id);
        courtService.deleteById(id);
        return Result.success();
    }

    /**
     * 添加场馆
     */
    @PostMapping("/courts")
    public Result<Court> addCourt(@RequestBody Court court) {
        System.out.println("接收到添加请求，court = " + court);
        Court newCourt = courtService.addCourt(court);
        return Result.success("添加成功", newCourt);
    }

    /**
     * 根据id查询场馆（用于编辑回显）
     */
    @GetMapping("/courts/{id}")
    public Result<Court> getCourt(@PathVariable Integer id) {
        System.out.println("接收到查询请求，id = " + id);
        Court court = courtService.getCourt(id);
        return Result.success(court);
    }

    /**
     * 更新场馆
     */
    @PutMapping("/courts/{id}")
    public Result<Court> updateCourt(@PathVariable Integer id, @RequestBody Court court) {
        System.out.println("接收到更新请求，id = " + id + ", court = " + court);
        court.setId(id); // 确保ID正确
        Court updatedCourt = courtService.updateCourt(court);
        return Result.success("更新成功", updatedCourt);
    }
}