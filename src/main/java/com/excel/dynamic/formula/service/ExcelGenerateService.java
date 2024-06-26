package com.excel.dynamic.formula.service;

import javax.servlet.http.HttpServletResponse;

public interface ExcelGenerateService {

	void excelGenerateFromObject(String fileName, String json, HttpServletResponse response);

	void getJsonBYIDS(Long parentId, Long subReportId, HttpServletResponse response);

}
