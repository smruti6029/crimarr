package com.excel.dynamic.formula.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Component
@JsonInclude(Include.NON_NULL)
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Response<T> {
	private int responseCode;
	private String message;
	private String requestedURI;
	private T data;
	private Boolean booleanValue;
	private List<ErrorResponseDto> errorMessageList;
	private Integer errorCount;
	private boolean isExcelManipulated;
	private Set<String> parentNamesUsingInInterReportFormula;
	private int interReportUsingFormulaErrorCount;

	

	public Set<String> getParentNamesUsingInInterReportFormula() {
		return parentNamesUsingInInterReportFormula;
	}

	public void setParentNamesUsingInInterReportFormula(Set<String> parentNamesUsingInInterReportFormula) {
		this.parentNamesUsingInInterReportFormula = parentNamesUsingInInterReportFormula;
	}

	public void setExcelManipulated(boolean isExcelManipulated) {
		this.isExcelManipulated = isExcelManipulated;
	}

	public HashMap<String, Object> getParentObjectMap() {
		return parentObjectMap;
	}

	public void setParentObjectMap(HashMap<String, Object> parentObjectMap) {
		this.parentObjectMap = parentObjectMap;
	}

	public Set<String> getParentSheetNames() {
		return parentSheetNames;
	}

	public void setParentSheetNames(Set<String> parentSheetNames) {
		this.parentSheetNames = parentSheetNames;
	}

	HashMap<String, Object> parentObjectMap;
	Set<String> parentSheetNames = new HashSet<>();

	public boolean getIsExcelManipulated() {
		return isExcelManipulated;
	}

	public void setIsExcelManupulated(boolean isExcelManipulated) {
		this.isExcelManipulated = isExcelManipulated;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getRequestedURI() {
		return requestedURI;
	}

	public void setRequestedURI(String requestedURI) {
		this.requestedURI = requestedURI;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	public List<ErrorResponseDto> getErrorMessageList() {
		return errorMessageList;
	}

	public void setErrorMessageList(List<ErrorResponseDto> errorMessageList) {
		this.errorMessageList = errorMessageList;
	}

	public Integer getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Integer errorCount) {
		this.errorCount = errorCount;
	}
	

	public int getInterReportUsingFormulaErrorCount() {
		return interReportUsingFormulaErrorCount;
	}

	public void setInterReportUsingFormulaErrorCount(int interReportUsingFormulaErrorCount) {
		this.interReportUsingFormulaErrorCount = interReportUsingFormulaErrorCount;
	}

	public Response(int responseCode, String message, T data) {
		super();
		this.responseCode = responseCode;
		this.message = message;
		this.data = data;
	}

	public Response(int responseCode, String message, T data, boolean isExcelManipulated) {
		super();
		this.responseCode = responseCode;
		this.message = message;
		this.data = data;
		this.isExcelManipulated = isExcelManipulated;
	}

	public Response(int responseCode, String message, Boolean booleanValue) {
		super();
		this.responseCode = responseCode;
		this.message = message;
		this.booleanValue = booleanValue;
	}

	public Response(int responseCode, String message, T data, List<ErrorResponseDto> errorMessageList,
			Integer errorCount, Set<String> parentNamesUsingInInterReportFormula,int interReportUsingFormulaErrorCount ) {
		super();
		this.responseCode = responseCode;
		this.message = message;
		this.data = data;
		this.errorMessageList = errorMessageList;
		this.errorCount = errorCount;
		this.isExcelManipulated = false;
		this.parentNamesUsingInInterReportFormula = parentNamesUsingInInterReportFormula;
		this.interReportUsingFormulaErrorCount=interReportUsingFormulaErrorCount;
	}

	public Response() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Response(int responseCode, HashMap<String, Object> parentObjectMap, Set<String> parentSheetNames) {
		super();
		this.responseCode = responseCode;
		this.parentObjectMap = parentObjectMap;
		this.parentSheetNames = parentSheetNames;
	}

}
