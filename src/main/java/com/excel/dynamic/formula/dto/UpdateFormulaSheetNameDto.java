package com.excel.dynamic.formula.dto;

import java.util.Map;
import java.util.Set;

public class UpdateFormulaSheetNameDto {

	private Map<String, FormulaUpdateDto> subReportFormulaUpdate;
	Set<String> allSheetName;
	public Map<String, FormulaUpdateDto> getSubReportFormulaUpdate() {
		return subReportFormulaUpdate;
	}
	public void setSubReportFormulaUpdate(Map<String, FormulaUpdateDto> subReportFormulaUpdate) {
		this.subReportFormulaUpdate = subReportFormulaUpdate;
	}
	public Set<String> getAllSheetName() {
		return allSheetName;
	}
	public void setAllSheetName(Set<String> allSheetName) {
		this.allSheetName = allSheetName;
	}
	public UpdateFormulaSheetNameDto() {
		super();
	}

	
	
}
