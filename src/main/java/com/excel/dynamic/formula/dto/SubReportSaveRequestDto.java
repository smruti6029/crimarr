package com.excel.dynamic.formula.dto;

import java.util.List;

public class SubReportSaveRequestDto {

	private String subReportName;
	private String requestData;
	private Long parentReportId;
	private Long errorCount;
	private Boolean isCheck;
	private Long subReportId;
	private Boolean isIgnoreAllDataSheetFormula;
	private List<String> allParentUsingInInterReport;
	private List<ErrorResponseDto> errorMessageList;
	private String status;
	private int interReportUsingFormulaErrorCount;
	private Boolean ispartialCompleted;


	public Boolean getIspartialCompleted() {
		return ispartialCompleted;
	}

	public void setIspartialCompleted(Boolean ispartialCompleted) {
		this.ispartialCompleted = ispartialCompleted;
	}

	public List<String> getAllParentUsingInInterReport() {
		return allParentUsingInInterReport;
	}

	public void setAllParentUsingInInterReport(List<String> allParentUsingInInterReport) {
		this.allParentUsingInInterReport = allParentUsingInInterReport;
	}

	public String getSubReportName() {
		return subReportName;
	}

	public void setSubReportName(String subReportName) {
		this.subReportName = subReportName;
	}

	public String getRequestData() {
		return requestData;
	}

	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}

	public Long getParentReportId() {
		return parentReportId;
	}

	public void setParentReportId(Long parentReportId) {
		this.parentReportId = parentReportId;
	}

	public Long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Long errorCount) {
		this.errorCount = errorCount;
	}

	public Boolean getIsCheck() {
		return isCheck;
	}

	public void setIsCheck(Boolean isCheck) {
		this.isCheck = isCheck;
	}

	public List<ErrorResponseDto> getErrorMessageList() {
		return errorMessageList;
	}

	public void setErrorMessageList(List<ErrorResponseDto> errorMessageList) {
		this.errorMessageList = errorMessageList;
	}

	public Long getSubReportId() {
		return subReportId;
	}

	public void setSubReportId(Long subReportId) {
		this.subReportId = subReportId;
	}

	public Boolean getIsIgnoreAllDataSheetFormula() {
		return isIgnoreAllDataSheetFormula;
	}

	public void setIsIgnoreAllDataSheetFormula(Boolean isIgnoreAllDataSheetFormula) {
		this.isIgnoreAllDataSheetFormula = isIgnoreAllDataSheetFormula;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getInterReportUsingFormulaErrorCount() {
		return interReportUsingFormulaErrorCount;
	}

	public void setInterReportUsingFormulaErrorCount(int interReportUsingFormulaErrorCount) {
		this.interReportUsingFormulaErrorCount = interReportUsingFormulaErrorCount;
	}
	
	

}
