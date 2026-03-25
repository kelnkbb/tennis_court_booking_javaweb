package com.tennis_court_booking.cs.mapper;

import com.tennis_court_booking.cs.entity.CsConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CsConversationMapper {

    int insert(CsConversation row);

    CsConversation findOpenByUserId(@Param("userId") Integer userId);

    CsConversation findById(@Param("id") Long id);

    int updateUpdatedAt(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    int closeById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    List<CsConversation> listOpen(@Param("limit") int limit);
}
