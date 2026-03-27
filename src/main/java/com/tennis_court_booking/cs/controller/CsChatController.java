package com.tennis_court_booking.cs.controller;

import com.tennis_court_booking.cs.entity.CsConversation;
import com.tennis_court_booking.cs.entity.CsMessage;
import com.tennis_court_booking.cs.service.CsChatService;
import com.tennis_court_booking.cs.vo.CsConversationVO;
import com.tennis_court_booking.cs.vo.CsMessageVO;
import com.tennis_court_booking.mapper.UserMapper;
import com.tennis_court_booking.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cs")
@RequiredArgsConstructor
public class CsChatController {

    private final CsChatService csChatService;
    private final UserMapper userMapper;

    @GetMapping("/me/conversation")
    public Result<CsConversationVO> myOpenConversation(@RequestAttribute("userId") Integer userId) {
        CsConversation c = csChatService.getOpenConversationForUser(userId);
        if (c == null) {
            return Result.success(null);
        }
        return Result.success(toConvVo(c));
    }

    @GetMapping("/me/messages")
    public Result<List<CsMessageVO>> myMessages(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(defaultValue = "100") int limit) {
        CsConversation c = csChatService.getOpenConversationForUser(userId);
        if (c == null) {
            return Result.success(List.of());
        }
        return Result.success(toMsgVoList(csChatService.listRecentMessagesChronological(c.getId(), limit)));
    }

    @PostMapping("/me/conversation/close")
    public Result<Void> closeMyConversation(@RequestAttribute("userId") Integer userId) {
        CsConversation c = csChatService.getOpenConversationForUser(userId);
        if (c == null) {
            return Result.error(Result.NOT_FOUND, "暂无进行中的会话");
        }
        try {
            csChatService.closeConversationForUser(c.getId(), userId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/admin/conversations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<List<CsConversationVO>> listOpenForAdmin(
            @RequestParam(defaultValue = "50") int limit) {
        List<CsConversationVO> list = csChatService.listOpenConversations(limit).stream()
                .map(this::toConvVo)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/admin/conversations/{id}/messages")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<List<CsMessageVO>> adminMessages(
            @PathVariable("id") Long conversationId,
            @RequestParam(defaultValue = "200") int limit) {
        CsConversation c = csChatService.findConversation(conversationId);
        if (c == null) {
            return Result.error(Result.NOT_FOUND, "会话不存在");
        }
        return Result.success(toMsgVoList(csChatService.listRecentMessagesChronological(conversationId, limit)));
    }

    @PostMapping("/admin/conversations/{id}/close")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Void> adminClose(@PathVariable("id") Long conversationId) {
        try {
            csChatService.closeConversationForAdmin(conversationId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    private CsConversationVO toConvVo(CsConversation c) {
        String username = null;
        if (c.getUserId() != null) {
            // 直查 DB，不走 UserService 多级缓存（缓存条目可能缺少 username 字段）
            username = userMapper.findUsernameById(c.getUserId());
        }
        return new CsConversationVO(c.getId(), c.getUserId(), username, c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private List<CsMessageVO> toMsgVoList(List<CsMessage> list) {
        return list.stream()
                .map(m -> new CsMessageVO(m.getId(), m.getConversationId(), m.getSenderType(),
                        m.getSenderUserId(), m.getContent(), m.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
