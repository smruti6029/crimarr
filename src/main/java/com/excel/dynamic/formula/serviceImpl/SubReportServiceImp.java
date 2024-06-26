package com.excel.dynamic.formula.serviceImpl;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.excel.dynamic.formula.dto.CheckBreakKeyStatus;
import com.excel.dynamic.formula.dto.ErrorResponseDto;
import com.excel.dynamic.formula.dto.ExtractInterReportValueFromreport;
import com.excel.dynamic.formula.dto.FormulaUpdateDto;
import com.excel.dynamic.formula.dto.ParentReportDataDTO;
import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.dto.SubReportDataDto;
import com.excel.dynamic.formula.dto.SubReportSaveRequestDto;
import com.excel.dynamic.formula.dto.UpdateFormulaSheetNameDto;
import com.excel.dynamic.formula.enums.CustomCellType;
import com.excel.dynamic.formula.enums.DateFormat;
import com.excel.dynamic.formula.enums.Status;
import com.excel.dynamic.formula.model.Configuration;
import com.excel.dynamic.formula.model.ParentReportData;
import com.excel.dynamic.formula.model.SubReportData;
import com.excel.dynamic.formula.repository.ConfigurationRepository;
import com.excel.dynamic.formula.repository.ParentReportRepository;
import com.excel.dynamic.formula.repository.SubReportRepository;
import com.excel.dynamic.formula.service.SubReportService;
import com.excel.dynamic.formula.util.ConversionUtility;
import com.excel.dynamic.formula.util.ExcelConversion;
import com.excel.dynamic.formula.util.ExcelFormulaEvaluator;
import com.excel.dynamic.formula.util.ExcelValidation;
import com.excel.dynamic.formula.util.FormulaCellReferenceExtractor;
import com.excel.dynamic.formula.util.IfConditionChecking;
import com.excel.dynamic.formula.util.TypeValidation;
import com.excel.dynamic.formula.util.ValidConditionCheck;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

@Service
public class SubReportServiceImp implements SubReportService {

	@Autowired
	private SubReportRepository subReportRepository;

	@Autowired
	private ParentReportRepository parentReportRepository;

	@Autowired
	private TypeValidation typeValidation;

	@Autowired
	private ConfigurationRepository configurationRepository;

	@Override
	public Response<?> saveSubReportRequestData(SubReportSaveRequestDto subReportSaveRequestDto) {
		if (subReportSaveRequestDto.getIsCheck() == null) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "The 'isCheck' key value is required", null);

		}
		if (subReportSaveRequestDto.getIsCheck()) {

			if (subReportSaveRequestDto.getParentReportId() == null) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent Report ID is missing", null);
			}
			Optional<ParentReportData> parentReportObject = parentReportRepository
					.findById(subReportSaveRequestDto.getParentReportId());
			if (!parentReportObject.isPresent()) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent report not found", null);
			}

			try {

				SubReportData subReport = new SubReportData();
				if (subReportSaveRequestDto.getSubReportId() != null) {
					Optional<SubReportData> subReportFromDb = subReportRepository.findByIdAndParentId(
							subReportSaveRequestDto.getSubReportId(), subReportSaveRequestDto.getParentReportId());
					if (subReportFromDb.isPresent()) {

						if (subReportFromDb.get().getStatus() != null
								&& subReportFromDb.get().getStatus().name().equals(Status.COMPLETED.name())) {
							return new Response<>(HttpStatus.BAD_REQUEST.value(),
									"You can't Update Subreport as The Status Is Completed", null);
						}
						subReport = subReportFromDb.get();
					} else {
						return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent Id and Sub report id is missing",
								null);
					}
				} else {

					if (subReportSaveRequestDto.getSubReportName() == null
							|| (subReportSaveRequestDto.getSubReportName() != null
									&& subReportSaveRequestDto.getSubReportName().trim().equals(""))) {
						return new Response<>(HttpStatus.BAD_REQUEST.value(), "Sub Report Name is Required", null);
					}

//					Optional<SubReportData> subReportData = subReportRepository.findByParentReportIdAndSubReportName(
//							subReportSaveRequestDto.getParentReportId(), subReportSaveRequestDto.getSubReportName());
					Optional<SubReportData> subReportData = subReportRepository
							.findByReportName(subReportSaveRequestDto.getSubReportName());
					if (subReportData.isPresent()) {
						return new Response<>(HttpStatus.BAD_REQUEST.value(), "Sub Report Name is already exist", null);
					}

					Optional<ParentReportData> parentRepotData = parentReportRepository
							.findByExcelFileName(subReportSaveRequestDto.getSubReportName());

					if (parentRepotData.isPresent()) {
						return new Response<>(HttpStatus.BAD_REQUEST.value(), "Subreport name should be unique !!",
								null);

					}
				}

				JsonElement element = JsonParser.parseString(subReportSaveRequestDto.getRequestData());
				List<ErrorResponseDto> errorMessageList = subReportSaveRequestDto.getErrorMessageList();

				JsonArray sheetsArray = element.getAsJsonArray();

				if (subReportSaveRequestDto.getSubReportId() == null) {
					subReport.setCreatedAt(new Date());
					subReport.setReportName(subReportSaveRequestDto.getSubReportName());
					subReport.setRequestObject(sheetsArray.toString().getBytes());
					subReport.setParentReportData(new ParentReportData(subReportSaveRequestDto.getParentReportId()));
					subReport.setIsActive(true);
					subReport.setUpdatedAt(new Date());
				} else {
					subReport.setRequestObject(sheetsArray.toString().getBytes());
					subReport.setParentReportData(new ParentReportData(subReportSaveRequestDto.getParentReportId()));
					subReport.setUpdatedAt(new Date());
				}

				if (subReportSaveRequestDto.getErrorCount() != null && subReportSaveRequestDto.getErrorCount() != 0) {
					subReport.setErrorCount(subReportSaveRequestDto.getErrorCount());
					ObjectMapper objectMapper = new ObjectMapper();

					String jsonString;
					String allParentNames;
					try {

						jsonString = objectMapper.writeValueAsString(errorMessageList);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						jsonString = "";
					}

					try {

						if (subReportSaveRequestDto.getAllParentUsingInInterReport() != null
								&& subReportSaveRequestDto.getAllParentUsingInInterReport().size() > 0) {
							allParentNames = objectMapper
									.writeValueAsString(subReportSaveRequestDto.getAllParentUsingInInterReport());

							subReport.setAllParentReportUsnigInInterReport(allParentNames);

						}
					} catch (JsonProcessingException e) {
						e.printStackTrace();

					}
					if (subReportSaveRequestDto.getIspartialCompleted() != null
							&& subReportSaveRequestDto.getIspartialCompleted()) {
						if (subReportSaveRequestDto.getErrorCount() != subReportSaveRequestDto
								.getInterReportUsingFormulaErrorCount()) {
							return new Response<>(HttpStatus.BAD_REQUEST.value(),
									"PARTIALLY_COMPLETED: Only InterReport error is present", null);

						} else {
							subReport.setErrorMessageList(jsonString);
							subReport.setStatus(Status.PARTIAL_COMPLETED);
						}

					} else {
						subReport.setErrorMessageList(jsonString);
						subReport.setStatus(Status.PENDING);
					}
				} else {
					subReport.setErrorCount(0L);
					subReport.setAllParentReportUsnigInInterReport(null);
					subReport.setErrorMessageList(null);
					subReport.setStatus(Status.COMPLETED);
				}
				subReportRepository.save(subReport);
				if (subReportSaveRequestDto.getSubReportId() != null) {
					return new Response<>(HttpStatus.OK.value(), "SubReport Updated successfully.", null);
				}
				return new Response<>(HttpStatus.OK.value(), "SubReport saved successfully.", null);

			} catch (Exception e) {
				e.printStackTrace();
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent report not found", null);
			}
		} else {
			return validSubReportData(subReportSaveRequestDto);
		}

	}

	@Override
	public Response<?> getSubReportByid(Long subreportId) {
		Optional<SubReportData> findById = subReportRepository.findById(subreportId);

		if (findById.isPresent()) {
			SubReportData subReportData = findById.get();
			SubReportDataDto dataDto = new SubReportDataDto();
			dataDto.setReportName(subReportData.getReportName());
			dataDto.setId(subReportData.getId());
			dataDto.setCreatedAt(subReportData.getCreatedAt());
			dataDto.setStatus(subReportData.getStatus());

			List<ErrorResponseDto> errorResponseDtos = new ArrayList<>();
			List<String> allParentUsingInInterReport = new ArrayList<>();

			ObjectMapper objectMapper = new ObjectMapper();
			if (subReportData.getErrorCount() != null && subReportData.getErrorCount() > 0) {
				// Convert the JSON string to a List of ErrorResponseDto objects

				try {
					errorResponseDtos = objectMapper.readValue(subReportData.getErrorMessageList(),
							new TypeReference<List<ErrorResponseDto>>() {
							});
				} catch (JsonProcessingException e) {
					// Handle exception
					e.printStackTrace();
					errorResponseDtos = null; // or any default value
				}

			}

			if (subReportData.getAllParentReportUsnigInInterReport() != null) {

				try {
					allParentUsingInInterReport = objectMapper.readValue(
							subReportData.getAllParentReportUsnigInInterReport(), new TypeReference<List<String>>() {
							});
					dataDto.setAllParentUsingInInterReport(allParentUsingInInterReport);
				} catch (JsonProcessingException e) {

				}

			}

			dataDto.setErrorMessageList(errorResponseDtos);
			dataDto.setErrorCount(subReportData.getErrorCount());
			ParentReportDataDTO parentReportDataDTO = new ParentReportDataDTO();

			parentReportDataDTO.setCreatedAt(subReportData.getParentReportData().getCreatedAt());
			parentReportDataDTO.setExcelFileName(subReportData.getParentReportData().getExcelFileName());
			parentReportDataDTO.setUpdatedAt(subReportData.getParentReportData().getUpdatedAt());
			parentReportDataDTO.setId(subReportData.getParentReportData().getId());
//			parentReportDataDTO
//					.setRequestData(convertByteToObject(subReportData.getParentReportData().getRequestData()));
//			dataDto.setParentReportDataDTO(parentReportDataDTO);
//			dataDto.setRequestObject(ConversionUtility
//					.convertObjectFormulaValidation(ConversionUtility.convertByteToObject(subReportData.getRequestObject())));

			dataDto.setRequestObject(ConversionUtility.convertByteToObject(subReportData.getRequestObject()));

			return new Response<>(200, "Report Found", dataDto);

		}

		return new Response<>(400, "No Report Found", null);
	}

	@Override
	public Response<?> validSubReportData(SubReportSaveRequestDto subReportSaveRequestDto) {

		if (subReportSaveRequestDto.getParentReportId() == null) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent Report ID is missing", null);
		}
		Optional<ParentReportData> parentReportObject = parentReportRepository
				.findById(subReportSaveRequestDto.getParentReportId());
		if (!parentReportObject.isPresent()) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Parent report not found", null);
		}
		Response<?> response = validateSubReportData(subReportSaveRequestDto.getRequestData(),
				new Gson().toJsonTree(ConversionUtility.convertByteToObject(parentReportObject.get().getRequestData())),
				subReportSaveRequestDto, parentReportObject.get().getExcelFileName());
		return response;

	}

	public Response<?> getParentObjectMap(JsonElement parentResponseString) {

		HashMap<String, Object> parentObjectMap = new HashMap<>();
		JsonArray jsonArray = parentResponseString.getAsJsonArray();
		Set<String> parentSheetNames = new HashSet<>();
		for (JsonElement element : jsonArray) {
			String sheetName = element.getAsJsonObject().get("sheetName").getAsString();
			parentSheetNames.add(sheetName);
			JsonArray sheetData = element.getAsJsonObject().getAsJsonArray("sheetData");
			for (JsonElement sheetDatum : sheetData) {
				JsonArray rowData = sheetDatum.getAsJsonObject().getAsJsonArray("rowData");
				for (JsonElement rowDatum : rowData) {
					String cellName = rowDatum.getAsJsonObject().get("cellName").getAsString();
					String key = sheetName + cellName;
					parentObjectMap.put(key, rowDatum);
				}
			}
		}
		return new Response<>(HttpStatus.OK.value(), parentObjectMap, parentSheetNames);
	}

	public HashMap<String, Object> getSubReportObject(JsonElement jsonElement) {

		JsonArray sheetsArray = jsonElement.getAsJsonArray();
		HashMap<String, Object> subObjectMap = new HashMap<>();
		for (JsonElement element1 : sheetsArray) {
			String sheetName = element1.getAsJsonObject().get("sheetName").getAsString();
			JsonArray sheetData = element1.getAsJsonObject().getAsJsonArray("sheetData");
			for (JsonElement sheetDatum : sheetData) {
				JsonArray rowData = sheetDatum.getAsJsonObject().getAsJsonArray("rowData");
				for (JsonElement rowDatum : rowData) {
					String cellName = rowDatum.getAsJsonObject().get("cellName").getAsString();
					String key = sheetName + "!" + cellName;
					subObjectMap.put(key, rowDatum);
				}
			}
		}
		return subObjectMap;

	}

	public Response<?> getFormulaUpdateForSubReport(String subReportString, HashMap<String, Object> parentObjectMap,
			SubReportSaveRequestDto subReportSaveRequestDto, Set<String> parentSheetNames,
			String continueDynamicRowColour, String breakDynamicRowColour, String parentName) {

		try {

//			HashMap<String, Object> parentObjectMap = new HashMap<>();
//			JsonArray jsonArray = parentResponseString.getAsJsonArray();
//			Set<String> parentSheetNames = new HashSet<>();
//			for (JsonElement element : jsonArray) {
//				String sheetName = element.getAsJsonObject().get("sheetName").getAsString();
//				parentSheetNames.add(sheetName);
//				JsonArray sheetData = element.getAsJsonObject().getAsJsonArray("sheetData");
//				for (JsonElement sheetDatum : sheetData) {
//					JsonArray rowData = sheetDatum.getAsJsonObject().getAsJsonArray("rowData");
//					for (JsonElement rowDatum : rowData) {
//						String cellName = rowDatum.getAsJsonObject().get("cellName").getAsString();
//						String key = sheetName + cellName;
//						parentObjectMap.put(key, rowDatum);
//					}
//				}
//			}

//			String jsonString = ConversionUtility.convertStringToJson(subReportString);
//			Workbook workbook = ExcelFormulaEvaluator.excelGenerateObjectFromJSON(jsonString, false);
			JsonElement element = JsonParser.parseString(subReportString);
			JsonArray sheetsArray = element.getAsJsonArray();

			long subSheetCount = 0;
			HashMap<String, Object> subObjectMap = new HashMap<>();
			for (JsonElement element1 : sheetsArray) {
				String sheetName = element1.getAsJsonObject().get("sheetName").getAsString();
				subSheetCount++;

				if (!parentSheetNames.contains(sheetName)) {
					return new Response<>(HttpStatus.BAD_REQUEST.value(),
							"Parent report is not match with the sub report", null, true);
				}

				JsonArray sheetData = element1.getAsJsonObject().getAsJsonArray("sheetData");
				for (JsonElement sheetDatum : sheetData) {
					JsonArray rowData = sheetDatum.getAsJsonObject().getAsJsonArray("rowData");
					for (JsonElement rowDatum : rowData) {
						String cellName = rowDatum.getAsJsonObject().get("cellName").getAsString();
						String key = sheetName + "!" + cellName;
						subObjectMap.put(key, rowDatum);
					}
				}
			}

			if (subSheetCount != parentSheetNames.size()) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Parent report is not match with the sub report . Provide Valid SubReport", null, true);
			}

			Map<String, CheckBreakKeyStatus> checkBreakStatus = new ConcurrentHashMap<>();
			Map<String, FormulaUpdateDto> subReportFormulaUpdate = new ConcurrentHashMap<>();

			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			// Iterate sheetArray data

			Set<String> allSheetName = new HashSet<>();

			for (JsonElement allSheeetNameextract : sheetsArray) {
				if (allSheeetNameextract != null) {
					JsonObject sheetObject = allSheeetNameextract.getAsJsonObject();
					String sheetName = sheetObject.get("sheetName").getAsString();
					allSheetName.add(sheetName);

				}
			}

			for (JsonElement sheet : sheetsArray) {

				executor.submit(() -> {

					int countContinueForRow = 0;
					int countRowFromFormula = 0;
					int subReportCount = 0;
					Integer parentreportIndex = null;
					Boolean checkParentRport = true;
					String isBreakKey = "";
					Integer countContinueCell = 0;
					boolean checkFormula = true;

					int continueRowBreakAt = 0;
					Set<String> allBreakKeys = new HashSet<>();

					int count = 0;
					Set<Integer> allContinueRowNumber = new TreeSet<>();

					// Check sheet element is not null
					if (sheet != null) {
						JsonObject sheetObject = sheet.getAsJsonObject();
						String sheetName = sheetObject.get("sheetName").getAsString();
//						allSheetName.add(sheetName);
						JsonArray rowsArray = sheetObject.getAsJsonArray("sheetData");

						int sequence = 0;

						// Iterate rowArray data
						for (JsonElement rowData : rowsArray) {

							subReportCount++;

							if (countContinueCell > 0) {
								checkParentRport = false;
							} else {
								checkParentRport = true;
							}

							// Check rowData is not null
							if (rowData != null) {
								JsonObject rowObject = rowData.getAsJsonObject();
								JsonArray cellArray = rowObject.getAsJsonArray("rowData");
								int rowNumber = rowObject.get("rowNumber").getAsInt();
								int countRowforFormula = 0;
								boolean isCountCheck = true;
								// Iterate cellArray data
								for (JsonElement cell : cellArray) {

									boolean isFormulaChanged = false;
									FormulaUpdateDto formulaUpdateDto = new FormulaUpdateDto();

									JsonObject cellObject = cell.getAsJsonObject();
									JsonObject cellDetalis = cellObject.get("cellDetails").getAsJsonObject();

									Boolean isAppend = false;

									Long uniqueId = cellObject.get("uniqueId").getAsLong();
									String cellName = cellObject.get("cellName").getAsString();
									ErrorResponseDto errorResponseDto = new ErrorResponseDto();
									errorResponseDto.setCellName(cellName);
									errorResponseDto.setSheetName(sheetName);
									errorResponseDto.setUniqueId(uniqueId);
									String headerName = cellObject.get("headerName").getAsString();
									JsonObject parentObject = new JsonObject();

									StringBuilder message = new StringBuilder();
									int currentRowIndex = Integer.parseInt(cellName.replaceAll("\\D", ""));
									JsonObject cellDetailsObjects = cellObject != null
											? cellObject.getAsJsonObject("cellDetails")
											: null;

									String subCellName = cellName;

									headerName = (cellDetailsObjects != null && cellDetailsObjects.has("value")
											&& !cellDetailsObjects.get("value").isJsonNull())
													? cellDetailsObjects.get("value").getAsString()
													: headerName;

									// Update parent Call Bu dynamic
									String replaceCellNameForParent = "";

									CheckBreakKeyStatus breakKeyStatus = checkBreakStatus
											.get(sheetName + "-" + cellName) != null
													? checkBreakStatus.get(sheetName + "-" + cellName)
													: new CheckBreakKeyStatus();

									String cellnameForParent = cellName;

//								New Parent call Logic
									if (checkParentRport) {

										if (countContinueForRow != 0) {
											int indexOfCell;
											if (countContinueForRow > 0) {
												indexOfCell = subReportCount - (countContinueForRow);
											} else {
												indexOfCell = subReportCount;
											}
											cellnameForParent = cellName.replaceAll("[^A-Za-z]", "");
											cellnameForParent += indexOfCell;
										}
										parentObject = (JsonObject) parentObjectMap.get(sheetName + cellnameForParent);

										JsonObject parentCellDetalis = parentObject != null
												&& parentObject.has("cellDetails")
														? parentObject.get("cellDetails").getAsJsonObject()
														: null;

//									cellName = cellnameForParent;

//										if (parentCellDetalis != null) {
//											if (parentCellDetalis.has("bgColor")
//													&& !parentCellDetalis.get("bgColor").getAsString().equals("")
//													&& parentCellDetalis.get("bgColor").getAsString()
//															.equals("#FFC000")) {

										if (parentCellDetalis != null) {
											if (parentCellDetalis.has("bgColor")
													&& !parentCellDetalis.get("bgColor").getAsString().equals("")
													&& ExcelConversion
															.getColorFamily(
																	parentCellDetalis.get("bgColor").getAsString())
															.equals(continueDynamicRowColour)) {

//											isBreakKey = parentObject.has("isBreakKey")
//													? parentObject.get("isBreakKey").getAsString()
//													: "";

//												isBreakKey = "#C0C0C0";
												isBreakKey = breakDynamicRowColour;
												breakKeyStatus.setBreakKey(isBreakKey);
												breakKeyStatus.setColumnName(cellnameForParent);
												breakKeyStatus.setStartIngCell(cellnameForParent);
												breakKeyStatus.setStatus(false);
												breakKeyStatus.setStartingRowNumber(rowNumber);
												countContinueCell++;
												parentreportIndex = Integer
														.parseInt(cellnameForParent.replaceAll("\\D", ""));
												allContinueRowNumber.add(parentreportIndex);

											}
										}

									} else {
										replaceCellNameForParent = cellName.replaceAll("[^A-Za-z]", "");
										replaceCellNameForParent = replaceCellNameForParent + parentreportIndex;
										cellnameForParent = replaceCellNameForParent;
										parentObject = (JsonObject) parentObjectMap
												.get(sheetName + replaceCellNameForParent);

									} // new Parent Get Here

//								System.err.println(cellName);

									if (parentreportIndex != null) {
										String parentCellName = cellName.replaceAll("[^A-Za-z]", "")
												+ parentreportIndex;

										if (!cellName.equals(parentCellName) && !parentCellName.equals("")
												&& !checkParentRport) {
											if (breakKeyStatus.getBreakKey() == null
													&& breakKeyStatus.getColumnName() == null) {
												breakKeyStatus = checkBreakStatus
														.get(sheetName + "-" + parentCellName) != null
																? checkBreakStatus.get(sheetName + "-" + parentCellName)
																: new CheckBreakKeyStatus();
											}
										}
									}

									JsonObject parentCellDetails = null;
									boolean hasParentFormula = false;
									String parentFormula = null;
									boolean hasParentCellDetails = false;

									if (parentObject != null && parentObject.has("cellDetails")
											&& parentObject.get("cellDetails") != null
											&& parentObject.get("cellDetails").isJsonObject()) {
										hasParentCellDetails = true;
										parentCellDetails = parentObject.get("cellDetails").getAsJsonObject();
										if (parentCellDetails != null && parentCellDetails.has("hasFormula")) {
											hasParentFormula = parentCellDetails.has("hasFormula")
													? !"false".equalsIgnoreCase(
															parentCellDetails.get("hasFormula").getAsString())
													: false;
											if (hasParentFormula) {
												parentFormula = parentCellDetails.get("formula").getAsString();
											}
										}
									}

									// Break The Column By the Color Key
									if (countContinueCell != 0 && hasParentCellDetails
											&& parentCellDetails.has("bgColor")
											&& !parentCellDetails.get("bgColor").getAsString().equals("")) {

										String subBreakColur = cellDetailsObjects != null
												&& cellDetailsObjects.has("bgColor")
														? cellDetailsObjects.get("bgColor").getAsString()
														: "";
										subBreakColur = ExcelConversion.getColorFamily(subBreakColur);

										if ((allBreakKeys == null || allBreakKeys.size() == 0)
												|| !allBreakKeys.contains(cellnameForParent)) {

											if (breakKeyStatus.getColumnName() != null
													&& subBreakColur.equals(breakDynamicRowColour)
													&& breakKeyStatus.getColumnName().replaceAll("[^A-Za-z]", "")
															.equals(subCellName.replaceAll("[^A-Za-z]", ""))) {

												//
												allBreakKeys.add(cellName);
												breakKeyStatus.setStatus(true);
												countContinueCell = 0;
//											System.out.println("BREAK WHERE : " + cellName);

												int indexNumber = Integer.parseInt(cellName.replaceAll("\\D", ""));
												if (countContinueCell == 0) {
													continueRowBreakAt = indexNumber;
												}
												if (indexNumber != 1) {
													indexNumber -= 1;
												}

												breakKeyStatus.setEndingIngCell(
														cellName.replaceAll("[^A-Za-z]", "") + indexNumber);
												breakKeyStatus.setEndingRowNumber(rowNumber);

												countContinueForRow--;
												checkBreakStatus.put(sheetName + "-" + breakKeyStatus.getColumnName(),
														breakKeyStatus);

												for (String breakStatusMapKey : checkBreakStatus.keySet()) {

													if (breakStatusMapKey.contains(sheetName)) {
														CheckBreakKeyStatus checkBreakKeyStatus = checkBreakStatus
																.get(breakStatusMapKey);

														if (checkBreakKeyStatus.getColumnName() != null
																&& checkBreakKeyStatus.getStartIngCell() != null
																&& checkBreakKeyStatus.getEndingIngCell() == null) {
															checkBreakKeyStatus.setEndingIngCell(
																	cellName.replaceAll("[^A-Za-z]", "") + indexNumber);
															checkBreakStatus.put(breakStatusMapKey,
																	checkBreakKeyStatus);
														}

													}

												}
												countRowFromFormula = 0;
												sequence = 0;
												break;
											}

										}

										if (breakKeyStatus.getColumnName() != null) {
											checkBreakStatus.put(sheetName + "-" + breakKeyStatus.getColumnName(),
													breakKeyStatus);
										}
									}

									boolean isIntersectionFormula = false;
									boolean isFormulaGenerate = false;
									if (hasParentFormula && parentFormula != null) {

										if (parentCellDetails.has("isInterReportFormula")
												&& parentCellDetails.get("isInterReportFormula").getAsBoolean()) {

											JsonObject parentReportSubReportMap = null;
											JsonObject subReportUpdateParentReportSubReportMap = null;

											if (cellDetalis.has("isInterSubreportNotSelected")
													&& cellDetalis.has("formula")) {
												parentFormula = cellDetalis.get("formula").getAsString();
												isFormulaGenerate = true;

											}

											ExtractInterReportValueFromreport extractInterReportValueFromreport2 = extractInterReportValueFromreport(
													parentFormula, parentReportSubReportMap,
													subReportUpdateParentReportSubReportMap, parentName);

											formulaUpdateDto = extractInterReportValueFromreport2.getFormulaUpdateDto();
											formulaUpdateDto.setInterReportParentFormula(parentFormula);

											SimpleEntry<String, Map<Integer, String>> extractInterReportValueFromreport = extractInterReportValueFromreport2
													.getModifiyFormulaAndInterReportCellAndValue();
											parentFormula = extractInterReportValueFromreport.getKey();

											formulaUpdateDto.setInterReportCellAndValue(
													extractInterReportValueFromreport.getValue());
											;

										}

										for (String sheetNameCheck : allSheetName) {
											if (parentFormula.contains(sheetNameCheck)) {
												isIntersectionFormula = true;
												break;
											}
										}
									}

									if ((countContinueCell != 0) && (hasParentFormula && parentFormula != null)
											&& !isIntersectionFormula && !isFormulaGenerate) {

										int findMatchingRowNumber = findMatchingRowNumberV2(cellnameForParent,
												allContinueRowNumber);
										Integer valueByRowNumber = 0;
										if (findMatchingRowNumber != 0) {
											valueByRowNumber = getValueByRowNumber(checkBreakStatus,
													findMatchingRowNumber, sheetName);

										}

										if (isCountCheck) {
											parentFormula = FormulaCellReferenceExtractor.adjustFormula(parentFormula,
													countRowFromFormula + valueByRowNumber);

											isCountCheck = false;
										} else {
											parentFormula = FormulaCellReferenceExtractor.adjustFormula(parentFormula,
													countRowFromFormula + valueByRowNumber);
										}

									}

									// Resize the (:) formula when the formula hava exiting Continue Cell
									boolean formulaCustomize = true;
									if (!isIntersectionFormula && countContinueCell == 0 && countContinueForRow > 0
											&& ((hasParentFormula && parentFormula != null)
													&& currentRowIndex != continueRowBreakAt)) {

										if (parentFormula.contains(":")) {
											List<String> components = FormulaCellReferenceExtractor
													.extractComponents(parentFormula);
											String[] cellsAndSheetNames = FormulaCellReferenceExtractor
													.extractCellsAndSheetNamesAfterColon(parentFormula);

											String sheetNameForMap = cellsAndSheetNames[0];
											String cellNameForMap = cellsAndSheetNames[1];
											if (sheetNameForMap.equals("")) {
												sheetNameForMap = sheetName;
											} else {
												cellNameForMap += sheetNameForMap;
											}

											CheckBreakKeyStatus checkBreakKeyStatus = checkBreakStatus
													.get(sheetNameForMap + "-" + cellsAndSheetNames[1]);

											;
											if (checkBreakKeyStatus != null
													&& checkBreakKeyStatus.getEndingIngCell() != null) {
												parentFormula = FormulaCellReferenceExtractor.updateEndingCell(
														components, cellNameForMap,
														checkBreakKeyStatus.getEndingIngCell());
												formulaCustomize = false;
//												System.out.println(parentFormula);
											}

										}
									}

									// Resize the formula (DIfference between CContinue Starting Row and Ending Row)
									if (!isIntersectionFormula && countContinueForRow > 0 && countContinueCell == 0
											&& checkParentRport && (hasParentFormula && parentFormula != null)
											&& formulaCustomize) {

//										System.out.println("Parentformula for customize" + parentFormula);

										if (allContinueRowNumber != null && allContinueRowNumber.size() > 0) {
//											List<String> extractOperandsAndOperators = FormulaCellReferenceExtractor
//													.extractOperandsAndOperators(parentFormula);

											List<String> extractOperandsAndOperators = FormulaCellReferenceExtractor
													.extractOperandsForInterReport(parentFormula);

											List<String> extractCellNames = FormulaCellReferenceExtractor
													.extractCellNamesForFormulaUpdate(parentFormula);

											Set<Integer> allIndex = new TreeSet<>();

											for (String cells : extractCellNames) {

												int findMatchingRowNumber = findMatchingRowNumber(cells,
														allContinueRowNumber);

												if (findMatchingRowNumber != 0) {
													Integer valueByRowNumber = getValueByRowNumber(checkBreakStatus,
															findMatchingRowNumber, sheetName);
													int rowNumberOfCell = Integer.parseInt(cells.replaceAll("\\D", ""));

													int indexOf = extractOperandsAndOperators.indexOf(cells);
													if (allIndex.size() == 0 || !allIndex.contains(indexOf)) {
														extractOperandsAndOperators.set(indexOf,
																(cells.replaceAll("[^A-Za-z]", "")
																		+ (rowNumberOfCell + valueByRowNumber)));
														allIndex.add(indexOf);
													} else if (allIndex.size() > 0) {
														List<String> dataAfterIndex = getDataAfterIndex(
																extractOperandsAndOperators, indexOf);
														int indexOfCell = extractOperandsAndOperators.indexOf(cells);

														extractOperandsAndOperators.set(indexOfCell,
																(cells.replaceAll("[^A-Za-z]", "")
																		+ (rowNumberOfCell + valueByRowNumber)));

													}
												}

											}
											parentFormula = extractOperandsAndOperators.stream()
													.collect(Collectors.joining());

										}

									}

									// formula extend by the Number

									if (hasParentFormula && parentFormula != null) {
										isFormulaChanged = true;

										formulaUpdateDto.setIsInterSheetFormula(isIntersectionFormula);
										formulaUpdateDto.setIsSubReportFormulaForInterReport(isFormulaGenerate);
										formulaUpdateDto.setSequence(sequence);
										formulaUpdateDto.setIsSequenceFormula(true);
										formulaUpdateDto.setIsChangedFormula(isFormulaChanged);
										formulaUpdateDto.setParentFormula(parentFormula);
										formulaUpdateDto.setSubCellName(subCellName);
										formulaUpdateDto.setParentRownumber(
												Integer.parseInt(cellnameForParent.replaceAll("\\D", "")));
										formulaUpdateDto.setRowNumber(rowNumber);
										subReportFormulaUpdate.put(sheetName + "-" + subCellName, formulaUpdateDto);
									}

//									} else {
//										formulaUpdateDto.setSequence(0);
//										formulaUpdateDto.setIsSequenceFormula(false);
//										formulaUpdateDto.setIsChangedFormula(false);
//										formulaUpdateDto.setParentFormula(parentFormula);
//										formulaUpdateDto.setSubCellName(subCellName);
//										formulaUpdateDto.setParentRownumber(
//												Integer.parseInt(cellnameForParent.replaceAll("\\D", "")));
//										formulaUpdateDto.setRowNumber(rowNumber);
//									}

//									JsonObject cellDetailsObject = null;
//
//									if (cellObject != null && cellObject.get("cellDetails") != null
//											&& (cellObject.get("cellDetails").isJsonObject())) {
//
//										cellDetailsObject = cellObject.get("cellDetails").getAsJsonObject();
//
//										if (cellDetailsObject != null) {
//
//											if (cellDetailsObject.has("isError")) {
//												cellDetailsObject.remove("isError");
//											}
//											if (cellDetailsObject.has("error")) {
//												cellDetailsObject.remove("error");
//											}
//
//										}
//
//										if (hasParentFormula && parentFormula != null && parentCellDetails != null
//												&& hasParentCellDetails) {
//
////											cellDetailsObject.addProperty("formulaPresentInParentCellName",
////													parentCellDetails.get("cellName").getAsString());
//
//										}
//
									if (countContinueCell == 0) {
//									       countRowFromFormula = 0;
										checkParentRport = true;
									}
//									}

								} // cellArray iteration end

								if (!isCountCheck) {
									countRowFromFormula++;
								}

							} // rowData not null check end
							if (countContinueCell != 0) {
								sequence++;
								countContinueForRow++;
							}

						} // rowArray iteration end

					} // sheet object not null check end
					allBreakKeys.clear();
				});// Sheet array iteration end
			}
			executor.shutdown();

			while (!executor.isTerminated()) {
				// Optionally, you can add a sleep here if you want to wait for the executor to
				// finish
				// Thread.sleep(1000); // Wait for 1 second
			}

			// Its USe for Customize By Dynamic Row for Formula

			Map<String, Integer> highestDifferenceByRowNumber = getHighestDifferenceByRowNumber(checkBreakStatus);

			for (String subReportKey : subReportFormulaUpdate.keySet()) {
				String sheetName = extractSheetName(subReportKey);

				FormulaUpdateDto formulaForUpdate = subReportFormulaUpdate.get(subReportKey);
				String parentFormula = formulaForUpdate.getParentFormula();

				// all continue Index In sheet

//				boolean isIntersectionFormula = false;
//				for (String sheetNameCheck : allSheetName) {
//					if (parentFormula.contains(sheetNameCheck)) {
//						isIntersectionFormula = true;
//						break;
//					}
//				}

//				if(formulaForUpdate.getIsSubReportSelected()!=null && formulaForUpdate.getIsSubReportSelected()) {
//					continue;
//				}

				if (formulaForUpdate.getIsInterSheetFormula()
						&& !formulaForUpdate.getIsSubReportFormulaForInterReport()) {
					List<String> extractComponents = extractOperandsAndOperators(parentFormula);

					for (String cellNameSheetName : extractComponents) {

//					System.out.println(cellNameSheetName);

						if (cellNameSheetName.contains("!")) {

							String[] extractCellsAndSheetNamesAfterColon = extractSheetAndCell(cellNameSheetName);
							String formulaSheetName = extractCellsAndSheetNamesAfterColon[0];
							String formulaCellName = extractCellsAndSheetNamesAfterColon[1];
							Set<Integer> alltheContinueIndexBysheetName2 = getAlltheContinueIndexBysheetName(
									formulaSheetName, highestDifferenceByRowNumber);
							int findMatchingRowNumber = findMatchingRowNumber(formulaCellName,
									alltheContinueIndexBysheetName2);

							if (findMatchingRowNumber != 0) {
								Integer valueByRowNumber = getValueByRowNumber(checkBreakStatus, findMatchingRowNumber,
										formulaSheetName);
								int rowNumberOfCell = Integer.parseInt(formulaCellName.replaceAll("\\D", ""));

								int indexOf = extractComponents.indexOf(formulaSheetName + "!" + formulaCellName);

								extractComponents.set(indexOf, (formulaSheetName + "!"
										+ formulaCellName.replaceAll("[^A-Za-z]", "")
										+ (rowNumberOfCell + valueByRowNumber + formulaForUpdate.getSequence())));

							} else {
								int indexOf = extractComponents.indexOf(formulaSheetName + "!" + formulaCellName);
								String repalceName = formulaCellName.replaceAll("[^A-Za-z]", "");
								int replaceRowNumber = Integer.parseInt(formulaCellName.replaceAll("\\D", ""));
								extractComponents.set(indexOf, (formulaSheetName + "!" + repalceName

										+ (replaceRowNumber + formulaForUpdate.getSequence())));

							}

						} else if (!cellNameSheetName.equals("") && isValidCellName(cellNameSheetName)) {

							Set<Integer> alltheContinueIndexBysheetName2 = getAlltheContinueIndexBysheetName(sheetName,
									highestDifferenceByRowNumber);
							int findMatchingRowNumber = findMatchingRowNumber(cellNameSheetName,
									alltheContinueIndexBysheetName2);

							if (findMatchingRowNumber != 0) {
								Integer valueByRowNumber = getValueByRowNumber(checkBreakStatus, findMatchingRowNumber,
										sheetName);
								int rowNumberOfCell = Integer.parseInt(cellNameSheetName.replaceAll("\\D", ""));

								int indexOf = extractComponents.indexOf(cellNameSheetName);

								extractComponents.set(indexOf, (cellNameSheetName.replaceAll("[^A-Za-z]", "")
										+ (rowNumberOfCell + valueByRowNumber)));
							}

						}

					}

					parentFormula = extractComponents.stream().collect(Collectors.joining());
					formulaForUpdate.setParentFormula(parentFormula);

				}

				subReportFormulaUpdate.put(subReportKey, formulaForUpdate);
			}

			UpdateFormulaSheetNameDto formulaSheetNameDto = new UpdateFormulaSheetNameDto();
			formulaSheetNameDto.setAllSheetName(allSheetName);
			formulaSheetNameDto.setSubReportFormulaUpdate(subReportFormulaUpdate);

			return new Response<>(HttpStatus.OK.value(), "Success", formulaSheetNameDto);
		} catch (

		Exception e) {
			e.printStackTrace();
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Error", null);
		}

	}

//	private String extractInterReportValueFromreport(String parentFormula) {
//
//		List<String> extractOperandsForInterReport = FormulaCellReferenceExtractor
//				.extractOperandsForInterReport(parentFormula);
//		
//		Map<String,String> allInterReportCellAndValue=new HashMap<>();
//
//		for (String operand : extractOperandsForInterReport) {
//
//			if (operand.contains("~")) {
//
//				String value = "0.0";
//				String[] extractTextInTildes = FormulaCellReferenceExtractor.extractTextInTildes(operand);
//				System.out.println("First part: " + extractTextInTildes[0]);
//				System.out.println("Second part: " + extractTextInTildes[1]);
//				String interReportName = extractTextInTildes[0];
//				String interReportCell = extractTextInTildes[1];
//
//				Optional<SubReportData> subReportdata = subReportRepository.findByReportName(interReportName);
//				if (subReportdata.isPresent() && subReportdata.get().getStatus().equals(Status.COMPLETED)) {
//
//					HashMap<String, Object> subReportObject = getSubReportObject(new Gson()
//							.toJsonTree(ConversionUtility.convertByteToObject(subReportdata.get().getRequestObject())));
//					JsonObject cellDetalisData = (JsonObject) subReportObject.get(interReportCell);
//
//					if (cellDetalisData.has("cellDetails")) {
//						JsonObject cellDetails = cellDetalisData.get("cellDetails").getAsJsonObject();
//						if (cellDetails.has("value")) {
//							value = cellDetails.get("value").getAsString();
//							if (value.equals("")) {
//								value = "0.0";
//							}
//
//						}
//
//					}
//
//				}
//				int indexOf = extractOperandsForInterReport.indexOf(operand);
//				extractOperandsForInterReport.set(indexOf, value);
//				allInterReportCellAndValue.put(value, operand);
//			}
//
//		}
//
//		
//		System.out.println(extractOperandsForInterReport.stream().collect(Collectors.joining()));
//		
//		return extractOperandsForInterReport.stream().collect(Collectors.joining());
//	}

	public ExtractInterReportValueFromreport extractInterReportValueFromreport(String parentFormula,
			JsonObject parentReportSubReportMap, JsonObject subReportUpdateParentReportSubReportMap,
			String parentName) {

		ExtractInterReportValueFromreport extractInterReportValueFromreport = new ExtractInterReportValueFromreport();

		FormulaUpdateDto formulaUpdateDto = new FormulaUpdateDto();

		boolean isSubReportSelected = false;
		Set<String> allInterParentReport = new HashSet<>();

//		ExtractInterReportValueFromreport();

		List<String> extractOperandsForInterReport = FormulaCellReferenceExtractor
				.extractOperandsForInterReport(parentFormula);
		Map<Integer, String> allInterReportCellAndValue = new HashMap<>();

		StringBuilder message = new StringBuilder();
		boolean isMessage = false;

		for (String operand : extractOperandsForInterReport) {
			boolean isApend = false;
			if (operand.contains("~")) {
				String value = "0.0";
				String[] extractTextInTildes = FormulaCellReferenceExtractor.extractTextInTildes(operand);
				String interParentReportName = extractTextInTildes[0];
				String interReportCell = extractTextInTildes[1];
				String[] extractTextInBrackets = extractTextInBrackets(interReportCell);
				String interSubReportName = extractTextInBrackets[0];
				interReportCell = extractTextInBrackets[1];

				if (interSubReportName == null || interSubReportName.isEmpty() || interSubReportName.equals("\"\"")) {

					if (interParentReportName.equals(parentName)) {
						List<SubReportData> findAllSubReportParentReportId = parentReportRepository
								.findByExcelFileName(interParentReportName)
								.map(parentReport -> subReportRepository.findAllByParentReportId(parentReport.getId()))
								.orElse(Collections.emptyList());
						if (!findAllSubReportParentReportId.isEmpty() && findAllSubReportParentReportId.size() > 0) {

							if (checkCompletionStatusOfSubReports(findAllSubReportParentReportId)) {
								isSubReportSelected = true;
								allInterParentReport.add(interParentReportName);

							}

						}

					} else {

						Boolean notInterReportFormulaCell = isNotInterReportFormulaCell(interParentReportName,
								interReportCell);

						if (notInterReportFormulaCell == null) {
							isMessage = true;
							if (!isApend) {
								message.append("Please update the following parent report(s): ")
										.append(interParentReportName);
								isApend = true;
							} else {
								message.append(", ").append(interParentReportName);
							}
						} else if (!notInterReportFormulaCell) {
							isMessage = true;
							if (!isApend) {
								message.append("No sub-reports found for parent: ").append(interParentReportName)
										.append(". Please add A sub report for it");
								isApend = true;
							} else {
								message.append(",Please add A sub report for ").append(interParentReportName);
							}
						} else {
							isSubReportSelected = true;
							allInterParentReport.add(interParentReportName);
						}

						if (isApend) {
							message.append(".");
						}

					}

				}

				if (interSubReportName != null && !interSubReportName.equals("\"\"")) {

					Optional<SubReportData> subReportdata = subReportRepository.findByReportName(interSubReportName);
					if (subReportdata.isPresent() && (subReportdata.get().getStatus().equals(Status.COMPLETED)
							|| subReportdata.get().getStatus().equals(Status.PARTIAL_COMPLETED))) {
						HashMap<String, Object> subReportObject = getSubReportObject(new Gson().toJsonTree(
								ConversionUtility.convertByteToObject(subReportdata.get().getRequestObject())));
						JsonObject cellDetalisData = (JsonObject) subReportObject.get(interReportCell);

						if (cellDetalisData != null && cellDetalisData.has("cellDetails")) {
							JsonObject cellDetails = cellDetalisData.get("cellDetails").getAsJsonObject();
							if (cellDetails.has("value")) {
								value = cellDetails.get("value").getAsString();
								if (value.equals("")) {
									value = "0.0";
								}
							}
						}
					}
				}
				int indexOf = extractOperandsForInterReport.indexOf(operand);
				extractOperandsForInterReport.set(indexOf, value);
				allInterReportCellAndValue.put(indexOf, operand);
			}
		}

		formulaUpdateDto.setIsInterReportErrorMessage(isMessage);
		if (isMessage) {
			formulaUpdateDto.setMessage(message.toString());

		}

		if (isSubReportSelected) {
			formulaUpdateDto.setAllInterParentReport(allInterParentReport);
			formulaUpdateDto.setIsSubReportSelected(isSubReportSelected);
		} else {
			formulaUpdateDto.setIsSubReportSelected(isSubReportSelected);
		}

		String modifiedFormula = extractOperandsForInterReport.stream().collect(Collectors.joining());
		AbstractMap.SimpleEntry<String, Map<Integer, String>> modifiyFormulaAndInterReportCellAndValue = new AbstractMap.SimpleEntry<>(
				modifiedFormula, allInterReportCellAndValue);
		extractInterReportValueFromreport
				.setModifiyFormulaAndInterReportCellAndValue(modifiyFormulaAndInterReportCellAndValue);

		extractInterReportValueFromreport.setFormulaUpdateDto(formulaUpdateDto);

		return extractInterReportValueFromreport;
	}

	private Boolean isNotInterReportFormulaCell(String interParentReportName, String sheetNameCellName) {
		Optional<ParentReportData> findByExcelFileName = parentReportRepository
				.findByExcelFileName(interParentReportName);

		List<SubReportData> findAllByParentReportId = subReportRepository
				.findAllByParentReportId(findByExcelFileName.get().getId());

		if (!findAllByParentReportId.isEmpty() && findAllByParentReportId.size() > 0) {

			return checkCompletionStatusOfSubReportsForInterReport(findAllByParentReportId);
		} else {
			return false;
		}
	}

	private Boolean checkCompletionStatusOfSubReportsForInterReport(
			List<SubReportData> findAllSubReportParentReportId) {
		boolean hasCompleted = false;

		for (SubReportData subReport : findAllSubReportParentReportId) {
			Status status = subReport.getStatus();
			if (status.equals(Status.PARTIAL_COMPLETED)) {
				return null;
			} else if (status.equals(Status.COMPLETED)) {
				hasCompleted = true;
			}
		}

		return hasCompleted ? Boolean.TRUE : Boolean.FALSE;
	}

	private boolean checkCompletionStatusOfSubReports(List<SubReportData> findAllSubReportParentReportId) {
		return findAllSubReportParentReportId.stream().map(SubReportData::getStatus)
				.anyMatch(status -> status.equals(Status.COMPLETED) || status.equals(Status.PARTIAL_COMPLETED));
	}

	public static String[] extractTextInBrackets(String input) {
		Pattern pattern = Pattern.compile("\\[(.*?)](.*?)$");
		Matcher matcher = pattern.matcher(input);

		String[] result = new String[2];

		if (matcher.find()) {
			result[0] = matcher.group(1).trim(); // Text inside square brackets
			if (result[0].isEmpty()) {
				result[0] = "\"\""; // Set empty string if no data inside brackets
			}
			result[1] = matcher.group(2).trim(); // Text outside square brackets
		} else {
			result[0] = "\"\""; // Set empty string for first part
			result[1] = input.trim(); // Set the entire input as the second part
		}

		return result;
	}

	@Override
	public Response<?> validateSubReportData(String subReportString, JsonElement parentResponseString,
			SubReportSaveRequestDto subReportSaveRequestDto, String parentName) {

		Optional<Configuration> continueDynamicRowColourFromDb = configurationRepository
				.findByKey("CONTINUE_ORANGE_COLOR");
		Optional<Configuration> BreakDynamicRowColourFromDb = configurationRepository.findByKey("BREAK_GRAY_COLOR");
		String continueDynamicRowColour = "#FFC000";
		String breakDynamicRowColour = "#C0C0C0";

		if (continueDynamicRowColourFromDb.isPresent()) {
			continueDynamicRowColour = continueDynamicRowColourFromDb.get().getValue();

		}
		if (BreakDynamicRowColourFromDb.isPresent()) {
			breakDynamicRowColour = BreakDynamicRowColourFromDb.get().getValue();

		}
		breakDynamicRowColour = ExcelConversion.getColorFamily(breakDynamicRowColour);
		continueDynamicRowColour = ExcelConversion.getColorFamily(continueDynamicRowColour);

		Map<String, FormulaUpdateDto> allUpadteFormula = new HashMap<>();
		Set<String> allSheetName = new HashSet<>();
		Set<String> parentNamesUsingInInterReportFormula = new HashSet<>();

		Response<?> parentObjectMapSheetNames = getParentObjectMap(parentResponseString);

		Response<?> formulaUpdateForSubReport = getFormulaUpdateForSubReport(subReportString,
				parentObjectMapSheetNames.getParentObjectMap(), subReportSaveRequestDto,
				parentObjectMapSheetNames.getParentSheetNames(), continueDynamicRowColour, breakDynamicRowColour,
				parentName);

		if (formulaUpdateForSubReport.getResponseCode() == 200) {
			UpdateFormulaSheetNameDto dto = (UpdateFormulaSheetNameDto) formulaUpdateForSubReport.getData();
			allSheetName = dto.getAllSheetName();
			allUpadteFormula = dto.getSubReportFormulaUpdate();

		} else {
			return formulaUpdateForSubReport;
		}
		try {

			int interReportUsingFormulaErrorCount = 0;

			Set<String> parentSheetNames = parentObjectMapSheetNames.getParentSheetNames();
			HashMap<String, Object> parentObjectMap = parentObjectMapSheetNames.getParentObjectMap();

			JsonElement element = JsonParser.parseString(subReportString);
			JsonArray sheetsArray = element.getAsJsonArray();

			long subSheetCount = 0;
			HashMap<String, Object> subObjectMap = new HashMap<>();
			for (JsonElement element1 : sheetsArray) {
				String sheetName = element1.getAsJsonObject().get("sheetName").getAsString();

				if (!parentSheetNames.contains(sheetName)) {
					return new Response<>(HttpStatus.BAD_REQUEST.value(),
							"Parent report is not match with the sub report", null, true);
				}
				subSheetCount++;

				JsonArray sheetData = element1.getAsJsonObject().getAsJsonArray("sheetData");
				for (JsonElement sheetDatum : sheetData) {
					JsonArray rowData = sheetDatum.getAsJsonObject().getAsJsonArray("rowData");
					for (JsonElement rowDatum : rowData) {
						JsonObject rowJsonData = rowDatum.getAsJsonObject();
						String cellName = rowJsonData.get("cellName").getAsString();
						JsonObject cellDetalis = rowJsonData.get("cellDetails").getAsJsonObject();

						FormulaUpdateDto formulaUpdateDto = allUpadteFormula.get(sheetName + "-" + cellName);
						if (formulaUpdateDto != null && formulaUpdateDto.getParentFormula() != null) {
							cellDetalis.addProperty("formula", formulaUpdateDto.getParentFormula());
						}
						String key = sheetName + "!" + cellName;
						subObjectMap.put(key, rowDatum);
					}
				}
			}

			String sheetDataString = new Gson().toJson(sheetsArray);

//			String jsonString = ConversionUtility.convertStringToJson(subReportStringif);
			Workbook workbook = ExcelFormulaEvaluator.excelGenerateObjectFromJSON(sheetDataString, false);

			if (subSheetCount != parentSheetNames.size()) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Parent report is not match with the sub report . Provide Valid SubReport", null, true);
			}

			List<ErrorResponseDto> errorMessageList = new ArrayList<>();
			Map<String, CheckBreakKeyStatus> checkBreakStatus = new ConcurrentHashMap<>();

			// Iterate sheetArray data
			for (JsonElement sheet : sheetsArray) {

				int countContinueForRow = 0;
				int countRowFromFormula = 0;
				int subReportCount = 0;
				Integer parentreportIndex = null;
				Boolean checkParentRport = true;
				String isBreakKey = "";
				Integer countContinueCell = 0;
//				boolean checkFormula = true;

				int continueRowBreakAt = 0;
				Set<String> allBreakKeys = new HashSet<>();

				int count = 0;
				Set<Integer> allContinueRowNumber = new TreeSet<>();

				// Check sheet element is not null
				if (sheet != null) {
					JsonObject sheetObject = sheet.getAsJsonObject();
					String sheetName = sheetObject.get("sheetName").getAsString();
					JsonArray rowsArray = sheetObject.getAsJsonArray("sheetData");

					// Iterate rowArray data
					for (JsonElement rowData : rowsArray) {

						subReportCount++;

						if (countContinueCell > 0) {
							checkParentRport = false;
						} else {
							checkParentRport = true;
						}

						// Check rowData is not null
						if (rowData != null) {
							JsonObject rowObject = rowData.getAsJsonObject();
							JsonArray cellArray = rowObject.getAsJsonArray("rowData");
							int countRowforFormula = 0;
							boolean isCountCheck = true;

							// Iterate cellArray data
							for (JsonElement cell : cellArray) {

								boolean isInterReportUsingInCell = false;

								Set<String> allParentNameUSingInInterReportInCell = new HashSet<>();

								Map<Integer, String> interReportCellAndValue = new TreeMap<>();
								JsonObject cellObject = cell.getAsJsonObject();
								JsonObject cellDetalis = cellObject.get("cellDetails").getAsJsonObject();

								Boolean isAppend = false;

								Long uniqueId = cellObject.get("uniqueId").getAsLong();
								String cellName = cellObject.get("cellName").getAsString();

								ErrorResponseDto errorResponseDto = new ErrorResponseDto();
								errorResponseDto.setCellName(cellName);
								errorResponseDto.setSheetName(sheetName);
								errorResponseDto.setUniqueId(uniqueId);
								String headerName = cellObject.get("headerName").getAsString();
								JsonObject parentObject = new JsonObject();

								StringBuilder message = new StringBuilder();
								int currentRowIndex = Integer.parseInt(cellName.replaceAll("\\D", ""));
								JsonObject cellDetailsObjects = cellObject != null
										? cellObject.getAsJsonObject("cellDetails")
										: null;

								String subCellName = cellName;

								headerName = (cellDetailsObjects != null && cellDetailsObjects.has("value")
										&& !cellDetailsObjects.get("value").isJsonNull())
												? cellDetailsObjects.get("value").getAsString()
												: headerName;

								// Update parent Call Bu dynamic
								String replaceCellNameForParent = "";

								String parentFormulaForSub = "";

								CheckBreakKeyStatus breakKeyStatus = checkBreakStatus
										.get(sheetName + "-" + cellName) != null
												? checkBreakStatus.get(sheetName + "-" + cellName)
												: new CheckBreakKeyStatus();

								String cellnameForParent = cellName;

//								New Parent call Logic
								if (checkParentRport) {

									if (countContinueForRow != 0) {
										int indexOfCell;
										if (countContinueForRow > 0) {
											indexOfCell = subReportCount - (countContinueForRow);
										} else {
											indexOfCell = subReportCount;
										}
										cellnameForParent = cellName.replaceAll("[^A-Za-z]", "");
										cellnameForParent += indexOfCell;
									}
									parentObject = (JsonObject) parentObjectMap.get(sheetName + cellnameForParent);

									JsonObject parentCellDetalis = parentObject != null
											&& parentObject.has("cellDetails")
													? parentObject.get("cellDetails").getAsJsonObject()
													: null;

//									cellName = cellnameForParent;

									if (parentCellDetalis != null) {
										if (parentCellDetalis.has("bgColor")
												&& !parentCellDetalis.get("bgColor").getAsString().equals("")
												&& ExcelConversion
														.getColorFamily(parentCellDetalis.get("bgColor").getAsString())
														.equals(continueDynamicRowColour)) {

//											isBreakKey = parentObject.has("isBreakKey")
//													? parentObject.get("isBreakKey").getAsString()
//													: "";

//											isBreakKey = "#C0C0C0";
											isBreakKey = breakDynamicRowColour;
											breakKeyStatus.setBreakKey(isBreakKey);
											breakKeyStatus.setColumnName(cellnameForParent);
											breakKeyStatus.setStartIngCell(cellnameForParent);
											breakKeyStatus.setStatus(false);
											countContinueCell++;
											parentreportIndex = Integer
													.parseInt(cellnameForParent.replaceAll("\\D", ""));
											allContinueRowNumber.add(parentreportIndex);

										}
									}

								} else {
									replaceCellNameForParent = cellName.replaceAll("[^A-Za-z]", "");
									replaceCellNameForParent = replaceCellNameForParent + parentreportIndex;
									parentObject = (JsonObject) parentObjectMap
											.get(sheetName + replaceCellNameForParent);

								} // new Parent Get Here

								if (parentreportIndex != null) {
									String parentCellName = cellName.replaceAll("[^A-Za-z]", "") + parentreportIndex;

									if (!cellName.equals(parentCellName) && !parentCellName.equals("")
											&& !checkParentRport) {
										if (breakKeyStatus.getBreakKey() == null
												&& breakKeyStatus.getColumnName() == null) {
											breakKeyStatus = checkBreakStatus
													.get(sheetName + "-" + parentCellName) != null
															? checkBreakStatus.get(sheetName + "-" + parentCellName)
															: new CheckBreakKeyStatus();
										}
									}
								}

								JsonObject parentCellDetails = null;
								boolean hasParentFormula = false;
								String parentFormula = null;
								boolean hasParentCellDetails = false;

								if (parentObject != null && parentObject.has("cellDetails")
										&& parentObject.get("cellDetails") != null
										&& parentObject.get("cellDetails").isJsonObject()) {
									hasParentCellDetails = true;
									parentCellDetails = parentObject.get("cellDetails").getAsJsonObject();
									if (parentCellDetails != null && parentCellDetails.has("hasFormula")) {
										hasParentFormula = parentCellDetails.has("hasFormula")
												? !"false".equalsIgnoreCase(
														parentCellDetails.get("hasFormula").getAsString())
												: false;
										if (hasParentFormula) {
											parentFormula = parentCellDetails.get("formula").getAsString();
										}
									}
								}

								// Break The Column By the Color Key
								if (countContinueCell != 0 && hasParentCellDetails && parentCellDetails.has("bgColor")
										&& !parentCellDetails.get("bgColor").getAsString().equals("")) {

									String subBreakColur = cellDetailsObjects != null
											&& cellDetailsObjects.has("bgColor")
													? cellDetailsObjects.get("bgColor").getAsString()
													: "";

									if ((allBreakKeys == null || allBreakKeys.size() == 0)
											|| !allBreakKeys.contains(cellnameForParent)) {

										if (breakKeyStatus.getColumnName() != null
												&& ExcelConversion.getColorFamily(subBreakColur)
														.equals(breakDynamicRowColour)
												&& breakKeyStatus.getColumnName().replaceAll("[^A-Za-z]", "")
														.equals(subCellName.replaceAll("[^A-Za-z]", ""))) {

											//
											allBreakKeys.add(cellName);
											breakKeyStatus.setStatus(true);
											countContinueCell = 0;

											int indexNumber = Integer.parseInt(cellName.replaceAll("\\D", ""));
											if (countContinueCell == 0) {
												continueRowBreakAt = indexNumber;
											}
											if (indexNumber != 1) {
												indexNumber -= 1;
											}

											breakKeyStatus.setEndingIngCell(
													cellName.replaceAll("[^A-Za-z]", "") + indexNumber);

											countContinueForRow--;
											checkBreakStatus.put(sheetName + "-" + breakKeyStatus.getColumnName(),
													breakKeyStatus);

											for (String breakStatusMapKey : checkBreakStatus.keySet()) {

												if (breakStatusMapKey.contains(sheetName)) {
													CheckBreakKeyStatus checkBreakKeyStatus = checkBreakStatus
															.get(breakStatusMapKey);

													if (checkBreakKeyStatus.getColumnName() != null
															&& checkBreakKeyStatus.getStartIngCell() != null
															&& checkBreakKeyStatus.getEndingIngCell() == null) {
														checkBreakKeyStatus.setEndingIngCell(
																cellName.replaceAll("[^A-Za-z]", "") + indexNumber);
														checkBreakStatus.put(breakStatusMapKey, checkBreakKeyStatus);
													}

												}

											}

											countRowFromFormula = 0;
											break;
										}

									}
									checkBreakStatus.put(sheetName + "-" + breakKeyStatus.getColumnName(),
											breakKeyStatus);
								}

								FormulaUpdateDto formulaUpdateDto = new FormulaUpdateDto();

								if (hasParentFormula && parentFormula != null) {

									formulaUpdateDto = allUpadteFormula.get(sheetName + "-" + cellName);

									if (formulaUpdateDto != null) {

//										if (formulaUpdateDto.getIsSubReportSelected() == null
//												|| !formulaUpdateDto.getIsSubReportSelected()) {

										parentFormula = formulaUpdateDto.getParentFormula();
										parentFormulaForSub = parentFormula;
										interReportCellAndValue = formulaUpdateDto.getInterReportCellAndValue();
										if (!interReportCellAndValue.isEmpty()) {

											parentFormulaForSub = replaceInterReportValuesInFormula(parentFormula,
													interReportCellAndValue);

										}

//										} else {
////											parentFormulaForSub = parentFormula;
//										}
									}

								}

								JsonObject cellDetailsObject = null;

								if (cellObject != null && cellObject.get("cellDetails") != null
										&& (cellObject.get("cellDetails").isJsonObject())) {

									cellDetailsObject = cellObject.get("cellDetails").getAsJsonObject();

									if (cellDetailsObject != null) {

										if (cellDetailsObject.has("isError")) {
											cellDetailsObject.remove("isError");
										}
										if (cellDetailsObject.has("error")) {
											cellDetailsObject.remove("error");
										}

										// Set Header Name Inside value
										if (breakKeyStatus != null && breakKeyStatus.isStatus()) {
											if (parentCellDetails != null
													&& parentObject.get("headerName").getAsString().equals("")) {
												cellObject.addProperty("headerName", "");
												cellDetailsObject.addProperty("value", headerName);
												JsonObject extraFormulaObject = new JsonObject();
												cellDetailsObject.add("extraFormula", extraFormulaObject);
												cellDetailsObject.addProperty("hasExtraFormula", false);
											}
											continue;
										}

										if (cellDetailsObject.has("value")
												&& !cellDetailsObject.get("value").getAsString().equals("")) {
											String value = cellDetailsObject.get("value").getAsString();
											headerName = value;
										}

										if (hasParentCellDetails && parentCellDetails.has("hasExtraFormula")
												&& parentCellDetails.get("hasExtraFormula").getAsBoolean()
												&& parentCellDetails.has("extraFormula")) {
											JsonObject extraFormulaObject = parentCellDetails.get("extraFormula")
													.getAsJsonObject();

											if (cellDetailsObject.has("extraFormula")
													&& (cellDetailsObject.get("extraFormula").getAsJsonObject()
															.has("isInterSubreportNotSelectedInValidCondition")
															|| cellDetailsObject.get("extraFormula").getAsJsonObject()
																	.has("isInterSubreportNotSelectedInIfCondition"))) {

											} else {
												cellDetailsObject.addProperty("hasExtraFormula", true);
												cellDetailsObject.add("extraFormula", extraFormulaObject);

											}

										}

										if (cellDetailsObject.has("formulaIsNotInChild")
												&& cellDetailsObject.get("formulaIsNotInChild").getAsBoolean()) {
											cellDetailsObject.remove("hasFormula");
											cellDetailsObject.remove("formula");
										}

										String formula = cellDetailsObject.has("formula")
												&& !cellDetailsObject.get("formula").getAsString().equals("")
														? cellDetailsObject.get("formula").getAsString()
														: null;

										if (formula != null && !formula.isEmpty() && formula.startsWith("=")) {
											formula = formula.substring(1);
										}
										boolean hasFormula = cellDetailsObject.has("hasFormula")
												? !"false".equalsIgnoreCase(
														cellDetailsObject.get("hasFormula").getAsString())
												: false;

										boolean isIgnoreDataSheetFormula = hasParentFormula
												&& parentCellDetails.has("isIgnoreDataSheetFormula")
														? parentCellDetails.get("isIgnoreDataSheetFormula")
																.getAsBoolean()
														: false;
										boolean isInterReportFormula = hasParentFormula
												&& parentCellDetails.has("isInterReportFormula")
														? parentCellDetails.get("isInterReportFormula").getAsBoolean()
														: false;

										if (subReportSaveRequestDto.getIsIgnoreAllDataSheetFormula() != null
												&& subReportSaveRequestDto.getIsIgnoreAllDataSheetFormula()) {
											isIgnoreDataSheetFormula = subReportSaveRequestDto
													.getIsIgnoreAllDataSheetFormula();
										}

										// Formula Conflict between parent and sub report
										if (hasParentFormula && !hasFormula) {

											if (!isIgnoreDataSheetFormula && !isInterReportFormula) {
												cellDetailsObject.addProperty("formula", parentFormulaForSub);
												headerName = "";
												if (isAppend) {

													message.append(
															", formula is present in parent report but not found in sub report!!");
													errorResponseDto.setErrorMessage(message.toString());
												} else {
													message.append("Error in cell " + cellName + " in sheet "
															+ sheetName
															+ ", formula is present in parent report but not found in sub report!!");
													errorResponseDto.setErrorMessage(message.toString());
													isAppend = true;
												}

											} else {
												headerName = "";
												formula = parentFormulaForSub;
												cellDetailsObject.addProperty("hasFormula", true);
												cellDetailsObject.addProperty("formula", parentFormulaForSub);
												cellDetailsObject.addProperty("formulaIsNotInChild", true);
												hasFormula = true;
											}

										}

										if (hasParentFormula && hasFormula && !isInterReportFormula
												&& !parentFormula.equals(formula) && !isIgnoreDataSheetFormula) {
											if (isAppend) {
												message.append(". And, Error in cell " + cellName + " in sheet "
														+ sheetName + ". As formula is not matched!! ");
												errorResponseDto.setErrorMessage(message.toString());
											} else {
												message.append("Error in cell " + cellName + " in sheet " + sheetName
														+ ". As formula is not matched!! ");
												errorResponseDto.setErrorMessage(message.toString());
												isAppend = true;
											}

										}

										boolean InterReportFormulaAndSubReportRequired = true;

										if (hasParentFormula && formulaUpdateDto != null
												&& formulaUpdateDto.getIsInterReportErrorMessage() != null
												&& formulaUpdateDto.getIsInterReportErrorMessage()) {

											isInterReportUsingInCell = true;

											if (isAppend) {
												message.append(formulaUpdateDto.getMessage());
												errorResponseDto.setErrorMessage(message.toString());
											} else {
												message.append(formulaUpdateDto.getMessage());
												errorResponseDto.setErrorMessage(message.toString());
												isAppend = true;
											}

										}

										if (hasParentFormula && formulaUpdateDto != null
												&& formulaUpdateDto.getIsSubReportSelected() != null
												&& formulaUpdateDto.getIsSubReportSelected()) {

											InterReportFormulaAndSubReportRequired = false;
											isInterReportUsingInCell = true;

											if (isAppend) {
												message.append(". And, Error in cell " + cellName + " in sheet "
														+ sheetName
														+ ". As formula is InterReportFormula Please Select A Valid Sub Report !! "
														+ parentFormulaForSub);
												errorResponseDto.setErrorMessage(message.toString());
											} else {
												message.append("Error in cell " + cellName + " in sheet " + sheetName
														+ ". As formula is InterReportFormula Please Select A Valid Sub Report !! "
														+ parentFormulaForSub);
												errorResponseDto.setErrorMessage(message.toString());
												isAppend = true;
											}

										}

										if ((!hasParentCellDetails || !hasParentFormula) && hasFormula
												&& !isIgnoreDataSheetFormula) {
											if (isAppend) {
												message.append(", this formula is not Present In Parent Report!! ");
												errorResponseDto.setErrorMessage(message.toString());
											} else {
												message.append("Error in cell " + cellName + " in sheet " + sheetName
														+ ". As this formula is not Present In Parent Report!! ");
												errorResponseDto.setErrorMessage(message.toString());
												isAppend = true;
											}
										}

										int cellIndex = Integer.parseInt(cellDetailsObject.get("index").getAsString());
										int rowIndex = Integer
												.parseInt(cellDetailsObject.get("rowIndex").getAsString());
										errorResponseDto.setRowNumber(rowIndex + 1);

										if (hasParentFormula && parentFormula != null && hasFormula
												&& InterReportFormulaAndSubReportRequired) {

											if (cellDetailsObject.has("index") && cellDetailsObject.has("rowIndex")) {

												Object value = ExcelFormulaEvaluator.evaluateFormula(workbook,
														sheetName, rowIndex, cellIndex, parentFormula);

												if (value == null
														|| (value != null && value.toString().equals("#ERROR"))) {
													if (isAppend) {
														message.append(
																", Please recheck the values  of the respective cells these are use in : "
																		+ formula + ".");
														errorResponseDto.setErrorMessage(message.toString());
													} else {
														message.append("Error in cell " + cellName + " in sheet "
																+ sheetName
																+ " Please recheck the values  of the respective cells these are use in : "
																+ formula + ".");
														errorResponseDto.setErrorMessage(message.toString());
														isAppend = true;
													}
												} else {
//													if (headerName.equals("")) {
//														cellObject.addProperty("headerName", value.toString());
//														if (cellDetailsObject.has("value") && cellDetailsObject
//																.get("value").getAsString().equals("")) {
//															cellDetailsObject.addProperty("value", value.toString());
//
//														}
//														headerName = value.toString();
//
//													}
													String checkValue = ConversionUtility.fixDecimal(value.toString());
													cellObject.addProperty("headerName", checkValue);
													headerName = value.toString();
													cellDetailsObject.addProperty("value", checkValue);
												}

//									
											}
										}

										// Checking for mandatory cell
										if (hasParentCellDetails && parentCellDetails.has("hasExtraFormula")
												&& parentCellDetails.get("hasExtraFormula").getAsBoolean()
												&& parentCellDetails.has("extraFormula")) {
											boolean checkIfconduction = false;
											Boolean isSubReportSelectInIfCondition = false;
											JsonObject subReportExtraFormula = null;
											JsonArray ifConditionArray = null;
											JsonObject extraFormulaObject = parentCellDetails.get("extraFormula")
													.getAsJsonObject();
											if (extraFormulaObject.has("isMandatory")
													&& extraFormulaObject.get("isMandatory").getAsBoolean()) {
												if (extraFormulaObject.has("hasIf")
														&& extraFormulaObject.get("hasIf").getAsBoolean()
														&& extraFormulaObject.has("ifcondition")) {

													if (extraFormulaObject.get("ifcondition").isJsonNull()
															|| extraFormulaObject.get("ifcondition").getAsJsonArray()
																	.isEmpty()
															|| extraFormulaObject.get("ifcondition").getAsJsonArray()
																	.size() == 0) {
														checkIfconduction = true;
													} else {

														ifConditionArray = extraFormulaObject.get("ifcondition")
																.getAsJsonArray();

														if (cellDetailsObject.has("hasExtraFormula")
																&& cellDetailsObject.get("hasExtraFormula")
																		.getAsBoolean()
																&& cellDetailsObject.has("extraFormula")) {

															subReportExtraFormula = cellDetailsObject
																	.get("extraFormula").getAsJsonObject();

															if (subReportExtraFormula
																	.has("isInterSubreportNotSelectedInIfCondition")) {

																ifConditionArray = subReportExtraFormula
																		.get("ifcondition").getAsJsonArray();
															}

														}

														if (extraFormulaObject.has("isInterReportInIfCondition")
																&& extraFormulaObject.get("isInterReportInIfCondition")
																		.getAsBoolean()) {

															ExtractInterReportValueFromreport extractInterReportValueForIfConduction = extractInterReportValueForIfConduction(
																	ifConditionArray);

															isSubReportSelectInIfCondition = extractInterReportValueForIfConduction
																	.getIsSubReportSelected();
															if (!isSubReportSelectInIfCondition) {
																ifConditionArray = extractInterReportValueForIfConduction
																		.getModifiedIfConditionArray();
															} else {

																allParentNameUSingInInterReportInCell
																		.addAll(extractInterReportValueForIfConduction
																				.getAllInterParentReport());

																Gson gson = new Gson();
																JsonElement allPArentReportNAmesUsingForIfCondition = gson
																		.toJsonTree(
																				extractInterReportValueForIfConduction
																						.getAllInterParentReport());
																subReportExtraFormula.add(
																		"allInterParentReportInIfCondition",
																		allPArentReportNAmesUsingForIfCondition);

															}
															subReportExtraFormula.addProperty(
																	"isInterSubreportNotSelectedInIfCondition",
																	isSubReportSelectInIfCondition.toString());

														}

														if (!isSubReportSelectInIfCondition) {
															checkIfconduction = IfConditionChecking.checkIfcondition(
																	ifConditionArray, subObjectMap, sheetName,
																	parentObjectMap);
														}

													}

												} else if (headerName.equals("") || headerName.isEmpty()) {
													if (isAppend) {
														message.append(". And, Error in cell " + cellName + " in sheet "
																+ sheetName + ". Value cannnot be empty in cell ");
														errorResponseDto.setErrorMessage(message.toString());
													} else {
														message.append("Error in cell " + cellName + " in sheet "
																+ sheetName + ". Value cannnot be empty in cell ");
														errorResponseDto.setErrorMessage(message.toString());
														isAppend = true;
													}

												}

												if (isSubReportSelectInIfCondition) {
													isInterReportUsingInCell = true;
													if (headerName.equals("") || headerName.isEmpty()) {
														if (isAppend) {
															message.append(
																	". Additionally, there's an error in cell. The if Condition uses an Inter Report. Please provide a valid Sub report for Cell:  "
																			+ cellName + " in the sheet " + sheetName
																			+ "Condition " + ifConditionArray.toString()
																					.replaceAll("\"\"", "\"empty\"")
//																			.replaceAll("[\\[\\]\"]", "")
																					.replaceAll(",", " "));
															errorResponseDto.setErrorMessage(message.toString());
														} else {
															message.append(
																	"There's an error in cell. The if Condition uses an Inter Report. Please provide a valid Sub report for  Cell :"
																			+ cellName + " in the sheet " + sheetName
																			+ "Condition " + ifConditionArray.toString()
																					.replaceAll("\"\"", "\"empty\"")
//																			.replaceAll("[\\[\\]\"]", "")
																					.replaceAll(",", " "));
															errorResponseDto.setErrorMessage(message.toString());
															isAppend = true;
														}

													}
												}

												if (checkIfconduction) {
													if (headerName.equals("") || headerName.isEmpty()) {
														if (isAppend) {
															message.append(". And, Error in cell " + cellName
																	+ " in sheet " + sheetName
																	+ ". Value cannnot be empty in cell ");
															errorResponseDto.setErrorMessage(message.toString());
														} else {
															message.append("Error in cell " + cellName + " in sheet "
																	+ sheetName + ". Value cannnot be empty in cell ");
															errorResponseDto.setErrorMessage(message.toString());
															isAppend = true;
														}

													}
												}
											}
											if (headerName != null && !headerName.equals("") && !headerName.isEmpty()) {
												if (extraFormulaObject.has("type")) {

													if (extraFormulaObject.get("type").getAsString()
															.equals(CustomCellType.EMAIL.name())) {
														boolean isEmail = TypeValidation.isValidEmail(headerName);
														if (!isEmail) {
															if (isAppend) {
																message.append(". And, Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". Value is must an Email ");
																errorResponseDto.setErrorMessage(message.toString());
															} else {
																message.append("Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". Value must be an Email ");
																errorResponseDto.setErrorMessage(message.toString());
																isAppend = true;
															}
														}
													} else if (extraFormulaObject.get("type").getAsString()
															.equals(CustomCellType.DATE.name())) {
														if (extraFormulaObject.has("dateFormat")) {

															String dateFormater = extraFormulaObject.get("dateFormat")
																	.getAsString();

															DateFormat dateFormatEnum = Arrays
																	.stream(DateFormat.values())
																	.filter(format -> format.getKey()
																			.equals(dateFormater))
																	.findFirst().orElse(null);

															String convertDateAsParentPattern = dateFormatEnum != null
																	? ConversionUtility.convertDateFormatAsPattern(
																			headerName,
																			DateFormat.DD_MM_YYYY.getFormat(),
																			dateFormatEnum.getFormat())
																	: null;

															if (convertDateAsParentPattern != null) {
																headerName = convertDateAsParentPattern;
															}

															boolean validDateFormat = false;

															if (dateFormatEnum != null) {
																validDateFormat = TypeValidation.isValidDateFormat(
																		headerName, dateFormatEnum.getFormat());
															}

//															for (DateFormat dateFormat : DateFormat.values()) {
//																if (dateFormat.getKey()
//																		.equalsIgnoreCase(dateFormater)) {
//																	validDateFormat = TypeValidation.isValidDateFormat(
//																			headerName, dateFormat.getFormat());
//																	if (validDateFormat) {
//																		break;
//																	}
//																}
//															}
															if (!validDateFormat) {
																if (isAppend) {
																	message.append(". And, Error in cell " + cellName
																			+ " in sheet " + sheetName
																			+ ". Date is must be " + dateFormater);
																	errorResponseDto
																			.setErrorMessage(message.toString());
																} else {
																	message.append("Error in cell " + cellName
																			+ " in sheet " + sheetName
																			+ ". Date must be " + dateFormater);
																	errorResponseDto
																			.setErrorMessage(message.toString());
																	isAppend = true;
																}
															}

														}

													} else if (extraFormulaObject.get("type").getAsString()
															.equals(CustomCellType.NUMBER.name())) {

														boolean isNumber = TypeValidation.isNumber(headerName);
														if (!isNumber) {
															if (isAppend) {
																message.append(". And, Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". Value is must an Number ");
																errorResponseDto.setErrorMessage(message.toString());
															} else {
																message.append("Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". Value must be an Number ");
																errorResponseDto.setErrorMessage(message.toString());
																isAppend = true;
															}
														}

													} else if (extraFormulaObject.get("type").getAsString()
															.equals(CustomCellType.PHONE.name())) {
														boolean isPhone = typeValidation.isValidPhone(headerName);
														if (!isPhone) {
															if (isAppend) {
																message.append(". And, Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". invalid Phone Number ");
																errorResponseDto.setErrorMessage(message.toString());
															} else {
																message.append("Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". invalid Phone Number ");
																errorResponseDto.setErrorMessage(message.toString());
																isAppend = true;
															}
														}
													} else if (extraFormulaObject.get("type").getAsString()
															.equals(CustomCellType.STD.name())) {
														boolean isStd = typeValidation.isValidStd(headerName);
														if (!isStd) {
															if (isAppend) {
																message.append(". And, Error in cell " + cellName
																		+ " in sheet " + sheetName
																		+ ". invalid STD Number ");
																errorResponseDto.setErrorMessage(message.toString());
															} else {
																message.append(
																		"Error in cell " + cellName + " in sheet "
																				+ sheetName + ". invalid STD Number ");
																errorResponseDto.setErrorMessage(message.toString());
																isAppend = true;
															}
														}
													}

												}
											}

											if (extraFormulaObject.has("hasValidCondition")
													&& extraFormulaObject.get("hasValidCondition").getAsBoolean()
													&& extraFormulaObject.has("validCondition")) {
												Boolean isSubReportSelectInValidCondition = false;
												JsonArray validConditionsArray = extraFormulaObject
														.get("validCondition").getAsJsonArray();

												if (cellDetailsObject.has("hasExtraFormula")
														&& cellDetailsObject.get("hasExtraFormula").getAsBoolean()
														&& cellDetailsObject.has("extraFormula")) {

													subReportExtraFormula = cellDetailsObject.get("extraFormula")
															.getAsJsonObject();

													if (subReportExtraFormula
															.has("isInterSubreportNotSelectedInValidCondition")) {

														validConditionsArray = subReportExtraFormula
																.get("validCondition").getAsJsonArray();
													}

												}

												if (extraFormulaObject.has("isInterReportInValidCondition")
														&& extraFormulaObject.get("isInterReportInValidCondition")
																.getAsBoolean()) {

													ExtractInterReportValueFromreport extractInterReportValueForValidConduction = extractInterReportValueForValidConduction(
															validConditionsArray);

													isSubReportSelectInValidCondition = extractInterReportValueForValidConduction
															.getIsSubReportSelected();
													if (!isSubReportSelectInValidCondition) {
														validConditionsArray = extractInterReportValueForValidConduction
																.getModifiedIfConditionArray();
													} else {

														Gson gson = new Gson();
														JsonElement allPArentReportNAmesUsingForValidCondition = gson
																.toJsonTree(extractInterReportValueForValidConduction
																		.getAllInterParentReport());
														subReportExtraFormula.add(
																"allInterParentReportInValidCondition",
																allPArentReportNAmesUsingForValidCondition);

														allParentNameUSingInInterReportInCell
																.addAll(extractInterReportValueForValidConduction
																		.getAllInterParentReport());
													}

													subReportExtraFormula.addProperty(
															"isInterSubreportNotSelectedInValidCondition",
															isSubReportSelectInValidCondition.toString());

												}

												Boolean checkCondition = true;

												if (!isSubReportSelectInValidCondition) {
													checkCondition = ValidConditionCheck.checkValidCondition(
															validConditionsArray, subObjectMap, sheetName, cellName,
															headerName, parentObjectMap);
												}
												String condition = validConditionsArray.toString()
														.replaceAll("[\\[\\]\"]", "").replaceAll(",", " ");

												if (!checkCondition && !isSubReportSelectInValidCondition) {

													if (isAppend) {
														message.append(". And, Error in cell " + cellName + " in sheet "
																+ sheetName + " Condition " + cellName + " "
																+ validConditionsArray.toString()
																		.replaceAll("\"\"", "\"empty\"")
																		.replaceAll("[\\[\\]\"]", "")
																		.replaceAll(",", " ")
																+ " Is Not Satisfied ");
														errorResponseDto.setErrorMessage(message.toString());
													} else {
														message.append("Error in cell " + cellName + " in sheet "
																+ sheetName + ". Condition " + cellName + " "
																+ validConditionsArray.toString()
																		.replaceAll("\"\"", "\"empty\"")
																		.replaceAll("[\\[\\]\"]", "")
																		.replaceAll(",", " ")
																+ " Is Not Satisfied");
														errorResponseDto.setErrorMessage(message.toString());
														isAppend = true;
													}

												}
												if (isSubReportSelectInValidCondition) {

													isInterReportUsingInCell = true;
													if (isAppend) {
														message.append(". Additionally, there's an error in cell "
																+ cellName + " in sheet " + sheetName + ". Condition : "
																+ cellName + " "
																+ validConditionsArray.toString()
																		.replaceAll("\"\"", "\"empty\"")
																		.replaceAll("[\\[\\]\"]", "")
																		.replaceAll(",", " ")
																+ ", uses an interreport cell. Please choose a sub report.");
														errorResponseDto.setErrorMessage(message.toString());
													} else {
														message.append("Error in cell " + cellName + " in sheet "
																+ sheetName + ". Condition : " + cellName + " "
																+ validConditionsArray.toString()
																		.replaceAll("\"\"", "\"empty\"")
																		.replaceAll("[\\[\\]\"]", "")
																		.replaceAll(",", " ")
																+ " , uses an interreport cell. Please choose a sub report.");
														errorResponseDto.setErrorMessage(message.toString());
														isAppend = true;
													}
												}

											}

										}

										// Bishwajit Code
										String validation = parentCellDetails != null
												&& parentCellDetails.has("validation")
												&& !parentCellDetails.get("validation").getAsString().equals("")
														? parentCellDetails.get("validation").getAsString()
														: null;
										boolean hasValidation = parentCellDetails != null
												&& parentCellDetails.has("hasValidation")
														? !"false".equalsIgnoreCase(
																parentCellDetails.get("hasValidation").getAsString())
														: false;

										if (hasValidation && validation != null) {
											JSONObject cellValidationObject = new JSONObject(
													parentCellDetails.toString());

											String validationBase64 = cellValidationObject.getString("validation")
													.toString();

											if (hasValidation && !validation.equals("")) {

												String base64ToJsonValidation = ExcelConversion
														.Base64ToJson(validationBase64);

												JsonObject jsonObject = (JsonObject) JsonParser
														.parseString(base64ToJsonValidation);

												if (jsonObject.has("formula1") && jsonObject.has("ValidationType")) {

													int validationType = jsonObject.get("ValidationType") != null
															? jsonObject.get("ValidationType").getAsInt()
															: 0;

													if (validationType == DataValidationConstraint.ValidationType.FORMULA) {

														String formula1 = jsonObject.has("formula1")
																? jsonObject.get("formula1").getAsString()
																: "";

														if (headerName.equals("") || headerName.isEmpty()) {
//															message.append("Value cannnot be empty in cell "
//																	+ cellObject.get("cellName").getAsString()
//																	+ " in sheet " + sheetName);
//															errorResponseDto.setErrorMessage(message.toString());
//															isAppend = true;
														} else {

															Object value = ExcelFormulaEvaluator.evaluateFormula(
																	workbook, sheetName, rowIndex, cellIndex, formula1);

															if (value == null || (value != null
																	&& value.toString().equals("false"))) {
																if (isAppend) {
																	message.append(
																			". And, please enter valid data to custom Validation formula");
																	errorResponseDto
																			.setErrorMessage(message.toString());
//																isAppend = true;
																} else {
																	message.append("Error in cell " + cellName
																			+ " in sheet " + sheetName
																			+ "Please enter valid data to custom Validation formula");
																	errorResponseDto
																			.setErrorMessage(message.toString());
																	isAppend = true;
																}
															}
														}

													}

													else {

														// Manajit Code

														Response<?> validateCellValidation = ExcelValidation
																.validateCellValidation(jsonObject, headerName);

														if (validateCellValidation.getResponseCode() != HttpStatus.OK
																.value()) {
															if (isAppend) {
																message.append(". And, " + validateCellValidation
																		.getMessage().toString());
																errorResponseDto.setErrorMessage(message.toString());
															} else {
																message.append("Error in cell " + cellName
																		+ " in sheet " + sheetName + ". And, "
																		+ validateCellValidation.getMessage()
																				.toString());
																errorResponseDto.setErrorMessage(message.toString());
																isAppend = true;
															}
														}

													}

												}
											}
										}
										if (hasParentCellDetails) {
											Response<?> validateCellResponse = ExcelValidation
													.validateCell(parentObject, headerName);
											if (validateCellResponse.getResponseCode() != HttpStatus.OK.value()) {
												if (isAppend) {

													message.append(". And, " + validateCellResponse.getMessage());
													errorResponseDto.setErrorMessage(message.toString());
//												isAppend = true;
												} else {
													message.append(
															" Error in cell " + cellName + " in sheet " + sheetName
																	+ ". And, " + validateCellResponse.getMessage());
													errorResponseDto.setErrorMessage(message.toString());
													isAppend = true;
												}
											}
										}

									}

								} else {

									if (hasParentCellDetails) {

										JsonObject cellDetails = parentObject;

										if (hasParentFormula) {
											cellDetails.addProperty("hasFormula", false);
											cellDetails.remove("formula");
											if (isAppend) {
												message.append(", As formula is not found!! ");
												errorResponseDto.setErrorMessage(message.toString());
											} else {
												message.append("Error in cell " + cellName + " in sheet " + sheetName
														+ ". As formula is not found!! ");
												errorResponseDto.setErrorMessage(message.toString());
												isAppend = true;
											}
										}

										cellObject.add("cellDetails", cellDetails);

										if (parentObject.has("extraFormula")) {
											JsonObject extraFormulaObject = parentObject.get("extraFormula")
													.getAsJsonObject();
											boolean checkIfconduction = false;
											if (extraFormulaObject.has("isMandatory")
													&& extraFormulaObject.get("isMandatory").getAsBoolean()) {
												if (extraFormulaObject.has("hasIf")
														&& extraFormulaObject.get("hasIf").getAsBoolean()
														&& extraFormulaObject.has("ifcondition")) {

													if (extraFormulaObject.get("ifcondition").isJsonNull()
															|| extraFormulaObject.get("ifcondition").getAsJsonArray()
																	.isEmpty()
															|| extraFormulaObject.get("ifcondition").getAsJsonArray()
																	.size() == 0) {
														checkIfconduction = true;
													} else {

														checkIfconduction = IfConditionChecking.checkIfcondition(
																extraFormulaObject.get("ifcondition").getAsJsonArray(),
																subObjectMap, sheetName);

													}

												} else if (headerName.equals("") || headerName.isEmpty()) {
													if (isAppend) {
														message.append(". And, Error in cell " + cellName + " in sheet "
																+ sheetName + ". Value cannnot be empty in cell ");
														errorResponseDto.setErrorMessage(message.toString());
													} else {
														message.append("Error in cell " + cellName + " in sheet "
																+ sheetName + ". Value cannnot be empty in cell ");
														errorResponseDto.setErrorMessage(message.toString());
														isAppend = true;
													}

												}
												if (checkIfconduction) {
													if (headerName.equals("") || headerName.isEmpty()) {
														if (isAppend) {
															message.append(". And, Error in cell " + cellName
																	+ " in sheet " + sheetName
																	+ ". Value cannnot be empty in cell ");
															errorResponseDto.setErrorMessage(message.toString());
														} else {
															message.append("Error in cell " + cellName + " in sheet "
																	+ sheetName + ". Value cannnot be empty in cell ");
															errorResponseDto.setErrorMessage(message.toString());
															isAppend = true;
														}

													}
												}
											}
										}

//										Response<?> validateCellResponse = ExcelValidation.validateCell(parentObject,
//												headerName);
//										if (validateCellResponse.getResponseCode() != HttpStatus.OK.value()) {
//											
//											if (isAppend) {
//												message.append(". And, " + validateCellResponse.getMessage());
//												errorResponseDto.setErrorMessage(message.toString());
////												isAppend = true;
//											} else {
//												message.append(" Error in cell " + cellName + " in sheet " + sheetName
//														+ ". And, " + validateCellResponse.getMessage());
//												errorResponseDto.setErrorMessage(message.toString());
//												isAppend = true;
//											}
//										}

									}
								}

								if (!message.toString().isEmpty()) {
									cellDetailsObject.addProperty("isError", "true");
									cellDetailsObject.addProperty("error", message.toString());
									errorMessageList.add(errorResponseDto);
								} else {
									if (cellDetailsObject.has("isError")) {
										cellDetailsObject.remove("isError");
									}
									if (cellDetailsObject.has("error")) {
										cellDetailsObject.remove("error");
									}
								}

								String parentHeaderName = parentCellDetails != null && parentObject.has("headerName")
										? parentObject.get("headerName").getAsString()
										: "";
								if (parentCellDetails != null && parentHeaderName.equals("")) {
									cellObject.addProperty("headerName", "");
									cellDetailsObject.addProperty("value", ConversionUtility.fixDecimal(headerName));
								}
								if (cellDetailsObject.has("isCellValueChanged")) {
									cellDetailsObject.remove("isCellValueChanged");

								}

								if (hasParentFormula && formulaUpdateDto != null
										&& formulaUpdateDto.getIsSubReportSelected() != null) {

									cellDetailsObject.addProperty("formula", parentFormulaForSub);

									cellDetailsObject.addProperty("isInterSubreportNotSelected",
											formulaUpdateDto.getIsSubReportSelected().toString());

									if (formulaUpdateDto.getAllInterParentReport() != null
											&& formulaUpdateDto.getAllInterParentReport().size() > 0) {

										allParentNameUSingInInterReportInCell
												.addAll(formulaUpdateDto.getAllInterParentReport());

										Gson gson = new Gson();
										JsonElement allPArentReportNAmesUsingInFormula = gson
												.toJsonTree(formulaUpdateDto.getAllInterParentReport());
										cellDetailsObject.add("allInterParentReportInFormula",
												allPArentReportNAmesUsingInFormula);

									}

								}

								if (allParentNameUSingInInterReportInCell.size() > 0) {

									parentNamesUsingInInterReportFormula.addAll(allParentNameUSingInInterReportInCell);

//									Gson gson = new Gson();
//									JsonElement allPArentReportNAmesUsingInFormula = gson
//											.toJsonTree(allParentNameUSingInInterReportInCell);
//									cellDetailsObject.add("allInterParentReport", allPArentReportNAmesUsingInFormula);

								}
//
//								if (hasParentFormula && parentFormula != null && parentCellDetails != null
//										&& hasParentCellDetails) {
//
//									cellDetailsObject.addProperty("formulaPresentInParentCellName",
//											parentCellDetails.get("cellName").getAsString());
//
//								}

								if (countContinueCell == 0) {
//									countRowFromFormula = 0;
									checkParentRport = true;
								}
								if (isInterReportUsingInCell) {
									interReportUsingFormulaErrorCount++;
								}

							} // cellArray iteration end

							if (!isCountCheck) {
								countRowFromFormula++;
							}

						} // rowData not null check end
						if (countContinueCell != 0) {
							countContinueForRow++;
						}

					} // rowArray iteration end

					if (countContinueForRow > 0) {
						countRowFromFormula = countContinueForRow;
					}

				} // sheet object not null check end
				allBreakKeys.clear();
			} // Sheet array iteration end

			if (errorMessageList.size() > 0) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), "Error", sheetsArray.toString(), errorMessageList,
						errorMessageList.size(), parentNamesUsingInInterReportFormula,
						interReportUsingFormulaErrorCount);
			} else {
				return new Response<>(HttpStatus.OK.value(), "success", sheetsArray.toString(), errorMessageList,
						errorMessageList.size(), parentNamesUsingInInterReportFormula,
						interReportUsingFormulaErrorCount);
			}
		} catch (

		Exception e) {
			e.printStackTrace();
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Something went wrong", null, false);
		}

	}

	private ExtractInterReportValueFromreport extractInterReportValueForIfConduction(JsonArray ifConditionArray) {

		ExtractInterReportValueFromreport extractInterReportValueFromreport = new ExtractInterReportValueFromreport();

		JsonArray modifiedIfConditionArray = new JsonArray();
		Set<String> allInterParentReport = new HashSet<>();
		boolean isSubReportSelected = false;

		for (JsonElement condition : ifConditionArray) {

			JsonArray ifCondition = condition.getAsJsonArray();
			JsonArray modifiedIfCondition = new JsonArray();

			for (int i = 0; i < ifCondition.size(); i++) {
				JsonElement element = ifCondition.get(i);
				String operand = element.getAsString();

				if (operand.contains("~")) {
					String value = "0.0";
					String[] extractTextInTildes = FormulaCellReferenceExtractor.extractTextInTildes(operand);
					String interParentReportName = extractTextInTildes[0];
					String interReportCell = extractTextInTildes[1];
					String[] extractTextInBrackets = extractTextInBrackets(interReportCell);
					String interSubReportName = extractTextInBrackets[0];

					if (interSubReportName == null || interSubReportName.isEmpty()
							|| interSubReportName.equals("\"\"")) {
						isSubReportSelected = true;
						allInterParentReport.add(interParentReportName);
					}

					interReportCell = extractTextInBrackets[1];

					if (interSubReportName != null && !interSubReportName.equals("\"\"")) {
						Optional<SubReportData> subReportData = subReportRepository
								.findByReportName(interSubReportName);
						if (subReportData.isPresent() && subReportData.get().getStatus().equals(Status.COMPLETED)) {
							HashMap<String, Object> subReportObject = getSubReportObject(new Gson().toJsonTree(
									ConversionUtility.convertByteToObject(subReportData.get().getRequestObject())));
							JsonObject cellDetailsData = (JsonObject) subReportObject.get(interReportCell);

							if (cellDetailsData != null && cellDetailsData.has("cellDetails")) {
								JsonObject cellDetails = cellDetailsData.get("cellDetails").getAsJsonObject();
								if (cellDetails.has("value")) {
									value = cellDetails.get("value").getAsString();
									if (value.equals("")) {
										value = "0.0";
									}
								}
							}
						}
					}

					modifiedIfCondition.add(new JsonPrimitive(value));
				} else {

					modifiedIfCondition.add(element);
				}
			}

			// Add the modifiedIfCondition to the modifiedIfConditionArray
			modifiedIfConditionArray.add(modifiedIfCondition);
		}
		if (isSubReportSelected) {
			extractInterReportValueFromreport.setIsSubReportSelected(isSubReportSelected);
			extractInterReportValueFromreport.setAllInterParentReport(allInterParentReport);
		} else {
			extractInterReportValueFromreport.setIsSubReportSelected(isSubReportSelected);
		}
		extractInterReportValueFromreport.setModifiedIfConditionArray(modifiedIfConditionArray);

		return extractInterReportValueFromreport;
	}

	private ExtractInterReportValueFromreport extractInterReportValueForValidConduction(JsonArray validCondition) {

		ExtractInterReportValueFromreport extractInterReportValueFromreport = new ExtractInterReportValueFromreport();

		Set<String> allInterParentReport = new HashSet<>();
		boolean isSubReportSelected = false;

		JsonArray modifiedIfCondition = new JsonArray();

		for (int i = 0; i < validCondition.size(); i++) {
			JsonElement element = validCondition.get(i);
			String operand = element.getAsString();

			if (operand.contains("~")) {
				String value = "0.0";
				String[] extractTextInTildes = FormulaCellReferenceExtractor.extractTextInTildes(operand);
				String interParentReportName = extractTextInTildes[0];
				String interReportCell = extractTextInTildes[1];
				String[] extractTextInBrackets = extractTextInBrackets(interReportCell);
				String interSubReportName = extractTextInBrackets[0];

				if (interSubReportName == null || interSubReportName.isEmpty() || interSubReportName.equals("\"\"")) {
					isSubReportSelected = true;
					allInterParentReport.add(interParentReportName);
				}

				interReportCell = extractTextInBrackets[1];

				if (interSubReportName != null && !interSubReportName.equals("\"\"")) {
					Optional<SubReportData> subReportData = subReportRepository.findByReportName(interSubReportName);
					if (subReportData.isPresent() && subReportData.get().getStatus().equals(Status.COMPLETED)) {
						HashMap<String, Object> subReportObject = getSubReportObject(new Gson().toJsonTree(
								ConversionUtility.convertByteToObject(subReportData.get().getRequestObject())));
						JsonObject cellDetailsData = (JsonObject) subReportObject.get(interReportCell);

						if (cellDetailsData != null && cellDetailsData.has("cellDetails")) {
							JsonObject cellDetails = cellDetailsData.get("cellDetails").getAsJsonObject();
							if (cellDetails.has("value")) {
								value = cellDetails.get("value").getAsString();
								if (value.equals("")) {
									value = "0.0";
								}
							}
						}
					}
				}

				modifiedIfCondition.add(new JsonPrimitive(value));
			} else {

				modifiedIfCondition.add(element);
			}
		}

		// Add the modifiedIfCondition to the modifiedIfConditionArray

		if (isSubReportSelected) {
			extractInterReportValueFromreport.setIsSubReportSelected(isSubReportSelected);
			extractInterReportValueFromreport.setAllInterParentReport(allInterParentReport);
		} else {
			extractInterReportValueFromreport.setIsSubReportSelected(isSubReportSelected);
		}
		extractInterReportValueFromreport.setModifiedIfConditionArray(modifiedIfCondition);

		return extractInterReportValueFromreport;
	}

	private static boolean isNumeric(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String extractCellNames(String formula) {
		StringBuilder cellNames = new StringBuilder();
		Pattern cellPattern = Pattern.compile("[A-Z]+[0-9]+");

		// Find cell references
		Matcher cellMatcher = cellPattern.matcher(formula);
		while (cellMatcher.find()) {
			String cellRange = cellMatcher.group();
			if (cellRange.contains(":")) { // If the cell range contains a colon, expand it
				String[] rangeParts = cellRange.split(":");
				if (rangeParts.length > 0) {
					cellNames.append(" from, " + rangeParts[0] + " to " + rangeParts[1]);
				}
			} else {
				cellNames.append(" " + cellRange);
			}
		}

		// Handle nested functions
		if (formula.contains("(")) {
			int startIndex = formula.indexOf("(");
			int endIndex = formula.lastIndexOf(")");
			String innerFormula = formula.substring(startIndex + 1, endIndex);
			extractCellNames(innerFormula);
		}
		return cellNames.toString();
	}

	public static Integer findMatchingRowNumber(String cellReference, Set<Integer> allContinueRowNumber) {
		int rowNumber = Integer.parseInt(cellReference.replaceAll("\\D", ""));

		int continueRow = 0;
		for (Integer rowNum : allContinueRowNumber) {
			if (rowNum <= rowNumber) {
				if (continueRow < rowNum) {
					continueRow = rowNum;
				}
			}
		}
		return continueRow;
	}

	public static Integer findMatchingRowNumberV2(String cellReference, Set<Integer> allContinueRowNumber) {
		int rowNumber = Integer.parseInt(cellReference.replaceAll("\\D", ""));

		int continueRow = 0;
		for (Integer rowNum : allContinueRowNumber) {
			if (rowNum <= rowNumber) {
				if (continueRow < rowNum) {
					continueRow = rowNum;
				}
			}
		}

		int priviousIndexElement = 0;
		if (continueRow != 0) {
			for (int num : allContinueRowNumber) {
				if (num == continueRow) {
					continueRow = priviousIndexElement;
					break;
				}
				priviousIndexElement = num;
			}
		}
		return continueRow;
	}

	public static Integer findMatchingRowNumberForFormula(String cellReference, Set<Integer> allContinueRowNumber) {
		int rowNumber = Integer.parseInt(cellReference.replaceAll("\\D", ""));

		int continueRow = 0;
		for (Integer rowNum : allContinueRowNumber) {
			if (rowNum <= rowNumber) {
				if (continueRow < rowNum) {
					continueRow = rowNum;
				}
			}
		}

		if (continueRow == rowNumber) {
			continueRow = 0;
		}
		return continueRow;
	}

	public static Integer getValueByRowNumber(Map<String, CheckBreakKeyStatus> checkBreakStatus, int rowNumber,
			String sheetName) {

		int countNewrowAdded = 0;

		Map<String, Integer> highestDifferenceByRowNumber = getHighestDifferenceByRowNumber(checkBreakStatus);

		try {
			Integer rowForadd = highestDifferenceByRowNumber.get(sheetName + "-" + rowNumber);

			if (rowForadd != null) {

				countNewrowAdded = rowForadd;

			}
		} catch (Exception e) {

		}

//		for (Map.Entry<Integer, Integer> entry : highestDifferenceByRowNumber.entrySet()) {
//			Integer key = entry.getKey();
//			Integer value = entry.getValue();
//
//			if (key == rowNumber) {
//				countNewrowAdded += value;
//			} else {
//				countNewrowAdded += value;
//			}
//		}
		return countNewrowAdded;
	}

	public static Map<String, Integer> getHighestDifferenceByRowNumber(
			Map<String, CheckBreakKeyStatus> checkBreakStatus) {
		Map<String, Integer> highestDifferenceMap = new TreeMap<>();

		for (Map.Entry<String, CheckBreakKeyStatus> entry : checkBreakStatus.entrySet()) {
			String cellReference = entry.getKey();
			String sheetName = extractSheetName(cellReference); // Extract sheet name
			int cellRowNumber = extractRowNumber(cellReference);

			CheckBreakKeyStatus checkBreakKeyStatus = entry.getValue();

			if (checkBreakKeyStatus.getEndingIngCell() != null && !checkBreakKeyStatus.getEndingIngCell().equals("")) {
				int startIngIndex = Integer.parseInt(checkBreakKeyStatus.getColumnName().replaceAll("\\D", ""));
				int endingIndex = 0;
				if (checkBreakKeyStatus.getEndingIngCell() != null
						&& !checkBreakKeyStatus.getEndingIngCell().equals("")) {
					endingIndex = Integer.parseInt(checkBreakKeyStatus.getEndingIngCell().replaceAll("\\D", ""));
				}

				int range = endingIndex - startIngIndex;

				// Construct key with sheet name and row number
				String sheetRowKey = sheetName + "-" + cellRowNumber;

				int currentHighestDifference = highestDifferenceMap.getOrDefault(sheetRowKey, 0);
				if (range > currentHighestDifference) {
					highestDifferenceMap.put(sheetRowKey, range);
				}
			}
		}

		return highestDifferenceMap;
	}

	public static <T> List<T> getDataAfterIndex(List<T> dataList, int index) {
		if (index < 0 || index >= dataList.size()) {
			// Handle invalid index
			return new ArrayList<>();
		}
		return dataList.subList(index + 1, dataList.size());
	}

	private static String extractSheetName(String cellReference) {
		int hyphenIndex = cellReference.indexOf('-');
		if (hyphenIndex != -1) {
			return cellReference.substring(0, hyphenIndex);
		} else {
			return null;
		}
	}

	public static int extractRowNumber(String cellReference) {
		try {
			String[] parts = cellReference.split("-");
			String cellPart = parts[parts.length - 1];
			String rowString = cellPart.replaceAll("[^0-9]", "");
			return Integer.parseInt(rowString);
		} catch (Exception e) {
//			e.printStackTrace();
			return 0;
		}
	}

	public static boolean isValidCellName(String cellName) {
		// Define a regex pattern to match valid cell names (e.g., A1, B1, AbC3)
		Pattern pattern = Pattern.compile("^[A-Za-z]+\\d+$");
		Matcher matcher = pattern.matcher(cellName);

		// If the cell name matches the pattern, return true; otherwise, return false
		return matcher.matches();
	}

//	public static List<String> extractOperandsAndOperators(String formula) {
//		List<String> operandsAndOperators = new ArrayList<>();
//
//		// Define a regex pattern to match operands (cell references) with or without
//		// sheet names, operators, commas, and parentheses
//		Pattern pattern = Pattern.compile("\\b\\w+!\\w+|\\b[A-Z]+\\b|\\b\\w+\\d+|[+*/()-]");
//		Matcher matcher = pattern.matcher(formula);
//
//		// Iterate through matches and add them to the operandsAndOperators list
//		while (matcher.find()) {
//			operandsAndOperators.add(matcher.group());
//		}
//		
//		
//
//		return operandsAndOperators;
//	}
	public static List<String> extractOperandsAndOperators(String formula) {
		List<String> operandsAndOperators = new ArrayList<>();

		// Define a regex pattern to match operands (cell references) with or without
		// sheet names, operators, commas, and parentheses
		Pattern pattern = Pattern.compile("\\b\\w+!\\w+|\\b[A-Z]+\\b|\\b\\w+\\d+|[+*/()-]|\\b(?:0(?:\\.0)?)\\b");
		Matcher matcher = pattern.matcher(formula);

		// Iterate through matches and add them to the operandsAndOperators list
		while (matcher.find()) {
			operandsAndOperators.add(matcher.group());
		}

		return operandsAndOperators;
	}

	public static String[] extractSheetAndCell(String formula) {
		String[] sheetAndCell = new String[2];

		// Define a regex pattern to match sheet name and cell name
		Pattern pattern = Pattern.compile("([^!]+)!([A-Za-z]+\\d+)");
		Matcher matcher = pattern.matcher(formula);

		// If the pattern is found, store the sheet name and cell name in the array
		if (matcher.find()) {
			sheetAndCell[0] = matcher.group(1); // Sheet name
			sheetAndCell[1] = matcher.group(2); // Cell name
		}

		return sheetAndCell;
	}

	public List<String> extractComponents(String formula) {
		List<String> components = new ArrayList<>();

		// Split the formula string based on operators
		String[] parts = formula.split("(?=[+*/-])|(?<=[+*/-])");

		// Add the split parts to the components list
		components.addAll(Arrays.asList(parts));

		// Remove any empty strings from the components list
		components.removeIf(String::isEmpty);

		return components;
	}

	public Set<Integer> getAlltheContinueIndexBysheetName(String sheetName,
			Map<String, Integer> highestDifferenceByRowNumber) {
		Set<Integer> dataList = new TreeSet<>();

		for (String key : highestDifferenceByRowNumber.keySet()) {
			if (key.startsWith(sheetName)) {
				// Extract row number from the key
				String[] parts = key.split("-");
				if (parts.length == 2) {
					String cell = parts[1];
					// Assuming the format is "SheetName-Cell"
					String cellSheetName = parts[0];
					// Add the data to the list if it matches the sheet name
					if (cellSheetName.equals(sheetName)) {

						dataList.add(Integer.parseInt(cell.replaceAll("\\D", "")));
					}
				}
			}
		}

		return dataList;
	}

	public String replaceInterReportValuesInFormula(String formula, Map<Integer, String> interReportCellAndValue) {

		try {
			String modifiedFormula = formula;

			List<String> extractOperandsForInterReport = FormulaCellReferenceExtractor
					.extractOperandsForInterReport(modifiedFormula);
			// Iterate over each key in the map
			for (Integer key : interReportCellAndValue.keySet()) {

				extractOperandsForInterReport.set(key, interReportCellAndValue.get(key));
			}
			return extractOperandsForInterReport.stream().collect(Collectors.joining());
		} catch (Exception e) {

			e.printStackTrace();

			return "";
		}
	}

//	public Set<Integer> getAlltheContinueIndexBysheetName(String sheetName,
//			Map<String, Integer> highestDifferenceByRowNumber) {
//		return highestDifferenceByRowNumber.keySet().stream().filter(key -> key.startsWith(sheetName)).map(key -> {
//
//			String[] parts = key.split("-");
//			if (parts.length == 2) {
//				String cell = parts[1];
//				String cellSheetName = parts[0];
//				if (cellSheetName.equals(sheetName)) {
//					return Integer.parseInt(cell.replaceAll("\\D", ""));
//				}
//			}
//			return null;
//		}).filter(cellNumber -> cellNumber != null).collect(Collectors.toCollection(TreeSet::new));
//	}

}