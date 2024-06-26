package com.excel.dynamic.formula.dto;

public class ErrorResponseDto {

	private Long uniqueId;

	private String sheetName;

	private String cellName;

	private String errorMessage;

	private long rowNumber;

	public ErrorResponseDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Long getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(Long uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getSheetName() {
		return sheetName;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	public String getCellName() {
		return cellName;
	}

	public void setCellName(String cellName) {
		this.cellName = cellName;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public long getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(long rowNumber) {
		this.rowNumber = rowNumber;
	}

	public ErrorResponseDto(Long uniqueId, String sheetName, String cellName, String errorMessage, long rowNumber) {
		super();
		this.uniqueId = uniqueId;
		this.sheetName = sheetName;
		this.cellName = cellName;
		this.errorMessage = errorMessage;
		this.rowNumber = rowNumber;
	}

}
