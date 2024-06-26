package com.excel.dynamic.formula.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.service.ExcelRequestDataService;

@RestController
@CrossOrigin
public class ExcelDataController {

	@Autowired
	private ExcelRequestDataService excelRequestDataService;
	
	@Autowired
	private ExcelRequestDataService dataServiceImpl2;

	@PostMapping("/get/excel/response/object")
	public ResponseEntity<?> getExcelResponseObject(@ModelAttribute MultipartFile file) {
		Response<?> response = dataServiceImpl2.getExcelResponseObject(file);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}

}
