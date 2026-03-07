package com.ainote.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteHistoryDTO {
    private Number revisionId;
    private Date revisionDate;
    private String revisionType; // 审计操作类型：ADD=新增, MOD=修改, DEL=删除
    private String title;
    private String summary;
    private String status;
}
