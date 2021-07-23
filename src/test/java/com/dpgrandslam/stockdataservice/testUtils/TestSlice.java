package com.dpgrandslam.stockdataservice.testUtils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class TestSlice<T> implements Slice<T> {

    private List<T> content;

    private TestSlice(List<T> content) {
        this.content = content;
    }

    private TestSlice() {
        this.content = new ArrayList<>();
    }

    @Override
    public int getNumber() {
        return content.size();
    }

    @Override
    public int getSize() {
        return content.size();
    }

    @Override
    public int getNumberOfElements() {
        return content.size();
    }

    @Override
    public List<T> getContent() {
        return content;
    }

    @Override
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    @Override
    public Sort getSort() {
        return null;
    }

    @Override
    public boolean isFirst() {
        return true;
    }

    @Override
    public boolean isLast() {
        return true;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public Pageable nextPageable() {
        return Pageable.unpaged();
    }

    @Override
    public Pageable previousPageable() {
        return Pageable.unpaged();
    }

    @Override
    public <U> Slice<U> map(Function<? super T, ? extends U> function) {
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    public static <E> Slice<E> from(List<E> list) {
        return new TestSlice<>(list);
    }

    public static <E> Slice<E> from(Set<E> set) {
        return new TestSlice<>(new ArrayList<>(set));
    }
}
