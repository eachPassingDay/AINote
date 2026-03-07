package com.ainote.dto;

import java.util.List;

public record CodePropositionDTO(
        String language,
        String functionality,
        List<String> core_apis) {
}
