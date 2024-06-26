package com.excel.dynamic.formula.controller;

import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.dto.SubReportSaveRequestDto;
import com.excel.dynamic.formula.service.SubReportService;

@RestController
@CrossOrigin
public class SubReportDataController {

	@Autowired
	private SubReportService subReportService;

	@PostMapping("/save/sub/report/data")
	public ResponseEntity<?> saveSubReportRequestData(@RequestBody SubReportSaveRequestDto subReportSaveRequestDto)
			throws EncryptedDocumentException, IOException {
		Response<?> response = subReportService.saveSubReportRequestData(subReportSaveRequestDto);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
	}
	
//	@PostMapping("/valid/sub/report/data")
//	public ResponseEntity<?> validSubReportRequestData(@RequestBody SubReportSaveRequestDto subReportSaveRequestDto)
//			throws EncryptedDocumentException, IOException {
//		Response<?> response = subReportService.validSubReportData(subReportSaveRequestDto);
//		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
//	}
	

	@GetMapping("/v1/get/report/Id")
	public ResponseEntity<?> getSubReportById(@RequestParam Long subreportId) {
		Response<?> response = subReportService.getSubReportByid(subreportId);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));

	}

//	@PostMapping("/validate/sub")
//	public ResponseEntity<?> validateSubReport(@RequestBody String requestString) {
//		Response<?> response = subReportService.validateSubReportData(requestString, null);
//		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getResponseCode()));
//	}

}
