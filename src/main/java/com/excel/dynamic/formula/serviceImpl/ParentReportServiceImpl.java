package com.excel.dynamic.formula.serviceImpl;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.excel.dynamic.formula.dto.ErrorResponseDto;
import com.excel.dynamic.formula.dto.ExcelFileListResponse;
import com.excel.dynamic.formula.dto.PaginatedRequestDto;
import com.excel.dynamic.formula.dto.PaginatedResponseDto;
import com.excel.dynamic.formula.dto.ParentReportDataDTO;
import com.excel.dynamic.formula.dto.ParentReportSavedResponseDto;
import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.dto.SubReportDataDto;
import com.excel.dynamic.formula.enums.Status;
import com.excel.dynamic.formula.model.ParentReportData;
import com.excel.dynamic.formula.model.ParentReportDataLazy;
import com.excel.dynamic.formula.model.SubReportData;
import com.excel.dynamic.formula.model.SubReportDataLazy;
import com.excel.dynamic.formula.repository.ParentReportLazyRepository;
import com.excel.dynamic.formula.repository.ParentReportRepository;
import com.excel.dynamic.formula.repository.SubReportLazyRepository;
import com.excel.dynamic.formula.repository.SubReportRepository;
import com.excel.dynamic.formula.service.ParentReportService;
import com.excel.dynamic.formula.util.ConversionUtility;
import com.excel.dynamic.formula.util.ExcelFormulaEvaluator;
import com.excel.dynamic.formula.util.ExcelValidation;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Service
public class ParentReportServiceImpl implements ParentReportService {

	@Autowired
	private ParentReportRepository parentReportRepository;

	@Autowired
	private SubReportRepository subReportRepository;

	@Autowired
	private SubReportLazyRepository subReportLazyRepository;

	@Autowired
	private ParentReportLazyRepository parentReportLazyRepository;

	@Override
	public Response<?> saveExcelRequestData(ParentReportSavedResponseDto parentReportSaveDto) {
		try {
			if (parentReportSaveDto.getFileName().isEmpty() || parentReportSaveDto.getFileName().equals("")) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "File name cannot be empty", null);
			}
			Optional<ParentReportData> excelDataExist = parentReportRepository
					.findByExcelFileName(parentReportSaveDto.getFileName());

			if (excelDataExist.isPresent()) {
				return new Response<>(HttpStatus.ALREADY_REPORTED.value(),
						"Excel data already exist with provided file name.", null);
			} else {

				Response<?> validateExcelData = ExcelValidation.validateExcelData(parentReportSaveDto);
				if (validateExcelData.getResponseCode() == HttpStatus.BAD_REQUEST.value()) {
					return validateExcelData;
				}

				ParentReportData data = new ParentReportData();
				data.setExcelFileName(parentReportSaveDto.getFileName());
				data.setRequestData(validateExcelData.getData().toString().getBytes());
				data.setCreatedAt(new Date());
				data.setUpdatedAt(new Date());
				data.setIsActive(true);

//				List<ErrorResponseDto> errorResponseDto = ExcelValidation
//						.checkFormulaAndValidations(parentReportSaveDto.getResponseString());

//				if (errorResponseDto.size() > 0) {
//					return new Response<>(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(),
//							errorResponseDto);
//				} else {
				parentReportRepository.save(data);
				return new Response<>(HttpStatus.OK.value(), "Report saved successfully.", null);
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Something went wrong.", null);
		}
	}

	@Override
	public Response<?> getExcelResponseData(String fileName) throws UnsupportedEncodingException {
		Optional<ParentReportData> excelDataExist = parentReportRepository.findByExcelFileName(fileName);
		if (excelDataExist != null && excelDataExist.isPresent()) {
			byte[] requestDataBytes = excelDataExist.get().getRequestData();
			if (requestDataBytes != null) {
				String requestDataString = new String(requestDataBytes, "UTF-8");
				return new Response<>(HttpStatus.OK.value(), "Excel Response Data.", requestDataString);
			} else {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Error while converting from byte array to string", null);
			}
		} else {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Excel data not found.", null);
		}
	}

	@Override
	public Response<?> getAllExcelFileName() {
		List<ParentReportData> excelList = parentReportRepository.findAll();
		List<ExcelFileListResponse> responseData = new ArrayList<>();
		if (excelList != null && excelList.size() > 0) {
			for (ParentReportData fileResponse : excelList) {
				ExcelFileListResponse dto = new ExcelFileListResponse();
				dto.setId(fileResponse.getId());
				dto.setFileName(fileResponse.getExcelFileName());
				List<SubReportData> subReportList = subReportRepository.findAllByParentReportId(fileResponse.getId());
//				dto.setSubReportData(subReportList);
				Integer subReportSize = subReportList.size();
				dto.setTotalNoOfReport(subReportSize.longValue());
				dto.setCreatedAt(fileResponse.getCreatedAt());
				responseData.add(dto);
			}

		}
		return new Response<>(HttpStatus.OK.value(), "Excel File List.", responseData);
	}

	@Override
	public Response<?> updateParentReportData(ParentReportSavedResponseDto reportSavedResponseDto) {
		try {
			if (reportSavedResponseDto.getId() == null) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent Id is null", null);
			}
			Optional<ParentReportData> parentExistData = parentReportRepository
					.findById(reportSavedResponseDto.getId());
			if (parentExistData != null && parentExistData.isPresent()) {

				List<SubReportDataLazy> subReportDataLazies = subReportLazyRepository
						.findAllByParentReportId(parentExistData.get().getId());

				if (subReportDataLazies.size() > 0) {
					if (subReportDataLazies.size() > 1) {
						return new Response<>(HttpStatus.BAD_REQUEST.value(),
								"Now, you can't update the parent report data !", null);
					} else {
						if (subReportDataLazies.get(0).getStatus() != null
								&& (subReportDataLazies.get(0).getStatus().name().equals(Status.COMPLETED.name())
										|| subReportDataLazies.get(0).getStatus().name()
												.equals(Status.PARTIAL_COMPLETED.name()))) {

							return new Response<>(HttpStatus.BAD_REQUEST.value(),
									"Now, you can't update the parent report data !", null);

						}
					}
				}

				Response<?> validateExcelData = ExcelValidation.validateExcelData(reportSavedResponseDto);

				if (validateExcelData.getResponseCode() == HttpStatus.BAD_REQUEST.value()) {
					return validateExcelData;
				}

				ParentReportData parentReportData = parentExistData.get();

				parentReportData.setRequestData(validateExcelData.getData().toString().getBytes());

				parentReportData.setUpdatedAt(new Date());

				parentReportRepository.save(parentReportData);

				return new Response<>(HttpStatus.OK.value(), "Updated successfully", parentReportData);

			} else {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "No data found.", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Something went wrong", null);
		}

	}

	@Override
	public Response<?> getAllExcelFileNameV2(int pageSize, int pageNo, String excelFileName) {
		try {
			PaginatedRequestDto paginatedRequest = new PaginatedRequestDto(excelFileName, pageSize, pageNo);
			Pageable pageable = pageSize > 0 ? PageRequest.of(pageNo, pageSize) : Pageable.unpaged();
			Page<ParentReportData> findAll = parentReportRepository.findAll(paginatedRequest, pageable);

			List<ParentReportData> excelList = findAll.getContent();
			List<ExcelFileListResponse> responseData = new ArrayList<>(excelList.size());

			if (!excelList.isEmpty()) {
				List<Long> parentIdList = excelList.stream().map(ParentReportData::getId).collect(Collectors.toList());
				List<SubReportData> listOfsubReportDataFromDB = subReportRepository
						.findAllByParentReportDataIn(parentIdList);

				Map<Long, List<SubReportData>> listOfSubReportDataByParentId = listOfsubReportDataFromDB.stream()
						.collect(Collectors.groupingBy(value -> value.getParentReportData().getId()));

				excelList.forEach(fileResponse -> {
					ExcelFileListResponse dto = new ExcelFileListResponse();
					dto.setId(fileResponse.getId());
					dto.setFileName(fileResponse.getExcelFileName());

					List<SubReportData> subReports = listOfSubReportDataByParentId.get(fileResponse.getId());
					if (subReports != null && !subReports.isEmpty()) {
						List<SubReportDataDto> listOfsubReportData = subReports.stream()
								.map(reportData -> new SubReportDataDto(reportData.getId(), reportData.getReportName(),
										reportData.getCreatedAt(), reportData.getStatus(), reportData.getErrorCount()))
								.collect(Collectors.toList());
						dto.setSubReportData(listOfsubReportData);
						dto.setTotalNoOfReport((long) listOfsubReportData.size());
					}
					dto.setCreatedAt(fileResponse.getCreatedAt());
					dto.setUpdatedAt(fileResponse.getUpdatedAt());
					responseData.add(dto);
				});
			}

			PaginatedResponseDto<Object> paginatedResponseDto = new PaginatedResponseDto<>(
					parentReportRepository.count(), responseData.size(), findAll.getTotalPages(), pageNo, responseData);
			return new Response<>(HttpStatus.OK.value(), "Excel File List.", paginatedResponseDto);
		} catch (Exception e) {
			return new Response<>(HttpStatus.OK.value(), "Error occurred while processing excel file list", null);
		}
	}

	@Override
	public Response<?> getAllExcelFileNameV3(int pageSize, int pageNo, String excelFileName, List<String> status,
			List<String> listOfParentFileName,List<String> ignoreSubReports) {
		
		
		
		try {
			PaginatedRequestDto paginatedRequest = new PaginatedRequestDto(excelFileName, pageSize, pageNo);
			paginatedRequest.setListOfParentFileName(listOfParentFileName);

			Pageable pageable = pageSize > 0 ? PageRequest.of(pageNo, pageSize) : Pageable.unpaged();
			Page<ParentReportDataLazy> findAll = parentReportLazyRepository.findAll(paginatedRequest, pageable);

			List<ParentReportDataLazy> excelList = findAll.getContent();
			List<ExcelFileListResponse> responseData = new ArrayList<>(excelList.size());

			if (!excelList.isEmpty()) {
				List<Long> parentIdList = excelList.stream().map(ParentReportDataLazy::getId)
						.collect(Collectors.toList());
				List<SubReportDataLazy> listOfsubReportDataFromDB = subReportLazyRepository
						.findAllByParentReportDataIn(parentIdList);

				Map<Long, List<SubReportDataLazy>> listOfSubReportDataByParentId = listOfsubReportDataFromDB.stream()
						.collect(Collectors.groupingBy(value -> value.getParentReportData()));

				excelList.forEach(fileResponse -> {
					ExcelFileListResponse dto = new ExcelFileListResponse();
					dto.setId(fileResponse.getId());
					dto.setFileName(fileResponse.getExcelFileName());

					List<SubReportDataLazy> subReports = listOfSubReportDataByParentId.get(fileResponse.getId());

					if (subReports != null && !subReports.isEmpty()) {

						
						if (status != null &&status.size()>0 && (status.contains(Status.COMPLETED.name()) || status.contains(Status.PARTIAL_COMPLETED.name()))) {
							List<SubReportDataDto> filteredReports = subReports.stream()
									.filter(reportData -> reportData.getStatus() != null
											&&  status.contains(reportData.getStatus().name())
											&& (ignoreSubReports == null || ignoreSubReports.isEmpty() || !ignoreSubReports.contains(reportData.getReportName())))
											
									.map(reportData -> new SubReportDataDto(reportData.getId(),
											reportData.getReportName(), reportData.getCreatedAt(),
											reportData.getStatus(), reportData.getErrorCount()))
									.collect(Collectors.toList());

							dto.setSubReportData(filteredReports);
							dto.setTotalNoOfReport((long) filteredReports.size());
						} else {
							List<SubReportDataDto> listOfsubReportData = subReports.stream()
									.map(reportData -> new SubReportDataDto(reportData.getId(),
											reportData.getReportName(), reportData.getCreatedAt(),
											reportData.getStatus(), reportData.getErrorCount()))
									.collect(Collectors.toList());
							dto.setSubReportData(listOfsubReportData);
							dto.setTotalNoOfReport((long) listOfsubReportData.size());
						}
					}
					dto.setCreatedAt(fileResponse.getCreatedAt());
					dto.setUpdatedAt(fileResponse.getUpdatedAt());
					responseData.add(dto);
				});
			}

			PaginatedResponseDto<Object> paginatedResponseDto = new PaginatedResponseDto<>(
					parentReportLazyRepository.count(), responseData.size(), findAll.getTotalPages(), pageNo,
					responseData);
			return new Response<>(HttpStatus.OK.value(), "Excel File List.", paginatedResponseDto);
		} catch (Exception e) {
			return new Response<>(HttpStatus.OK.value(), "Error occurred while processing excel file list", null);
		}
	}

	@Override
	public Response<?> getParentReportByid(Long parentReportId) {
		Optional<ParentReportData> findById = parentReportRepository.findById(parentReportId);

		if (findById.isPresent()) {
			ParentReportDataDTO parentReportDto = ParentReportDataDTO.convertEntityToDto(findById.get());
//			parentReportDto.setRequestData(ConversionUtility
//					.convertObjectFormulaValidation(ConversionUtility.convertByteToObject(findById.get().getRequestData())));
			parentReportDto.setRequestData(ConversionUtility.convertByteToObject(findById.get().getRequestData()));
			return new Response<>(200, "Parent Report Found", parentReportDto);
		}
		return new Response<>(400, "Parent Report Not found", null);
	}

//	@Override
//	public List<ErrorResponseDto> validateParentResponseData(String jsonString) {
//
//		Workbook workbook = ExcelFormulaEvaluator.excelGenerateObjectFromJSON(jsonString, true);
//
//		jsonString = jsonString.replace("\\\"", "\"");
//		byte[] bytes = jsonString.getBytes();
//		String requestData = new String(bytes);
//		JSONArray requestJsonArray = new JSONArray(requestData);
//		List<ErrorResponseDto> validationResponseList = new ArrayList<>();
//
//		if (requestJsonArray != null && requestJsonArray.length() > 0) {
//			for (Object requestObject : requestJsonArray) {
//				JSONObject requestSheetData = new JSONObject(requestObject.toString());
//				if (requestSheetData.has("sheetName")) {
//					String sheetName = requestSheetData.getString("sheetName");
//					if (requestSheetData.has("sheetData")) {
//						JSONArray rowDataArray = requestSheetData.getJSONArray("sheetData");
//
//						if (rowDataArray != null && rowDataArray.length() > 0) {
//							for (Object requestRowObject : rowDataArray) {
//								JSONObject requestRowDataObject = new JSONObject(requestRowObject.toString());
//								if (requestRowDataObject.has("rowData")) {
//									JSONArray cellDataArray = requestRowDataObject.getJSONArray("rowData");
//
//									if (cellDataArray != null && cellDataArray.length() > 0) {
//										for (Object cellObj : cellDataArray) {
//											JSONObject requestCellObject = new JSONObject(cellObj.toString());
//											long uniqueId = requestCellObject.getLong("uniqueId");
//											String cellName = requestCellObject.getString("cellName");
//											Boolean isAppend = false;
//											StringBuilder message = new StringBuilder();
//											ErrorResponseDto errorResponseDto = new ErrorResponseDto();
//
//											if (requestCellObject.has("cellDetails")) {
//												if (!requestCellObject.get("cellDetails").equals("")) {
//													JSONObject existingCellDetails = requestCellObject
//															.getJSONObject("cellDetails");
//													if (existingCellDetails.has("hasFormula")) {
//														boolean hasFormula = existingCellDetails
//																.getBoolean("hasFormula");
//
//														if (hasFormula) {
//															String formula = existingCellDetails.getString("formula");
//
//															if (formula.equals("")) {
//
//																errorResponseDto.setUniqueId(uniqueId);
//																errorResponseDto.setSheetName(sheetName);
//																errorResponseDto.setCellName(cellName);
//																message.append(
//																		"Formula can not empty if hasFormula is true."
//																				+ formula
//																				+ ",Error in formula in sheet "
//																				+ sheetName + ", cellName " + cellName);
//																errorResponseDto.setErrorMessage(message.toString());
//																isAppend = true;
//
////																String errorMessage = "Formula can not empty if hasFormula is true."
////																		+ formula + ",Error in formula in sheet "
////																		+ sheetName + ", cellName " + cellName;
//
////																validationResponseList.add(new ErrorResponseDto(
////																		uniqueId, sheetName, cellName, errorMessage));
//															} else {
//
//																boolean excelFormula = ExcelValidation
//																		.isValidExcelFormula(workbook,
//																				workbook.getSheet(sheetName), formula);
//
//																if (!excelFormula) {
//																	errorResponseDto.setUniqueId(uniqueId);
//																	errorResponseDto.setSheetName(sheetName);
//																	errorResponseDto.setCellName(cellName);
//																	message.append("Error in formula in sheet "
//																			+ sheetName + ", cellName " + cellName
//																			+ " with the Error Formula : " + formula);
//																	errorResponseDto
//																			.setErrorMessage(message.toString());
//																	isAppend = true;
//
////																	String errorMessage = "Error in formula in sheet "
////																			+ sheetName + ", cellName " + cellName
////																			+ " with the Error Formula : " + formula;
////																	validationResponseList
////																			.add(new ErrorResponseDto(uniqueId,
////																					sheetName, cellName, errorMessage));
//																}
//
//															}
//														}
//													}
//
//													if (existingCellDetails.has("hasValidation")) {
//														Boolean validation = existingCellDetails
//																.getBoolean("hasValidation");
//														if (validation) {
//															if (existingCellDetails.has("validation")) {
//																String validationString = existingCellDetails
//																		.getString("validation");
//																if (!validationString.isEmpty()
//																		&& !validationString.equals("")) {
//																	Response<?> isValid = ExcelValidation
//																			.validateCellData(validationString);
//																	if (!isValid.getBooleanValue()) {
//																		if (isAppend) {
//																			message.append(isValid.getMessage());
//																		} else {
//																			ErrorResponseDto cellValidationErrorDto = new ErrorResponseDto();
//																			cellValidationErrorDto
//																					.setUniqueId(uniqueId);
//																			cellValidationErrorDto
//																					.setSheetName(sheetName);
//																			cellValidationErrorDto
//																					.setCellName(cellName);
//																			message.append(isValid.getMessage());
//																			validationResponseList
//																					.add(cellValidationErrorDto);
//																		}
//																	}
//
//																} else {
//																	if (isAppend) {
//																		message.append("Validation is mandatory.");
//																		errorResponseDto
//																				.setErrorMessage(message.toString());
//																		validationResponseList.add(errorResponseDto);
//																	} else {
//																		ErrorResponseDto cellValidationErrorDto = new ErrorResponseDto();
//																		cellValidationErrorDto.setUniqueId(uniqueId);
//																		cellValidationErrorDto.setSheetName(sheetName);
//																		cellValidationErrorDto.setCellName(cellName);
//																		message.append("Validation is mandatory.");
//																		validationResponseList
//																				.add(cellValidationErrorDto);
//																	}
//																}
//															} else {
//																if (isAppend) {
//																	message.append(
//																			", and Validation key is missing in cell details");
//																	errorResponseDto
//																			.setErrorMessage(message.toString());
//																	validationResponseList.add(errorResponseDto);
//																} else {
//																	ErrorResponseDto cellValidationErrorDto = new ErrorResponseDto();
//																	cellValidationErrorDto.setUniqueId(uniqueId);
//																	cellValidationErrorDto.setSheetName(sheetName);
//																	cellValidationErrorDto.setCellName(cellName);
//																	message.append(
//																			", and Validation key is missing in cell details");
//																	validationResponseList.add(cellValidationErrorDto);
//
//																}
//															}
//
//														} else {
//															if (!message.toString().isEmpty()) {
//																validationResponseList.add(errorResponseDto);
//															}
//														}
//													}
//
//												}
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return validationResponseList;
//	}

}
