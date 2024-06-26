package com.excel.dynamic.formula.util;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.ptg.AreaPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationConstraint.OperatorType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import com.excel.dynamic.formula.constant.Constant;
import com.excel.dynamic.formula.dto.DataValidationConstraintImpl;
import com.excel.dynamic.formula.dto.ParentReportSavedResponseDto;
import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.enums.CustomCellType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExcelValidation {

	private static Set<String> conditionalOperators = new HashSet<>(
			Arrays.asList(">", "<", "==", "=", "!=", ">=", "<="));

	private static Set<String> allOperators = new HashSet<>(
			Arrays.asList(">", "<", "==", "=", "!=", ">=", "<=", "+", "-", "*", "/", "%"));

	public static boolean isValidExcelFormula(Workbook wb, Sheet wbsheet, String formula, int rowIndex, int cellIndex,
			String cellName) {
		try (Workbook workbook = wb) {
			Row row = wbsheet.getRow(rowIndex + 1);
			row = row != null ? row : wbsheet.createRow(rowIndex);
			Cell cell = row.getCell(cellIndex);
			cell = cell != null ? cell : row.createCell(rowIndex);
			cell.setCellFormula(formula);
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			CellValue cellValue = evaluator.evaluate(cell);
			return true;
		} catch (FormulaParseException e) {
			// e.printStackTrace();
			return false;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public static boolean isValidExcelFormulaV2(Workbook wb, Sheet wbsheet, String formula, int rowIndex, int cellIndex,
			String cellName) {
		Boolean vallidFormulaCheck = null;

		try (Workbook workbook = wb) {

			vallidFormulaCheck = ValueErrorChecking.vallidFormulaCheck(formula);
			if (vallidFormulaCheck != null && !vallidFormulaCheck) {
				return false;
			}

			Row row = wbsheet.getRow(rowIndex + 1);
			row = row != null ? row : wbsheet.createRow(rowIndex);
			Cell cell = row.getCell(cellIndex);
			cell = cell != null ? cell : row.createCell(rowIndex);
			cell.setCellFormula(formula);
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			CellValue cellValue = evaluator.evaluate(cell);

			if (cellValue.getCellType() == CellType.ERROR
					&& (cellValue.formatAsString().equals(Constant.EXCEL_CIRCULAR_REF_ERROR)
							|| cellValue.formatAsString().equals(Constant.EXCEL_NULL_ERROR))) {
				return false;
			}

			if (cellValue.getCellType() == CellType.ERROR
					&& cellValue.formatAsString().equals(Constant.EXCEL_VALUE_ERROR)) {
				Boolean checkFormulaForValueError = EvaluateFormula.checkFormulaForValueError(formula);
				if (checkFormulaForValueError) {
					return false;
				} else {
//					if (!isCircularReference(wbsheet.getSheetName(), cellName, formula)) {
//						return false;
//					}
				}
			}

			if (!isCircularReference(wbsheet.getSheetName(), cellName, formula)) {
				return false;
			}

			return true;
		} catch (

		FormulaParseException e) {
			// e.printStackTrace();
			return false;
		} catch (Exception e) {
			if (vallidFormulaCheck != null && vallidFormulaCheck) {
				return true;
			}
			// e.printStackTrace();
			e.printStackTrace();
			return false;
		}
	}

//	public static boolean isValidExcelFormulaV2(Workbook wb, Sheet wbsheet, String formula, int rowIndex, int cellIndex,
//			String cellName) {
//		try (Workbook workbook = wb) {
//			Row row = wbsheet.getRow(rowIndex + 1);
//			row = row != null ? row : wbsheet.createRow(rowIndex);
//			Cell cell = row.getCell(cellIndex);
//			cell = cell != null ? cell : row.createCell(rowIndex);
//			cell.setCellFormula(formula);
//
//			// Parse the formula into tokens
//			Ptg[] ptgs = FormulaParser.parse(formula, null, FormulaType.CELL, wb.getSheetIndex(wbsheet));
//
//			// Extract cell references from tokens
//			List<String> cellReferences = extractCellReferences(ptgs);
//			if (cellReferences.contains(cellName)) {
//				return false;
//			}
//			// 
//			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
//			CellValue cellValue = evaluator.evaluate(cell);
//			return true;
//		} catch (FormulaParseException e) {
//			// e.printStackTrace();
//			return false;
//		} catch (Exception e) {
//			// e.printStackTrace();
//			return false;
//		}
//	}

	private static List<String> extractCellReferences(Ptg[] ptgs) {
		List<String> cellReferences = new ArrayList<>();
		for (Ptg ptg : ptgs) {
			if (ptg instanceof RefPtg) {
				RefPtg refPtg = (RefPtg) ptg;
				CellReference cellRef = new CellReference(refPtg.getRow(), refPtg.getColumn());
				cellReferences.add(cellRef.formatAsString());
			} else if (ptg instanceof AreaPtg) {
				AreaPtg areaPtg = (AreaPtg) ptg;
				CellReference firstCellRef = new CellReference(areaPtg.getFirstRow(), areaPtg.getFirstColumn());
				CellReference lastCellRef = new CellReference(areaPtg.getLastRow(), areaPtg.getLastColumn());

				// Iterate through all cells within the range and add their references
				for (int row = firstCellRef.getRow(); row <= lastCellRef.getRow(); row++) {
					for (int col = firstCellRef.getCol(); col <= lastCellRef.getCol(); col++) {
						CellReference cellRef = new CellReference(row, col);
						cellReferences.add(cellRef.formatAsString());
					}
				}
			}
		}
		return cellReferences;
	}

	private static class ExcelCellComparator implements Comparator<String> {
		@Override
		public int compare(String cellRef1, String cellRef2) {
			CellReference ref1 = new CellReference(cellRef1);
			CellReference ref2 = new CellReference(cellRef2);

			// First, compare row numbers
			int rowCompare = ref1.getRow() - ref2.getRow();
			if (rowCompare != 0) {
				return rowCompare;
			}

			// If rows are equal, compare column letters
			return ref1.getCol() - ref2.getCol();
		}
	}

	/// validate parent report object (formula)
//	public static List<ErrorResponseDto> checkFormulaAndValidations(String jsonString) {
//		try {
//
//			Workbook workbook = ExcelFormulaEvaluator.excelGenerateObjectFromJSON(jsonString, true);
//
//			byte[] bytes = jsonString.getBytes();
//			String requestData = new String(bytes);
//			JSONArray requestJsonArray = new JSONArray(requestData);
//			List<ErrorResponseDto> validationResponseList = new ArrayList<>();
//
//			if (requestJsonArray != null && requestJsonArray.length() > 0) {
//				for (Object requestObject : requestJsonArray) {
//					JSONObject requestSheetData = new JSONObject(requestObject.toString());
//					if (requestSheetData.has("sheetName")) {
//						String sheetName = requestSheetData.getString("sheetName");
//						if (requestSheetData.has("sheetData")) {
//							JSONArray rowDataArray = requestSheetData.getJSONArray("sheetData");
//
//							if (rowDataArray != null && rowDataArray.length() > 0) {
//								for (Object requestRowObject : rowDataArray) {
//									JSONObject requestRowDataObject = new JSONObject(requestRowObject.toString());
//									if (requestRowDataObject.has("rowData")) {
//										JSONArray cellDataArray = requestRowDataObject.getJSONArray("rowData");
//
//										if (cellDataArray != null && cellDataArray.length() > 0) {
//											for (Object cellObj : cellDataArray) {
//												JSONObject requestCellObject = new JSONObject(cellObj.toString());
//												long uniqueId = requestCellObject.getLong("uniqueId");
//												String cellName = requestCellObject.getString("cellName");
//
//												if (requestCellObject.has("cellDetails")) {
//													if (!requestCellObject.get("cellDetails").equals("")) {
//														JSONObject existingCellDetails = requestCellObject
//																.getJSONObject("cellDetails");
//														// validateCellDetails(requestCellObject,
//														// validationResponseList,
//														// sheetName, requestData, uniqueId, cellName);
//
//														if (existingCellDetails.has("hasFormula")) {
//															boolean hasFormula = existingCellDetails
//																	.getBoolean("hasFormula");
//
//															if (hasFormula && existingCellDetails.has("formula")) {
//																String formula = existingCellDetails
//																		.getString("formula");
//
//																if (formula.equals("")) {
//																	String errorMessage = "Formula can not empty if hasFormula is true."
//																			+ formula + ",Error offormula of sheet "
//																			+ sheetName + ", cellName " + cellName;
//
//																	validationResponseList
//																			.add(new ErrorResponseDto(uniqueId,
//																					sheetName, cellName, errorMessage));
////																return new ErrorResponseDto(
////																		uniqueId, sheetName, cellName, errorMessage);
//																} else {
//																	boolean excelFormula = isValidExcelFormula(workbook,
//																			workbook.getSheet(sheetName), formula,);
//
//																	if (!excelFormula) {
//																		String errorMessage = "Error of formula of sheet "
//																				+ sheetName + ", cellName " + cellName
//																				+ " with the Error Formula : "
//																				+ formula;
//																		validationResponseList.add(new ErrorResponseDto(
//																				uniqueId, sheetName, cellName,
//																				errorMessage));
//																	}
//
//																}
//															}
//														}
//													}
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
//			return validationResponseList;
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//
//	}

	// Extra formula
	// hasFormula
	// Validation

	/// validate parent report object (validation)
	public static Response<?> validateExcelData(ParentReportSavedResponseDto parentReportSaveDto) {

		String requestData = parentReportSaveDto.getResponseString();

		Workbook workbook = ExcelFormulaEvaluator.excelGenerateObjectFromJSON(requestData, true);

		JsonElement jsonElement = JsonParser.parseString(requestData);
		JsonArray requestJsonArray = jsonElement.getAsJsonArray();

		if (requestJsonArray != null && requestJsonArray.size() > 0) {
			for (JsonElement requestObject : requestJsonArray) {
				JsonObject requestSheetData = requestObject.getAsJsonObject();
				if (requestSheetData.has("sheetName")) {
					if (requestSheetData.has("sheetData")) {
						JsonArray rowDataArray = requestSheetData.getAsJsonArray("sheetData");

						if (rowDataArray != null && rowDataArray.size() > 0) {
							for (JsonElement requestRowObject : rowDataArray) {
								JsonObject requestRowDataObject = requestRowObject.getAsJsonObject();
								if (requestRowDataObject.has("rowData")) {
									JsonArray cellDataArray = requestRowDataObject.getAsJsonArray("rowData");

									if (cellDataArray != null && cellDataArray.size() > 0) {
										for (JsonElement cellObj : cellDataArray) {
											JsonObject requestCellObject = cellObj.getAsJsonObject();
											String sheetName = requestSheetData.get("sheetName").getAsString();
											String cellName = requestCellObject.get("cellName").getAsString();
											StringBuilder message = new StringBuilder();
											message.append(
													"Error in cell " + cellName + " of sheet " + sheetName + ", ");

											String headerName = requestCellObject.get("headerName").toString();
											if (requestCellObject.has("cellDetails")) {
												if (!requestCellObject.get("cellDetails").isJsonNull()
														&& requestCellObject.get("cellDetails").isJsonObject()) {
													JsonObject existingCellDetails = requestCellObject
															.getAsJsonObject("cellDetails");
													if (existingCellDetails.has("hasFormula")
															&& existingCellDetails.get("hasFormula").getAsBoolean()) {
														if (existingCellDetails.has("formula")) {
															String formula = existingCellDetails.get("formula")
																	.getAsString();
															if (!formula.isEmpty() && formula.startsWith("=")) {
																formula = formula.substring(1);
																existingCellDetails.addProperty("formula", formula);
															}
															if (!formula.trim().equals("")) {

																int rowIndex = existingCellDetails.get("rowIndex")
																		.getAsInt();
																int cellIndex = existingCellDetails.get("index")
																		.getAsInt();

																boolean interReportFormula = existingCellDetails
																		.has("isInterReportFormula")
																				? existingCellDetails
																						.get("isInterReportFormula")
																						.getAsBoolean()
																				: false;

																System.out.println(formula);
																
																Boolean isFormulaValid = true;
																if (interReportFormula) {																										

																	String formulaForInterReport = InterReportZeroAdjustedOperand(
																			formula);

																	isFormulaValid = isValidExcelFormulaV2(workbook,
																			workbook.getSheet(requestSheetData
																					.get("sheetName").getAsString()),
																			formulaForInterReport, rowIndex, cellIndex,
																			cellName);
																} else {
																	isFormulaValid = isValidExcelFormulaV2(workbook,
																			workbook.getSheet(requestSheetData
																					.get("sheetName").getAsString()),
																			formula, rowIndex, cellIndex, cellName);
																}

																if (!isFormulaValid) {
																	return new Response<>(
																			HttpStatus.BAD_REQUEST.value(),
																			message.append(
																					"Invalid formula. Given formula, "
																							+ formula)
																					.toString(),
																			null);
																}
															} else {
																return new Response<>(HttpStatus.BAD_REQUEST.value(),
																		message.append("Formula cannot be empty")
																				.toString(),
																		null);
															}
														} else {
															return new Response<>(HttpStatus.BAD_REQUEST.value(),
																	message.append("Formula key is missing").toString(),
																	null);
														}
													}

													if (existingCellDetails.has("hasExtraFormula")
															&& existingCellDetails.get("hasExtraFormula")
																	.getAsBoolean()) {
														if (existingCellDetails.has("extraFormula")) {
															JsonObject extraFormulaObject = existingCellDetails
																	.getAsJsonObject("extraFormula");
															if (extraFormulaObject != null) {

																if (extraFormulaObject.has("isMandatory")
																		&& extraFormulaObject.get("isMandatory")
																				.getAsBoolean()) {
																	if (headerName.equals("")) {
																		return new Response<>(
																				HttpStatus.BAD_REQUEST.value(),
																				message.append(
																						"Header name is required")
																						.toString(),
																				null);
																	}
																}

																if (extraFormulaObject.has("hasValidCondition")
																		&& extraFormulaObject.get("hasValidCondition")
																				.getAsBoolean()
																		&& extraFormulaObject.has("validCondition")) {

																	JsonArray validConditionsArray = extraFormulaObject
																			.get("validCondition").getAsJsonArray();

																	if (validConditionsArray.isJsonNull()
																			|| (!validConditionsArray.isJsonNull()
																					&& validConditionsArray.size() > 0
																					&& (!conditionalOperators.contains(
																							validConditionsArray.get(0)
																									.getAsString())
																							|| allOperators.contains(
																									validConditionsArray
																											.get(validConditionsArray
																													.size()
																													- 1)
																											.getAsString())))) {
																		return new Response<>(
																				HttpStatus.BAD_REQUEST.value(),
																				message.append(
																						"Please provide valid condition")
																						.toString(),
																				null);

																	}

																}

																if (extraFormulaObject.has("type")
																		&& extraFormulaObject.get("type").getAsString()
																				.equals(CustomCellType.DATE.name())) {
																	if (extraFormulaObject.has("dateFormat")) {
																		String dateFormat = extraFormulaObject
																				.get("dateFormat").getAsString();
																		if (dateFormat == null
																				|| dateFormat.equals("")) {
																			return new Response<>(
																					HttpStatus.BAD_REQUEST.value(),
																					message.append(
																							"Dateformat is required")
																							.toString(),
																					null);
																		}

																	} else {
																		return new Response<>(
																				HttpStatus.BAD_REQUEST.value(),
																				message.append("Dateformat is required")
																						.toString(),
																				null);
																	}

																}

																if (extraFormulaObject.has("range")
																		&& extraFormulaObject.get("range")
																				.getAsBoolean()) {
																	String minRange = extraFormulaObject.get("minRange")
																			.getAsString();
																	String maxRange = extraFormulaObject.get("maxRange")
																			.getAsString();
																	if ((minRange.isEmpty() || minRange.equals(""))
																			&& (maxRange.isEmpty()
																					|| maxRange.equals(""))) {
																		return new Response<>(
																				HttpStatus.BAD_REQUEST.value(),
																				message.append(
																						"Both min range and max range cannot be null")
																						.toString(),
																				null);
																	}

																	if (minRange != null && maxRange != null
																			&& !minRange.equals("")
																			&& !maxRange.equals("")
																			&& new BigDecimal(minRange).compareTo(
																					new BigDecimal(maxRange)) > 0) {
																		return new Response<>(
																				HttpStatus.BAD_REQUEST.value(),
																				message.append(
																						"Minimum range should be less than Maximum Range")
																						.toString(),
																				null);
																	}

																}

																if (extraFormulaObject.has("hasOptions")
																		&& extraFormulaObject.get("hasOptions")
																				.getAsBoolean()) {
																	if (extraFormulaObject.has("optionList")
																			&& !extraFormulaObject.get("optionList")
																					.isJsonNull()) {
																		JsonArray optionListArray = extraFormulaObject
																				.getAsJsonArray("optionList");
																		if (optionListArray == null) {
																			return new Response<>(
																					HttpStatus.BAD_REQUEST.value(),
																					message.append(
																							"Options in list is null")
																							.toString(),
																					null);
																		}
																		if (optionListArray.size() < 1) {
																			return new Response<>(
																					HttpStatus.BAD_REQUEST.value(),
																					message.append(
																							"Option list cannot be empty")
																							.toString(),
																					null);
																		}
																	} else {
																		return new Response<>(
																				HttpStatus.BAD_REQUEST.value(),
																				message.append(
																						"Option list key is missing")
																						.toString(),
																				null);
																	}
																}

															} else {
																return new Response<>(HttpStatus.BAD_REQUEST.value(),
																		message.append("Extra formula cannot be null")
																				.toString(),
																		null);
															}
														} else {
															return new Response<>(HttpStatus.BAD_REQUEST.value(),
																	message.append("Extra formula key is missing")
																			.toString(),
																	null);
														}
													}

												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return new Response<>(HttpStatus.OK.value(), "Validation checked successfully.", requestJsonArray.toString());
	}

	private static String InterReportZeroAdjustedOperand(String formula) {
		List<String> extractOperandsForInterReport = FormulaCellReferenceExtractor
				.extractOperandsForInterReport(formula);

		for (String operand : extractOperandsForInterReport) {

			if (operand.contains("~")) {
				int indexOf = extractOperandsForInterReport.indexOf(operand);
				extractOperandsForInterReport.set(indexOf, "0.0");

			}
		}
		return extractOperandsForInterReport.stream().collect(Collectors.joining());
	}

	public static Response<?> validateCellData(String validation) {

		String base64ToJsonValidation = ExcelConversion.Base64ToJson(validation);

		JSONObject jsonObject = new JSONObject(base64ToJsonValidation);

		String formula1 = jsonObject.getString("formula1");
		String formula2 = jsonObject.getString("formula2");
		int operator = jsonObject.getInt("operator");
		int validationType = jsonObject.getInt("ValidationType");
		JSONArray explicitListValues = jsonObject.getJSONArray("ExplicitListValues");

//	    //

		String[] explicitListArray = new String[explicitListValues.length()];
		for (int i = 0; i < explicitListValues.length(); i++) {
			if (!explicitListValues.isNull(i)) {
				explicitListArray[i] = explicitListValues.getString(i);
			} else {
				explicitListArray[i] = "";
			}
		}

		DataValidationConstraint constraint = new DataValidationConstraintImpl(validationType, operator, formula1,
				formula2, explicitListArray);
		return validateDataValidationConstraint(constraint);
	}

	private static Response<?> validateDataValidationConstraint(DataValidationConstraint constraint) {
		// Check if the constraint is null
		if (constraint == null) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Constraint is null", false);
		}

		// Get the validation type
		int validationType = constraint.getValidationType();

		// Get the formula 1 and formula 2
		String formula1 = constraint.getFormula1();
		String formula2 = constraint.getFormula2();

		// Get the explicit list values
		String[] explicitListValues = constraint.getExplicitListValues();

		// StringBuilder to hold response messages
		StringBuilder responseMessage = new StringBuilder();

		// Validate based on the validation type
		switch (validationType) {
		case DataValidationConstraint.ValidationType.ANY:
			// Any value type - no restriction
			responseMessage.append("Any value type - no restriction. ");
			return new Response<>(HttpStatus.OK.value(), responseMessage.toString(), true);
		case DataValidationConstraint.ValidationType.INTEGER:
			switch (constraint.getOperator()) {
			case OperatorType.BETWEEN:
			case OperatorType.NOT_BETWEEN:
				Response<Boolean> validInteger = isValidInteger(constraint.getFormula1());
				Response<Boolean> validInteger2 = isValidInteger(constraint.getFormula2());
				return (validInteger.getBooleanValue() && validInteger2.getBooleanValue())
						? new Response<>(validInteger.getResponseCode(), validInteger.getMessage(),
								validInteger.getBooleanValue())
						: new Response<>(validInteger.getResponseCode(), validInteger.getMessage(), false);
			default:
				validInteger = isValidInteger(constraint.getFormula1());
				return new Response<>(validInteger.getResponseCode(), validInteger.getMessage(),
						validInteger.getBooleanValue());
			}

		case DataValidationConstraint.ValidationType.DECIMAL:
			switch (constraint.getOperator()) {
			case OperatorType.BETWEEN:
			case OperatorType.NOT_BETWEEN:
				Response<Boolean> validDecimal = isValidDecimal(constraint.getFormula1());
				Response<Boolean> validDecimal2 = isValidDecimal(constraint.getFormula2());
				return (validDecimal.getBooleanValue() && validDecimal2.getBooleanValue())
						? new Response<>(validDecimal.getResponseCode(), validDecimal.getMessage(),
								validDecimal.getBooleanValue())
						: new Response<>(validDecimal.getResponseCode(), validDecimal.getMessage(), false);

			default:
				validDecimal = isValidDecimal(constraint.getFormula1());
				return new Response<>(validDecimal.getResponseCode(), validDecimal.getMessage(),
						validDecimal.getBooleanValue());
			}

		case DataValidationConstraint.ValidationType.LIST:
			return isValidateList(constraint.getFormula1(), explicitListValues);

		case DataValidationConstraint.ValidationType.DATE:
			switch (constraint.getOperator()) {
			case OperatorType.BETWEEN:
			case OperatorType.NOT_BETWEEN:
				Response<Boolean> validDate = isValidDate(constraint.getFormula1());
				Response<Boolean> validDate2 = isValidDate(constraint.getFormula2());
				return (validDate.getBooleanValue() && validDate2.getBooleanValue())
						? new Response<>(validDate.getResponseCode(), validDate.getMessage(),
								validDate.getBooleanValue())
						: new Response<>(validDate.getResponseCode(), validDate.getMessage(), false);
			default:
				validDate = isValidDate(constraint.getFormula1());
				return new Response<>(validDate.getResponseCode(), validDate.getMessage(), validDate.getBooleanValue());
			}
		case DataValidationConstraint.ValidationType.TIME:
			switch (constraint.getOperator()) {
			case OperatorType.BETWEEN:
			case OperatorType.NOT_BETWEEN:
				Response<Boolean> validTime = isValidTime(constraint.getFormula1());
				Response<Boolean> validTime2 = isValidTime(constraint.getFormula2());
				return (validTime.getBooleanValue() && validTime2.getBooleanValue())
						? new Response<>(validTime.getResponseCode(), validTime.getMessage(),
								validTime.getBooleanValue())
						: new Response<>(validTime.getResponseCode(), validTime.getMessage(), false);
			default:
				validTime = isValidTime(constraint.getFormula1());
				return new Response<>(validTime.getResponseCode(), validTime.getMessage(), validTime.getBooleanValue());
			}
		case DataValidationConstraint.ValidationType.TEXT_LENGTH:
			switch (constraint.getOperator()) {
			case OperatorType.BETWEEN:
			case OperatorType.NOT_BETWEEN:
				Response<Boolean> validStringLength = isValidStringLength(constraint.getFormula1());
				Response<Boolean> validStringLength2 = isValidStringLength(constraint.getFormula2());
				return (validStringLength.getBooleanValue() && validStringLength2.getBooleanValue())
						? new Response<>(validStringLength.getResponseCode(), validStringLength.getMessage(),
								validStringLength.getBooleanValue())
						: new Response<>(validStringLength.getResponseCode(), validStringLength.getMessage(), false);
			default:
				validStringLength = isValidStringLength(constraint.getFormula1());
				return new Response<>(validStringLength.getResponseCode(), validStringLength.getMessage(),
						validStringLength.getBooleanValue());
			}
		case DataValidationConstraint.ValidationType.FORMULA:
			boolean isValidFormula = isValidExcelFormula(null, null, formula1, 0, 0, null);

			if (isValidFormula) {
				responseMessage.append("Valid custom formula constraint: ").append(isValidFormula);
				return new Response<>(HttpStatus.OK.value(), responseMessage.toString(), isValidFormula);
			}

		default:
			isValidFormula = isValidExcelFormula(null, null, formula1, 0, 0, null);
			return new Response<>(HttpStatus.BAD_REQUEST.value(),
					"Invalid validation type - custom formula is not correct ", false);

		}
	}

	// Helper methods for validation
	private static Response<Boolean> isValidInteger(String value) {

		if (value.contains(".")) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(),
					"Value: " + value + " is not a valid integer (contains decimal point).", false);
		}
		if (value.equals("0")) {
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is a valid integer.", true);
		}
		try {
			Integer.parseInt(value);
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is a valid integer.", true);
		} catch (NumberFormatException e) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Value: " + value + " is not a valid integer.",
					false);
		}
	}

	private static Response<Boolean> isValidDecimal(String value) {
		String decimalOrIntegerPattern = "^-?(\\d+|\\d*\\.\\d+)$";
		if (value == null || value.equals("")) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(),
					"Value: " + value + " is not a valid decimal or integer.", false);
		}
		if (value.equals("0")) {
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is a valid decimal or integer .", true);
		}
		if (value.matches(decimalOrIntegerPattern)) {
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is a valid decimal or integer.", true);
		} else {
			return new Response<>(HttpStatus.BAD_REQUEST.value(),
					"Value: " + value + " is not a valid decimal or integer.", false);
		}
	}

	private static Response<Boolean> isValidateList(String formula1, String[] explicitListValues) {

		if (formula1 == null || explicitListValues == null) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Invalid list or formula input", false);
		}
		String[] formula1Values = formula1.split(",");
		if (formula1Values.length != explicitListValues.length) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(),
					"Size mismatch between formula values and explicit list values", false);
		}
		Set<String> explicitSet = new HashSet<>(Arrays.asList(explicitListValues));
		for (String value : formula1Values) {
			if (!explicitSet.contains(value.trim())) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Value: " + value + " is not present in the explicit list", false);
			}
		}
		return new Response<>(HttpStatus.OK.value(), "All values in formula match with explicit list", true);
	}

	private static Response<Boolean> isValidDate(String value) {
		if (value == null || value.equals("0")) {
			return new Response<>(HttpStatus.OK.value(), "Null date value", true);
		}
		try {
			LocalDate.parse(value); // Assuming date format is in ISO_LOCAL_DATE format
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is Valid date format", true);
		} catch (DateTimeParseException e) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Value: " + value + " is Invalid date format", false);
		}
	}

	private static Response<Boolean> isValidTime(String value) {
		if (value == null || value.equals("0")) {
			return new Response<>(HttpStatus.OK.value(), "Null time value", true);
		}
		try {
			LocalTime.parse(value); // Assuming time format is in ISO_LOCAL_TIME format
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is Valid time format", true);
		} catch (DateTimeParseException e) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Value: " + value + " is Invalid time format", false);
		}
	}

	private static Response<Boolean> isValidStringLength(String value) {
		if (value.contains(".")) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(),
					"Value: " + value + " is not a Valid String integer (contains decimal point).", false);
		}
		if (value.equals("0")) {
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is a Valid String integer.", true);
		}
		try {
			Integer.parseInt(value);
			return new Response<>(HttpStatus.OK.value(), "Value: " + value + " is a Valid String integer", true);
		} catch (NumberFormatException e) {
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Value: " + value + " is not a Valid String integer.",
					false);
		}
	}

//	public static List<ErrorResponseDto> validateExcelFormulaa(String data) {
//		List<ErrorResponseDto> errorList = new ArrayList<>();
//		JsonArray requestJsonArray = JsonParser.parseString(data).getAsJsonArray();
//		if (requestJsonArray != null && requestJsonArray.size() > 0) {
//			for (int i = 0; i < requestJsonArray.size(); i++) {
//				JsonObject requestSheetData = requestJsonArray.get(i).getAsJsonObject();
//				validateSheet(requestSheetData, errorList);
//			}
//		}
//		return errorList;
//	}
//
//	private static void validateSheet(JsonObject sheetData, List<ErrorResponseDto> errorList) {
//		if (sheetData.has("sheetName")) {
//			String sheetName = sheetData.get("sheetName").getAsString();
//			if (sheetData.has("sheetData")) {
//				JsonArray rowDataArray = sheetData.get("sheetData").getAsJsonArray();
//				if (rowDataArray != null && rowDataArray.size() > 0) {
//					for (int i = 0; i < rowDataArray.size(); i++) {
//						JsonObject requestRowDataObject = rowDataArray.get(i).getAsJsonObject();
//						validateRow(requestRowDataObject, errorList, sheetName);
//					}
//				}
//			}
//		}
//	}
//
//	private static void validateRow(JsonObject rowData, List<ErrorResponseDto> errorList, String sheetName) {
//		if (rowData.has("rowData")) {
//			JsonArray cellDataArray = rowData.get("rowData").getAsJsonArray();
//			if (cellDataArray != null && cellDataArray.size() > 0) {
//				for (int i = 0; i < cellDataArray.size(); i++) {
//					JsonObject requestCellObject = cellDataArray.get(i).getAsJsonObject();
//					validateCell(requestCellObject, errorList, sheetName);
//				}
//			}
//		}
//	}
//
//	private static void validateCell(JsonObject cellData, List<ErrorResponseDto> errorList, String sheetName) {
//		String headername = cellData.get("headerName").getAsString();
//		long uniqueId = cellData.get("uniqueId").getAsLong();
//		String cellName = cellData.get("cellName").getAsString();
//		if (cellData.has("cellDetails")) {
//			JsonObject inputCellDetailsObj = cellData.get("cellDetails").getAsJsonObject();
//			validateCellDetails(inputCellDetailsObj, errorList, sheetName, headername, uniqueId, cellName);
//		}
//	}
//
//	private static void validateCellDetails(JsonObject inputCellDetailsObj, List<ErrorResponseDto> errorList,
//			String sheetName, String headername, long uniqueId, String cellName) {
//		if (inputCellDetailsObj.has("hasValidation")) {
//			boolean inputHasValidation = inputCellDetailsObj.get("hasValidation").getAsBoolean();
//			if (inputHasValidation && inputCellDetailsObj.has("validation")) {
//				String validationString = inputCellDetailsObj.get("validation").getAsString();
//				JsonObject validationObj = JsonParser.parseString(validationString).getAsJsonObject();
//				validateCellValidation(validationObj, errorList, sheetName, headername, uniqueId, cellName);
//			}
//		}
//	}

	public static Response<?> validateCellValidation(JsonObject validationObj, String headername) {
		if (headername != null && !headername.equals("")) {
			Integer validationType = validationObj.get("ValidationType").getAsInt();
			String formula1 = validationObj.get("formula1").getAsString();
			String formula2 = validationObj.get("formula2").getAsString();
			JsonArray explicitListjsonArray = validationObj.get("ExplicitListValues").getAsJsonArray();

			switch (validationType) {
			case DataValidationConstraint.ValidationType.INTEGER:
			case DataValidationConstraint.ValidationType.DATE:
			case DataValidationConstraint.ValidationType.TIME:
			case DataValidationConstraint.ValidationType.DECIMAL:
			case DataValidationConstraint.ValidationType.TEXT_LENGTH:
				String errorMessage = null;
				try {
					errorMessage = ExcelValidation.validateByOperator(validationObj.get("operator").getAsInt(),
							formula1, formula2, headername, validationType);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (errorMessage != null) {
					return new Response<>(HttpStatus.BAD_REQUEST.value(), errorMessage, null);
				}
				break;

			case DataValidationConstraint.ValidationType.LIST:
				String[] formula1Values = formula1.split(",");
				String[] parts = headername.split("\\.");
				String headerName = parts[0];
				boolean matchFound = false;
				for (int i = 0; i < explicitListjsonArray.size(); i++) {
					if (headerName.equals(formula1Values[i])) {
						matchFound = true;
						break;
					}
				}

				if (!matchFound) {
					String message = "Value does not match with explicit list";
					return new Response<>(HttpStatus.BAD_REQUEST.value(), message, null);
				}

				break;
			}
		}

		return new Response<>(HttpStatus.OK.value(), "Sucess", null);
	}

	public static String validateByOperator(Integer operator, String formula1, String formula2, String headername,
			Integer validationType) throws ParseException {

		try {

			if (headername != null && !headername.equals("") && !headername.equals("#ERROR")) {

				switch (validationType) {
				case DataValidationConstraint.ValidationType.DATE:

					Date dateString = ExcelFormulaEvaluator.parseDateString(headername);

					if (dateString == null) {
						return "Invalid Date Format : " + headername;

					}
					double dateValue = ExcelFormulaEvaluator.convertToExcelDateValue(dateString) + 1;
					Date convertFromExcelDateValue1 = ExcelFormulaEvaluator
							.convertFromExcelDateValue(Integer.parseInt(formula1));
					Date convertFromExcelDateValue2 = ExcelFormulaEvaluator
							.convertFromExcelDateValue(Integer.parseInt(formula2));
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
					String format1 = dateFormat.format(convertFromExcelDateValue1);
					String format2 = dateFormat.format(convertFromExcelDateValue2);

					if (validationType == DataValidationConstraint.ValidationType.TIME) {
						dateFormat = new SimpleDateFormat("HH:mm:ss");
					}
					switch (operator) {
					case DataValidationConstraint.OperatorType.EQUAL:
						return (Double.parseDouble(formula1) == dateValue) ? null
								: "This error occurs because header date is not equal to cell Date";

					case DataValidationConstraint.OperatorType.GREATER_OR_EQUAL:
						return (Double.parseDouble(formula1) <= dateValue) ? null
								: "This error occurs because the " + headername
										+ " is not greater than or equal to the " + " " + format1;
					case DataValidationConstraint.OperatorType.GREATER_THAN:
						return (Double.parseDouble(formula1) < dateValue) ? null
								: "This error occurs because the " + " " + headername + " is not greater than the "
										+ " " + format1;
					case DataValidationConstraint.OperatorType.LESS_THAN:
						return (Double.parseDouble(formula1) > dateValue) ? null
								: "This error occurs because the " + " " + headername + " is not less than the " + " "
										+ format1;
					case DataValidationConstraint.OperatorType.LESS_OR_EQUAL:
						return (Double.parseDouble(formula1) >= dateValue) ? null
								: "This error occurs because the " + " " + headername
										+ " is not less than or equal to the " + " " + convertFromExcelDateValue1;
					case DataValidationConstraint.OperatorType.NOT_EQUAL:
						return (Double.parseDouble(formula1) != dateValue) ? null
								: "This error occurs because the " + " " + headername + " matches the " + " of "
										+ format1;
					case DataValidationConstraint.OperatorType.BETWEEN:
						double lowerBound = Double.parseDouble(formula1);
						double upperBound = Double.parseDouble(formula2);
						return (lowerBound <= dateValue && dateValue <= upperBound) ? null
								: "The " + " " + headername + " is not between the " + " " + format1 + " and "
										+ format2;

					case DataValidationConstraint.OperatorType.NOT_BETWEEN:
						double notBetweenLowerBound = Double.parseDouble(formula1);
						double notBetweenUpperBound = Double.parseDouble(formula2);
						return (dateValue < notBetweenLowerBound || dateValue > notBetweenUpperBound) ? null
								: "The " + headername + " is between the " + " " + format1 + " and " + format2;
					}

				case DataValidationConstraint.ValidationType.TEXT_LENGTH:
					int headerLength = 0;

					if (headername.contains(".")) {
						String[] split = headername.split("\\.");

						if (split.length > 1) {
							headername = split[0];
							headerLength = headername.length();
						}
					} else {
						headerLength = headername.length();
					}

					switch (operator) {
					case DataValidationConstraint.OperatorType.EQUAL:
						return (Double.parseDouble(formula1) == headerLength) ? null
								: "This error occurs due to the length of headerName " + headerLength
										+ " is not equal to the length of formula1 " + Double.parseDouble(formula1);
					case DataValidationConstraint.OperatorType.GREATER_OR_EQUAL:
						return (Double.parseDouble(formula1) <= headerLength) ? null
								: "This error occurs due to the length of headerName " + headerLength
										+ " is less than or equal to the length of formula1 "
										+ Double.parseDouble(formula1);
					case DataValidationConstraint.OperatorType.GREATER_THAN:
						return (Double.parseDouble(formula1) < headerLength) ? null
								: "This error occurs due to the length of headerName " + headerLength
										+ " is less than the length of formula1 " + Double.parseDouble(formula1);
					case DataValidationConstraint.OperatorType.LESS_THAN:
						return (Double.parseDouble(formula1) > headerLength) ? null
								: "This error occurs due to the length of formula1 " + Double.parseDouble(formula1)
										+ " is not less than the length of headerName " + headerLength;
					case DataValidationConstraint.OperatorType.LESS_OR_EQUAL:
						return (Double.parseDouble(formula1) <= headerLength) ? null
								: "This error occurs due to the length of formula1 " + Double.parseDouble(formula1)
										+ " is greater than the length of headerName " + headerLength;
					case DataValidationConstraint.OperatorType.NOT_EQUAL:
						return (Double.parseDouble(formula1) != headerLength) ? null
								: "This error occurs due to the length of formula1 " + Double.parseDouble(formula1)
										+ " is equal to the header length " + headerLength;
					case DataValidationConstraint.OperatorType.BETWEEN:
						return (Double.parseDouble(formula1) <= headerLength
								&& headerLength <= (Double.parseDouble(formula2))
										? null
										: "The length of formula1 " + headerLength + " is not between "
												+ Double.parseDouble(formula1) + " and "
												+ Double.parseDouble(formula2));
					case DataValidationConstraint.OperatorType.NOT_BETWEEN:
						return (headerLength < Double.parseDouble(formula1)
								|| headerLength > (Double.parseDouble(formula2))
										? null
										: "This error occurs due to the length of " + headerLength + " is between "
												+ Double.parseDouble(formula1) + " and "
												+ Double.parseDouble(formula2));
					}
					break;

				default:
					switch (operator) {
					case DataValidationConstraint.OperatorType.EQUAL:
						try {
							return (Double.parseDouble(formula1) == Double.parseDouble(headername)) ? null
									: "Formula1 (" + formula1 + ")  is not equal to (headerName) " + headername;
						} catch (Exception e) {
							return "Invalid Number Format ";
						}
					case DataValidationConstraint.OperatorType.GREATER_OR_EQUAL:
						try {
							return (Double.parseDouble(formula1) <= Double.parseDouble(headername)) ? null
									: "This error occurs due to the Cellvalue (" + headername
											+ ") is less than the from Formula1." + formula1;
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}
					case DataValidationConstraint.OperatorType.GREATER_THAN:
						try {
							return (Double.parseDouble(formula1) < Double.parseDouble(headername)) ? null
									: "This error occurs due to the CellValue (" + headername
											+ ") is not greater than  from formula1 " + formula1;
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}
					case DataValidationConstraint.OperatorType.LESS_THAN:
						try {
							return (Double.parseDouble(formula1) > Double.parseDouble(headername)) ? null
									: "This error occurs due to the CellValue (" + headername
											+ ") is not less than from formula1 " + formula1;
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}
					case DataValidationConstraint.OperatorType.LESS_OR_EQUAL:
						try {
							return (Double.parseDouble(formula1) >= Double.parseDouble(headername)) ? null
									: "This error occurs due to the Cellvalue (" + headername
											+ ") is greater than the from Formula1." + formula1;
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}
					case DataValidationConstraint.OperatorType.NOT_EQUAL:
						try {
							return (Double.parseDouble(formula1) != Double.parseDouble(headername)) ? null
									: "Formula1 (" + formula1 + ") is equal to the header value.";
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}
					case DataValidationConstraint.OperatorType.BETWEEN:
						try {
							return (Double.parseDouble(formula1) <= Double.parseDouble(headername)
									&& Double.parseDouble(headername) <= Double.parseDouble(formula2)) ? null
											: "This error occurs due to the CellValue " + headername
													+ " is not between " + formula1 + " and " + formula2;
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}

					case DataValidationConstraint.OperatorType.NOT_BETWEEN:
						try {
							return (Double.parseDouble(formula1) < Double.parseDouble(headername)
									|| Double.parseDouble(headername) > Double.parseDouble(formula2)) ? null
											: "This error occurs due to the Value of " + headername + " is between "
													+ formula1 + " and " + formula2;
						} catch (NumberFormatException e) {
							return "Invalid Number Format";
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String extractMatchPattern(String formula) {
		int matchStartIndex = formula.indexOf("MATCH");
		if (matchStartIndex != -1) {
			int openingParenthesisIndex = formula.indexOf("(", matchStartIndex);
			int closingParenthesisIndex = formula.indexOf(")", openingParenthesisIndex);

			String matchArgument = formula.substring(openingParenthesisIndex + 1, closingParenthesisIndex);

			String[] arguments = matchArgument.split(",");
			if (arguments.length >= 1) {
				String patternArgument = arguments[0].trim();
				if (patternArgument.startsWith("\"") && patternArgument.endsWith("\"")) {
					patternArgument = patternArgument.substring(1, patternArgument.length() - 1);
					String pattern = Arrays.toString(arguments);
					boolean matcher = pattern.contains(patternArgument);
					if (matcher) {
						return patternArgument;
					}
				}

			}
		}
		return null;
	}

	public static String[] extractPattern(String pattern) {

		if (pattern != null) {
			String[] parts = pattern.split("\\*");

			String[] extractedChars = new String[parts.length - 1];

			for (int i = 1; i < parts.length; i++) {
				extractedChars[i - 1] = parts[i].substring(0, 1);
			}

			return extractedChars;

		}
		return null;

	}

	public static Response<?> validateCell(JsonObject inputJson, String value) {
		try {
			String inputHeaderName = inputJson.get("headerName").getAsString().trim();
			inputHeaderName = value;
			StringBuilder errorMessage = new StringBuilder();

			JsonObject existingCellDetails = inputJson.getAsJsonObject("cellDetails");
			if (existingCellDetails.has("hasExtraFormula")
					&& existingCellDetails.get("hasExtraFormula").getAsBoolean()) {

				JsonObject extraFormulaDetails = existingCellDetails.getAsJsonObject("extraFormula");

				if ((extraFormulaDetails.has("isMandatory") && extraFormulaDetails.get("isMandatory").getAsBoolean())
						|| (inputHeaderName != null && !inputHeaderName.equals(""))) {

					if (extraFormulaDetails.has("hasOptions") && extraFormulaDetails.get("hasOptions").getAsBoolean()) {
						JsonArray optionsList = extraFormulaDetails.getAsJsonArray("optionList");
						List<String> optList = new ArrayList<>();

						for (JsonElement element : optionsList) {
							optList.add(element.getAsString());
						}
						if (!optList.contains(inputHeaderName)) {
							errorMessage.append("Value should be from the options only. ");
						}
					}

					if (extraFormulaDetails.has("range") && extraFormulaDetails.get("range").getAsBoolean()) {
						String minimumRange = extraFormulaDetails.get("minRange").getAsString();
						String maximumRange = extraFormulaDetails.get("maxRange").getAsString();

						BigDecimal minRange = null;
						BigDecimal maxRange = null;
						if (!minimumRange.equals("")) {
							minRange = new BigDecimal(minimumRange);
						}
						if (!maximumRange.equals("")) {
							maxRange = new BigDecimal(maximumRange);

						}

						String type = extraFormulaDetails.has("type") && !extraFormulaDetails.get("type").isJsonNull()
								? extraFormulaDetails.get("type").getAsString()
								: "";

//						if (isNumeric(inputHeaderName)) {
						if (!type.equals("") && type.equals(CustomCellType.NUMBER.name())
								&& !inputHeaderName.equals("")) {
//							Double inputHeaderValue = Double.valueOf(inputHeaderName);
							BigDecimal inputHeaderValue = new BigDecimal(inputHeaderName);

							if (minRange != null && maxRange == null) {
//								if (inputHeaderValue < minRange) {
								if (inputHeaderValue.compareTo(minRange) < 0) {
									errorMessage.append("Value cannot be less than " + minRange);
								}
							} else if (minRange == null && maxRange != null) {
								if (inputHeaderValue.compareTo(maxRange) > 0) {
									errorMessage.append("Value cannot be greater than " + maxRange);
								}
							} else {
								if ((inputHeaderValue.compareTo(minRange) < 0)
										|| (inputHeaderValue.compareTo(maxRange) > 0)) {
									errorMessage
											.append("The value should be between, " + minRange + " and " + maxRange);
								}
							}

						}
						// else if (isValidString(inputHeaderName)) {
						else if ((!type.equals("") && (type.equals(CustomCellType.TEXT.name())
								|| type.equals(CustomCellType.EMAIL.name()) || type.equals(CustomCellType.STD.name())
								|| type.equals(CustomCellType.PHONE.name()))) || type.equals("")) {
							BigDecimal inputHeaderValue = new BigDecimal(String.valueOf(inputHeaderName.length()));

							if (minRange != null && maxRange == null) {
								if (inputHeaderValue.compareTo(minRange) < 0) {
									errorMessage.append("Value length should less than " + minRange);
								}
							} else if (minRange == null && maxRange != null) {
								if (inputHeaderValue.compareTo(maxRange) > 0) {
									errorMessage.append("Value length should greater than " + maxRange);
								}
							} else if ((inputHeaderValue.compareTo(minRange) < 0)
									|| inputHeaderValue.compareTo(maxRange) > 0) {
								errorMessage.append("The length of value should be less than " + maxRange
										+ " and greater than, " + minRange);

							}
						} else {
//							if (minRange != null && maxRange == null) {
//								errorMessage.append("Value cannot be less than the range " + minRange.intValue());
//							} else if (minRange == null && maxRange != null) {
//								errorMessage.append("Value cannot be greater than the range " + maxRange.intValue());
//							} else {
//								errorMessage.append(
//										"Value must be in range " + minRange.intValue() + " & " + maxRange.intValue());
//							}
						}
					}
				}
			}
			if (!errorMessage.toString().isEmpty()) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(), errorMessage.toString(), null);
			} else {
				return new Response<>(HttpStatus.OK.value(), "success", null);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Something went wrong.", false);
		}
	}

	private static boolean isNumeric(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isValidString(String value) {
		return value.matches("[a-zA-Z0-9\\s]+");
	}

	public static boolean checkExcelFormat(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType != null) {
			return contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					|| contentType.equals("application/vnd.ms-excel");
		} else {
			return false;
		}
	}

	public static boolean isCircularReference(String sheetName, String cell, String formula) {
		List<String> extractedCells = extractCellNames(formula);
		if (extractedCells.contains(cell)) {
			return false;
		}

		for (String cellName : extractedCells) {
			String[] parts = cellName.split("!");
			if (parts.length == 2) {
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].replaceAll("'", "").equals(sheetName) && parts[1].equals(cell)) {
						return false;
					}

				}
			}
		}

		return true;
	}

	public static List<String> extractCellNames(String formula) {
		List<String> cellNames = new ArrayList<>();

//		Pattern pattern = Pattern.compile("([$]?[A-Za-z0-9_]+!)?[$]?[A-Za-z]+[$]?[0-9]+(:[$]?[A-Za-z]+[$]?[0-9]+)?");
//		Pattern pattern = Pattern.compile(
//				"([A-Za-z0-9_*]+(?:-[A-Za-z0-9_*]+)*)!([A-Z]+\\d+(:[A-Z]+\\d+)?)|([A-Z]+\\d+(?::[A-Z]+\\d+)?)");
		Pattern pattern = Pattern
				.compile("(?:'([^']+)'|([A-Za-z0-9_*]+(?:-[A-Za-z0-9_*]+)*))!([A-Z]+\\d+)(?::([A-Z]+\\d+))?");

		Matcher matcher = pattern.matcher(formula);

		while (matcher.find()) {
			String cellRef = matcher.group();
			if (cellRef.contains(":")) {

				String[] range = cellRef.split(":");
				String start = range[0].replaceAll("[*]", "");
				String end = range[1].replaceAll("[*]", "");
				cellNames.addAll(expandRange(start, end));
			} else {
				cellNames.add(cellRef.replaceAll("[*]", ""));
			}
		}

		return cellNames;
	}

//	private static List<String> expandRange(String start, String end) {
//		List<String> cellNames = new ArrayList<>();
//
//		String startCol = start.replaceAll("[0-9]", "");
//		
//		int startRow = Integer.parseInt(start.replaceAll("[A-Z]", ""));
//
//		String endCol = end.replaceAll("[0-9]", "");
//		int endRow = Integer.parseInt(end.replaceAll("[A-Z]", ""));
//
//		for (int row = startRow; row <= endRow; row++) {
//			for (char col = startCol.charAt(0); col <= endCol.charAt(0); col++) {
//				cellNames.add("" + col + row);
//			}
//		}
//		return cellNames;
//	}

	private static List<String> expandRange(String start, String end) {
		List<String> cellNames = new ArrayList<>();

		int startRow = Integer.parseInt(start.replaceAll("[^0-9]", ""));
		String startCol = start.replaceAll("[^A-Z]", "");

		int endRow = Integer.parseInt(end.replaceAll("[^0-9]", ""));
		String endCol = end.replaceAll("[^A-Z]", "");

		for (int row = startRow; row <= endRow; row++) {
			for (char col = startCol.charAt(0); col <= endCol.charAt(0); col++) {
				cellNames.add("" + col + row);
			}
		}
		return cellNames;
	}

}
