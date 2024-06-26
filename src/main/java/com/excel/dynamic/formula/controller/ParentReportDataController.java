package com.excel.dynamic.formula.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.validation.constraints.Min;

import org.apache.poi.EncryptedDocumentException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.excel.dynamic.formula.dto.ParentReportSavedResponseDto;
import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.service.ParentReportService;
import com.excel.dynamic.formula.util.ConversionUtility;

@RestController
@CrossOrigin
public class ParentReportDataController {

	@Autowired
	private ParentReportService parentReportService;

	@PostMapping("/save/parent/report/data")
	public ResponseEntity<?> saveParentReportRequestData(@RequestBody ParentReportSavedResponseDto savedReportDto)
			throws EncryptedDocumentException, IOException {

		Response<?> response = parentReportService.saveExcelRequestData(savedReportDto);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}

	@GetMapping("/view/parent/excel/response/data")
	public ResponseEntity<?> viewParentExcelResponseData(@RequestParam("fileName") String fileName)
			throws UnsupportedEncodingException {
		Response<?> response = parentReportService.getExcelResponseData(fileName);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}

	@PostMapping("/update/parent/report/data")
	public ResponseEntity<?> updateParentReportData(@RequestBody ParentReportSavedResponseDto reportSavedResponseDto) {
		Response<?> response = parentReportService.updateParentReportData(reportSavedResponseDto);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}

	@GetMapping("/get/all/excel/report")
	public ResponseEntity<?> getAllExcelFileName(@RequestParam(required = false, defaultValue = "0") int pageSize,
			@RequestParam(required = false, defaultValue = "0") int pageNo,
			@RequestParam(required = false) String parentFileName, @RequestParam(required = false) List<String> status,
			@RequestParam(required = false) List<String> listOfParentFileName,@RequestParam(required = false) List<String> excludeCurrentSubReports) {
//		Response<?> response = parentReportService.getAllExcelFileNameV2(pageSize, pageNo, parentFileName);
		Response<?> response = parentReportService.getAllExcelFileNameV3(pageSize, pageNo, parentFileName, status,listOfParentFileName,excludeCurrentSubReports);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}

//	@GetMapping("v2/get/all/excel/report")
//	public ResponseEntity<?> getAllExcelFileNameV2(@RequestParam @Min(0) int pageSize, @RequestParam @Min(0) int pageNo,
//			@RequestParam String parentFileName) {
//		Response<?> response = parentReportService.getAllExcelFileNameV2(pageSize, pageNo, parentFileName);
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}

	@GetMapping("/get/parent/report/id")
	public ResponseEntity<?> getParentReportByid(@RequestParam("parentReportId") Long parentReportId) {
		Response<?> response = parentReportService.getParentReportByid(parentReportId);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}

}
