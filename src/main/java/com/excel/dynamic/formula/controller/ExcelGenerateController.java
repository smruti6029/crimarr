package com.excel.dynamic.formula.controller;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.excel.dynamic.formula.service.ExcelGenerateService;

@RestController
@CrossOrigin
public class ExcelGenerateController {

	@Autowired
	private ExcelGenerateService excelGenerateService;

	@PostMapping("/generate/excel")
	public ResponseEntity<?> viewExcelResponseData(@RequestParam("fileName") String fileName, @RequestBody String json,
			HttpServletResponse response) throws UnsupportedEncodingException {
		excelGenerateService.excelGenerateFromObject(fileName, json, response);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/v2/generate/excel")
	public ResponseEntity<?> getExcelResponse(@RequestParam Long parentId,
			@RequestParam(required = false) Long subReportID, HttpServletResponse response)
			throws UnsupportedEncodingException {
		excelGenerateService.getJsonBYIDS(parentId, subReportID, response);
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
