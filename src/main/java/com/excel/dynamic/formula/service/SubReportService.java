package com.excel.dynamic.formula.service;

import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.dto.SubReportSaveRequestDto;
import com.google.gson.JsonElement;

public interface SubReportService {

	Response<?> getSubReportByid(Long subreportId);

	Response<?> saveSubReportRequestData(SubReportSaveRequestDto subReportSaveRequestDto);

//	Response<?> validateSubReportData(String subReportString, JsonElement parentResponseString);

	Response<?> validateSubReportData(String subReportString, JsonElement parentResponseString,
			SubReportSaveRequestDto subReportSaveRequestDto,String parentName);

	Response<?> validSubReportData(SubReportSaveRequestDto subReportSaveRequestDto);


}
