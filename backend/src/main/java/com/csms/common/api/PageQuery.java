package com.csms.common.api;

import lombok.Data;

@Data
public class PageQuery {
    private int page = 1;
    private int size = 10;
    private String sortBy;
    private String sortOrder;
}
