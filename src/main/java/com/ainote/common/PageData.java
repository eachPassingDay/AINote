package com.ainote.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {
    private List<T> records;
    private long total;
    private int current;
    private int size;

    public static <T> PageData<T> of(List<T> records, long total, int current, int size) {
        return new PageData<>(records, total, current, size);
    }
}
