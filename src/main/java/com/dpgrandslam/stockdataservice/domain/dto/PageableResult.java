package com.dpgrandslam.stockdataservice.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageableResult<T> {

    private Integer nextPageNumber;
    private Integer size;
    private List<T> data;

    public static <E> PageableResult<E> fromList(List<E> list, Integer nextPageNumber) {
        PageableResult<E> result = new PageableResult<>();
        result.setData(list);
        result.setSize(list.size());
        result.setNextPageNumber(nextPageNumber);
        return result;
    }
}
