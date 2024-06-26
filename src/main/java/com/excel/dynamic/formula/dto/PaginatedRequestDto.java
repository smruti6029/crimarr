package com.excel.dynamic.formula.dto;

import java.util.List;

public class PaginatedRequestDto {

	private String parentFileName;

	List<String> listOfParentFileName;
	
	private int pageSize;

	private int pageNo;

	public String getParentFileName() {
		return parentFileName;
	}

	public void setParentFileName(String parentFileName) {
		this.parentFileName = parentFileName;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
	

	public List<String> getListOfParentFileName() {
		return listOfParentFileName;
	}

	public void setListOfParentFileName(List<String> listOfParentFileName) {
		this.listOfParentFileName = listOfParentFileName;
	}

	public PaginatedRequestDto(String parentFileName, int pageSize, int pageNo) {
		super();
		this.parentFileName = parentFileName;
		this.pageSize = pageSize;
		this.pageNo = pageNo;
	}



}
