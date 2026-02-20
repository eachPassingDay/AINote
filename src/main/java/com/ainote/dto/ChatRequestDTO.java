package com.ainote.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String query;
    private String filterDomain;
    private String filterType;
}
