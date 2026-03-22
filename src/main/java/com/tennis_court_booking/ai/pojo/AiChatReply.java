package com.tennis_court_booking.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatReply {
    private String content;
    private List<String> quickOptions = new ArrayList<>();
}
