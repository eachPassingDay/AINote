package com.ainote.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphDataDTO {
    private List<NodeDTO> nodes;
    private List<LinkDTO> links;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDTO {
        private String id;
        private String label;
        private String group;
        private int weight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkDTO {
        private String source;
        private String target;
        private String type;
    }
}
