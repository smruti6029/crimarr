package com.excel.dynamic.formula.dto;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FormulaUpdateDto {

	private String subCellName;

	private int rowNumber;

	private String parentFormula;

	private Boolean isChangedFormula;

	private Boolean isSequenceFormula;
	
	private int parentRownumber;

	private int sequence;
	
	private Boolean isInterSheetFormula;
	
	private Map<Integer,String> interReportCellAndValue=new  TreeMap<>();
	
	private Map<String,String>  parentReportAndSubreportName;
	
	private Boolean isUpdateParentFormulaInSubReport;
	
	private Set<String> allInterParentReport;
	
	private Boolean isSubReportSelected; 
	
	private String interReportParentFormula;
	
	private Boolean isSubReportFormulaForInterReport;
	
	private Boolean isInterReportErrorMessage;
	
	private String message;


	public Boolean getIsInterReportErrorMessage() {
		return isInterReportErrorMessage;
	}

	public void setIsInterReportErrorMessage(Boolean isInterReportErrorMessage) {
		this.isInterReportErrorMessage = isInterReportErrorMessage;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Boolean getIsSubReportFormulaForInterReport() {
		return isSubReportFormulaForInterReport;
	}

	public void setIsSubReportFormulaForInterReport(Boolean isSubReportFormulaForInterReport) {
		this.isSubReportFormulaForInterReport = isSubReportFormulaForInterReport;
	}

	public String getInterReportParentFormula() {
		return interReportParentFormula;
	}

	public void setInterReportParentFormula(String interReportParentFormula) {
		this.interReportParentFormula = interReportParentFormula;
	}

	public Map<String, String> getParentReportAndSubreportName() {
		return parentReportAndSubreportName;
	}

	public void setParentReportAndSubreportName(Map<String, String> parentReportAndSubreportName) {
		this.parentReportAndSubreportName = parentReportAndSubreportName;
	}

	public Boolean getIsUpdateParentFormulaInSubReport() {
		return isUpdateParentFormulaInSubReport;
	}

	public void setIsUpdateParentFormulaInSubReport(Boolean isUpdateParentFormulaInSubReport) {
		this.isUpdateParentFormulaInSubReport = isUpdateParentFormulaInSubReport;
	}

	

	public Set<String> getAllInterParentReport() {
		return allInterParentReport;
	}

	public void setAllInterParentReport(Set<String> allInterParentReport) {
		this.allInterParentReport = allInterParentReport;
	}

	public Boolean getIsSubReportSelected() {
		return isSubReportSelected;
	}

	public void setIsSubReportSelected(Boolean isSubReportSelected) {
		this.isSubReportSelected = isSubReportSelected;
	}

	public Map<Integer, String> getInterReportCellAndValue() {
		return interReportCellAndValue;
	}

	public void setInterReportCellAndValue(Map<Integer, String> interReportCellAndValue) {
		this.interReportCellAndValue = interReportCellAndValue;
	}

	public Boolean getIsInterSheetFormula() {
		return isInterSheetFormula;
	}

	public void setIsInterSheetFormula(Boolean isInterSheetFormula) {
		this.isInterSheetFormula = isInterSheetFormula;
	}

	public int getParentRownumber() {
		return parentRownumber;
	}

	public void setParentRownumber(int parentRownumber) {
		this.parentRownumber = parentRownumber;
	}

	public String getSubCellName() {
		return subCellName;
	}

	public void setSubCellName(String subCellName) {
		this.subCellName = subCellName;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public String getParentFormula() {
		return parentFormula;
	}

	public void setParentFormula(String parentFormula) {
		this.parentFormula = parentFormula;
	}

	public Boolean getIsChangedFormula() {
		return isChangedFormula;
	}

	public void setIsChangedFormula(Boolean isChangedFormula) {
		this.isChangedFormula = isChangedFormula;
	}

	public Boolean getIsSequenceFormula() {
		return isSequenceFormula;
	}

	public void setIsSequenceFormula(Boolean isSequenceFormula) {
		this.isSequenceFormula = isSequenceFormula;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public FormulaUpdateDto(String subCellName, int rowNumber, String parentFormula, Boolean isChangedFormula,
			Boolean isSequenceFormula) {
		super();
		this.subCellName = subCellName;
		this.rowNumber = rowNumber;
		this.parentFormula = parentFormula;
		this.isChangedFormula = isChangedFormula;
		this.isSequenceFormula = isSequenceFormula;
	}

	public FormulaUpdateDto() {
		super();

	}

}
