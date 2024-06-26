package com.excel.dynamic.formula.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import com.excel.dynamic.formula.dto.ErrorResponseDto;
import com.excel.dynamic.formula.dto.ParentReportSavedResponseDto;
import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.dto.SubReportSaveRequestDto;

public interface ExcelRequestDataService {

	Response<?> getExcelResponseObject(MultipartFile file);

}
