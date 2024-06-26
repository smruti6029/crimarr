package com.excel.dynamic.formula.dto;

import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

import com.google.gson.JsonArray;

public class ExtractInterReportValueFromreport {

	private SimpleEntry<String, Map<Integer, String>> modifiyFormulaAndInterReportCellAndValue;

	private FormulaUpdateDto formulaUpdateDto;

	private Boolean isSubReportSelected;

	private Set<String> allInterParentReport;
	
	private JsonArray modifiedIfConditionArray;

	
	public JsonArray getModifiedIfConditionArray() {
		return modifiedIfConditionArray;
	}

	public void setModifiedIfConditionArray(JsonArray modifiedIfConditionArray) {
		this.modifiedIfConditionArray = modifiedIfConditionArray;
	}

	public Boolean getIsSubReportSelected() {
		return isSubReportSelected;
	}

	public void setIsSubReportSelected(Boolean isSubReportSelected) {
		this.isSubReportSelected = isSubReportSelected;
	}

	public Set<String> getAllInterParentReport() {
		return allInterParentReport;
	}

	public void setAllInterParentReport(Set<String> allInterParentReport) {
		this.allInterParentReport = allInterParentReport;
	}

	public SimpleEntry<String, Map<Integer, String>> getModifiyFormulaAndInterReportCellAndValue() {
		return modifiyFormulaAndInterReportCellAndValue;
	}

	public void setModifiyFormulaAndInterReportCellAndValue(
			SimpleEntry<String, Map<Integer, String>> modifiyFormulaAndInterReportCellAndValue) {
		this.modifiyFormulaAndInterReportCellAndValue = modifiyFormulaAndInterReportCellAndValue;
	}

	public FormulaUpdateDto getFormulaUpdateDto() {
		return formulaUpdateDto;
	}

	public void setFormulaUpdateDto(FormulaUpdateDto formulaUpdateDto) {
		this.formulaUpdateDto = formulaUpdateDto;
	}

	public ExtractInterReportValueFromreport() {
		super();

	}

}
