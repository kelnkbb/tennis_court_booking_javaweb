package com.tennis_court_booking.cs.mapper;

import com.tennis_court_booking.cs.entity.CsMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CsMessageMapper {

    int insert(CsMessage row);

    List<CsMessage> listByConversationIdDesc(@Param("conversationId") Long conversationId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);
}
