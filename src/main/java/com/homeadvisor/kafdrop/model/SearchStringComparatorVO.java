package com.homeadvisor.kafdrop.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchStringComparatorVO {
    private boolean isOrComparator = true;
    private List<String> contains = new ArrayList<>();
    private boolean isSearchEnabled = false;

    public SearchStringComparatorVO(String searchBy) {
        if (StringUtils.isNotBlank(searchBy)) {
            if (searchBy.contains("&")) {
                contains = Arrays.asList(searchBy.split("[&]"));
                isOrComparator = false;
            } else {
                contains = Arrays.asList(searchBy.split("[|]"));
            }
            isSearchEnabled = true;
        }
    }

    public boolean isOrComparator() {
        return isOrComparator;
    }

    public SearchStringComparatorVO setOrComparator(boolean orComparator) {
        isOrComparator = orComparator;
        return this;
    }

    public boolean isAndComparator() {
        return !isOrComparator;
    }

    public SearchStringComparatorVO setAndComparator(boolean andComparator) {
        isOrComparator = !andComparator;
        return this;
    }

    public List<String> getContains() {
        return contains;
    }

    public SearchStringComparatorVO setContains(List<String> contains) {
        this.contains = contains;
        return this;
    }

    public boolean isSearchEnabled() {
        return isSearchEnabled;
    }

    public SearchStringComparatorVO setSearchEnabled(boolean searchEnabled) {
        isSearchEnabled = searchEnabled;
        return this;
    }

    public boolean validate(String message) {
        return isOrComparator ? contains.stream().anyMatch(message::contains) :
                contains.stream().allMatch(message::contains);
    }
}
