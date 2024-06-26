package com.excel.dynamic.formula.dto;

import java.util.Date;

import com.excel.dynamic.formula.model.ParentReportData;

public class ParentReportDataDTO {

	private Long id;
	private Object requestData;
	private Date createdAt;
	private String excelFileName;
	private Date updatedAt;
	private Boolean isActive;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Object getRequestData() {
		return requestData;
	}

	public void setRequestData(Object requestData) {
		this.requestData = requestData;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getExcelFileName() {
		return excelFileName;
	}

	public void setExcelFileName(String excelFileName) {
		this.excelFileName = excelFileName;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public ParentReportDataDTO(Long id, Date createdAt, String excelFileName, Date updatedAt, Boolean isActive) {
		super();
		this.id = id;
		this.createdAt = createdAt;
		this.excelFileName = excelFileName;
		this.updatedAt = updatedAt;
		this.isActive = isActive;
	}

	public ParentReportDataDTO() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static ParentReportDataDTO convertEntityToDto(ParentReportData parentReportData) {
		if (parentReportData == null) {
			return null;
		}

		return new ParentReportDataDTO(parentReportData.getId(), parentReportData.getCreatedAt(),
				parentReportData.getExcelFileName(), parentReportData.getUpdatedAt(), parentReportData.getIsActive());
	}

}
