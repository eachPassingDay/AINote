package com.ainote.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolishRequestDTO {
    /**
     * 用户希望润色的文本片段
     */
    private String text;

    /**
     * 润色指令（如"语气更正式"、"精炼表达"等）
     */
    private String instruction;

    /**
     * 完整笔记正文或周边文本，仅作为理解上下文的参考，绝不会被改写
     */
    private String context;
}
