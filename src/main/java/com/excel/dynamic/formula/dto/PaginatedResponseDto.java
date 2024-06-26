package com.excel.dynamic.formula.dto;


public class PaginatedResponseDto<T> {

	private long totalElement;
	private long itemsPerPage;
	private long totalPage;
	private long pageNo;
	private T data;
	public long getTotalElement() {
		return totalElement;
	}
	public void setTotalElement(long totalElement) {
		this.totalElement = totalElement;
	}
	public long getItemsPerPage() {
		return itemsPerPage;
	}
	public void setItemsPerPage(long itemsPerPage) {
		this.itemsPerPage = itemsPerPage;
	}
	public long getTotalPage() {
		return totalPage;
	}
	public void setTotalPage(long totalPage) {
		this.totalPage = totalPage;
	}
	public long getPageNo() {
		return pageNo;
	}
	public void setPageNo(long pageNo) {
		this.pageNo = pageNo;
	}
	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}
	public PaginatedResponseDto(long totalElement, long itemsPerPage, long totalPage, long pageNo, T data) {
		super();
		this.totalElement = totalElement;
		this.itemsPerPage = itemsPerPage;
		this.totalPage = totalPage;
		this.pageNo = pageNo;
		this.data = data;
	}
	public PaginatedResponseDto() {
		super();
	}
	
	
	
	

}
