package com.excel.dynamic.formula.dto;

import java.util.Date;
import java.util.List;

import com.excel.dynamic.formula.enums.Status;

public class SubReportDataDto {

	private Long id;

	private String reportName;

	private Object requestObject;

	private Date createdAt;

	private Status status;

	private Long errorCount;

	private ParentReportDataDTO parentReportDataDTO;
	
	private List<ErrorResponseDto> errorMessageList;

	private List<String> allParentUsingInInterReport;
	
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public Object getRequestObject() {
		return requestObject;
	}

	public void setRequestObject(Object requestObject) {
		this.requestObject = requestObject;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public SubReportDataDto(Long id, String reportName, Object requestObject, Date createdAt) {
		super();
		this.id = id;
		this.reportName = reportName;
		this.requestObject = requestObject;
		this.createdAt = createdAt;
	}

	public SubReportDataDto() {

	}

	public Long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Long errorCount) {
		this.errorCount = errorCount;
	}

	public ParentReportDataDTO getParentReportDataDTO() {
		return parentReportDataDTO;
	}

	public void setParentReportDataDTO(ParentReportDataDTO parentReportDataDTO) {
		this.parentReportDataDTO = parentReportDataDTO;
	}

	public List<ErrorResponseDto> getErrorMessageList() {
		return errorMessageList;
	}

	public void setErrorMessageList(List<ErrorResponseDto> errorMessageList) {
		this.errorMessageList = errorMessageList;
	}
	

	public List<String> getAllParentUsingInInterReport() {
		return allParentUsingInInterReport;
	}

	public void setAllParentUsingInInterReport(List<String> allParentUsingInInterReport) {
		this.allParentUsingInInterReport = allParentUsingInInterReport;
	}

	public SubReportDataDto(Long id, String reportName, Object requestObject, Date createdAt,
			ParentReportDataDTO parentReportDataDTO) {
		super();
		this.id = id;
		this.reportName = reportName;
		this.requestObject = requestObject;
		this.createdAt = createdAt;
		this.parentReportDataDTO = parentReportDataDTO;
	}

	public SubReportDataDto(Long id, String reportName, Date createdAt, Status status, Long errorCount) {
		super();
		this.id = id;
		this.reportName = reportName;
		this.createdAt = createdAt;
		this.status = status;
		this.errorCount = errorCount;
	}

	
}
