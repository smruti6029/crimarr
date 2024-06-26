package com.excel.dynamic.formula.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.excel.dynamic.formula.dto.ErrorResponseDto;
import com.excel.dynamic.formula.dto.ParentReportSavedResponseDto;
import com.excel.dynamic.formula.dto.Response;

public interface ParentReportService {

	Response<?> saveExcelRequestData(ParentReportSavedResponseDto savedReportDto);

	Response<?> getExcelResponseData(String fileName) throws UnsupportedEncodingException;

	Response<?> getAllExcelFileName();

	Response<?> getAllExcelFileNameV2(int pageSize, int pageNo, String excelFileName);

	Response<?> getParentReportByid(Long parentReportId);

	Response<?> updateParentReportData(ParentReportSavedResponseDto reportSavedResponseDto);
	
//	List<ErrorResponseDto> validateParentResponseData(String jsonString);

	Response<?> getAllExcelFileNameV3(int pageSize, int pageNo, String excelFileName, List<String> status, List<String> listParentFileName,List<String> excludeCurrentSubReports);

}
