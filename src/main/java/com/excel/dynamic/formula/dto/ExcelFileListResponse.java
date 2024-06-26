package com.excel.dynamic.formula.dto;

import java.util.Date;
import java.util.List;


public class ExcelFileListResponse {

	private Long id;
	private String fileName;
	private List<SubReportDataDto> subReportData;
	private Date createdAt;
	private Long totalNoOfReport;
	private Date updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public List<SubReportDataDto> getSubReportData() {
		return subReportData;
	}

	public void setSubReportData(List<SubReportDataDto> subReportData) {
		this.subReportData = subReportData;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Long getTotalNoOfReport() {
		return totalNoOfReport;
	}

	public void setTotalNoOfReport(Long totalNoOfReport) {
		this.totalNoOfReport = totalNoOfReport;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

}
