package isakids.isa.dto;

import java.util.List;

public class PageResponse<T> {

    public List<T> content;
    public int pageNumber;
    public int pageSize;
    public long totalElements;
    public int totalPages;
    public boolean first;
    public boolean last;
}
