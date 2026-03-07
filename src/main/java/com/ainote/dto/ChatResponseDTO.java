package com.ainote.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private String sessionId;
    private String reply;
    private List<Citation> citations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String noteId;
        private String title;
    }
}
