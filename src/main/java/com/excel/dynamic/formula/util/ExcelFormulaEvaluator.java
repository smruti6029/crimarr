package com.excel.dynamic.formula.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import com.excel.dynamic.formula.constant.Constant;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExcelFormulaEvaluator {

	public static Workbook excelGenerateObjectFromJSON(String json, boolean isForParent) {
		try {
//			JsonElement element = JsonParser.parseString(json);
			Gson gson = new Gson();
			JsonArray sheetsArray = gson.fromJson(json, JsonArray.class);

			Workbook workbook = new XSSFWorkbook();

			for (JsonElement sheet : sheetsArray) {
				if (sheet != null) {
					try {
						createExcelSheet(workbook, sheet, isForParent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			return workbook;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Object evaluateFormula(Workbook workbook, String sheetName, int rowIndex, int cellIndex,
			String formula) {
	

		try {
			Object result = null;

			Sheet sheetObj = workbook.getSheet(sheetName);

//				!= null ? workbook.getSheet(sheetName)
//				: workbook.createSheet(sheetName);
			Row rowObj = sheetObj.getRow(rowIndex + 1);
			rowObj = rowObj != null ? rowObj : sheetObj.createRow(rowIndex + 1);
			if (rowObj != null) {
				Cell cellObj = rowObj.getCell(cellIndex);
				cellObj = cellObj != null ? cellObj : rowObj.createCell(cellIndex);
				if (formula != null) {
					cellObj.setCellFormula(formula);
					if (cellObj != null) {
						result = cellFormulaEvaluator(workbook, cellObj);
					}
				}
			}
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	public static Object evaluateDataValidationFormula(Workbook workbook, String sheetName, int rowIndex, int cellIndex,
			JSONObject validationObj) {

		Object result = null;

		Sheet sheetObj = workbook.getSheet(sheetName);
		Row rowObj = sheetObj.getRow(rowIndex);
		rowObj = rowObj != null ? rowObj : sheetObj.createRow(rowIndex);
		if (rowObj != null) {
			Cell cellObj = rowObj.getCell(cellIndex);
			cellObj = cellObj != null ? cellObj : rowObj.createCell(cellIndex);
			if (validationObj != null) {

				String formula = validationObj.getString("formula1");
				cellObj.setCellFormula(formula);
				if (cellObj != null) {
					result = cellFormulaEvaluator(workbook, cellObj);
				}
			}
		}
		return result;
	}

	private static void createExcelSheet(Workbook workbook, JsonElement sheetData, boolean isForParent) {

		JsonObject sheetObject = sheetData.getAsJsonObject();
		String sheetName = sheetObject.get("sheetName").getAsString();
		JsonArray rowsArray = sheetObject.getAsJsonArray("sheetData");

		Sheet excelSheet = workbook.createSheet(sheetName);

		for (JsonElement rowData : rowsArray) {
			if (rowData != null) {
				createExcelRow(excelSheet, rowData, workbook, sheetName, isForParent);
			}
		}
	}

	private static void createExcelRow(Sheet excelSheet, JsonElement rowData, Workbook workbook, String sheetName,
			boolean isForParent) {
		JsonObject rowObject = rowData.getAsJsonObject();
		int rowNumber = rowObject.get("rowNumber").getAsInt();
		JsonArray cellArray = rowObject.getAsJsonArray("rowData");

		Row excelRow = excelSheet.createRow(rowNumber);

		for (JsonElement cell : cellArray) {
			JsonObject cellObject = cell.getAsJsonObject();
			if (cellObject != null && cellObject.get("cellDetails") != null
					&& (cellObject.get("cellDetails").isJsonObject() || cellObject.get("cellDetails").isJsonNull())) {
				createExcelCell(excelRow, cellObject, workbook, sheetName, isForParent);
			}
		}
	}

	private static void createExcelCell(Row excelRow, JsonObject cellData, Workbook workbook, String sheetName,
			boolean isForParent) {
		JsonObject cellValue = cellData.get("cellDetails").getAsJsonObject();
		String headerName = cellData.get("headerName").getAsString();
		if (cellValue.has("value") && cellValue.get("value") != null) {
			String value = cellValue.get("value").getAsString();
			headerName = value;
		}

		if (cellValue != null) {
			if (cellValue.has("index") && cellValue.has("rowIndex")) {
				int cellIndex = Integer.parseInt(cellValue.get("index").getAsString());
				int rowIndex = Integer.parseInt(cellValue.get("rowIndex").getAsString());
				excelRow.setRowNum(rowIndex);
				Cell excelCell = excelRow.createCell(cellIndex);
				if (headerName != null && !headerName.isEmpty() && !headerName.equals("")) {
					excelCell.setCellValue(headerName);
				}
				String cellType = cellValue.get("cellType") != null
						&& !cellValue.get("cellType").getAsString().equals("") ? cellValue.get("cellType").getAsString()
								: isNumeric(headerName) ? CellType.NUMERIC.name() : CellType.STRING.name();

				if (isNumeric(headerName)) {
					cellType = CellType.NUMERIC.name();
				}

				setCellType((XSSFCell) excelCell, cellType, headerName);
				if (!isForParent) {
					handleCellFormula(excelCell, cellValue);
				}
				handleCellValidation(excelCell, cellValue);

			}
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

	private static void handleCellFormula(Cell excelCell, JsonObject cellValue) {
		boolean hasFormula = cellValue.has("hasFormula")
				? !"false".equalsIgnoreCase(cellValue.get("hasFormula").getAsString())
				: false;

		if (hasFormula) {
			String formula = cellValue.has("formula") && !cellValue.get("formula").getAsString().equals("")
					? cellValue.get("formula").getAsString()
					: null;

			if (formula != null) {
				try {
					excelCell.setCellFormula(formula);
				} catch (Exception e) {

				}
			}
		}
	}

	private static void handleCellValidation(Cell excelCell, JsonObject cellValue) {

		boolean hasValidation = cellValue.has("hasValidation")
				? !"false".equalsIgnoreCase(cellValue.get("hasValidation").getAsString())
				: false;

		String validationString = "";

		JsonElement validationElement = cellValue.get("validation");

		if (hasValidation && validationElement != null && !validationElement.isJsonNull()) {

			validationString = cellValue.get("validation").getAsString();

			String base64ToJsonValidation = ExcelConversion.Base64ToJson(validationString);

			JsonObject jsonObjectValidation = (JsonObject) JsonParser.parseString(base64ToJsonValidation);

			if (hasValidation) {
				applyValidation(excelCell, jsonObjectValidation);
			}
		}
	}

	private static void applyValidation(Cell excelCell, JsonObject validationMap) {
		try {

			// Get the current sheet
			Sheet sheet = excelCell.getSheet();

			// Get the data validation helper
			DataValidationHelper dvHelper = sheet.getDataValidationHelper();

			// Define the cell range for validation (single cell)
			CellRangeAddressList addressList = new CellRangeAddressList(excelCell.getRowIndex(),
					excelCell.getRowIndex(), excelCell.getColumnIndex(), excelCell.getColumnIndex());
			// Create a data validation object
			DataValidationConstraint constraint = null;

			// Set validation properties
			int validationType = validationMap.has("ValidationType")
					? Integer.parseInt(validationMap.get("ValidationType").getAsString())
					: 0;
			String operator = validationMap.has("operator") ? validationMap.get("operator").getAsString() : "";
			String formula1 = validationMap.has("formula1") ? validationMap.get("formula1").getAsString() : "";
			String formula2 = validationMap.has("formula2") ? validationMap.get("formula2").getAsString() : "";
			String explicitList = "";

			JsonElement explicitListElement = validationMap.get("ExplicitListValues");

			if (validationType == DataValidationConstraint.ValidationType.LIST) {

				explicitList = "";
				if (explicitListElement != null && !explicitListElement.isJsonNull()) {
					if (explicitListElement.isJsonArray()) {
						JsonArray jsonArray = explicitListElement.getAsJsonArray();
						StringBuilder sb = new StringBuilder();
						for (JsonElement element : jsonArray) {
							if (!element.isJsonNull()) { // Check if element is not null
								sb.append(element.getAsString()).append(",");
							}
						}
						explicitList = sb.toString();
						if (explicitList.length() > 0) {
							explicitList = explicitList.substring(0, explicitList.length() - 1);
						}
					} else {
						explicitList = explicitListElement.getAsString();
					}
				}
			}

			switch (validationType) {
			case DataValidationConstraint.ValidationType.INTEGER:
				constraint = dvHelper.createIntegerConstraint(getOperatorType(operator), formula1, formula2);
				break;
			case DataValidationConstraint.ValidationType.DECIMAL:
				constraint = dvHelper.createDecimalConstraint(getOperatorType(operator), formula1, formula2);
				break;
			case DataValidationConstraint.ValidationType.DATE:
				constraint = dvHelper.createDateConstraint(getOperatorType(operator), formula1, formula2,
						Constant.DATE_FORMAT_DD_MM_YYYY_WITH_SLASH);
				break;
			case DataValidationConstraint.ValidationType.TEXT_LENGTH:
				constraint = dvHelper.createTextLengthConstraint(getOperatorType(operator), formula1, formula2);
				break;
			case DataValidationConstraint.ValidationType.TIME:
				constraint = dvHelper.createTimeConstraint(getOperatorType(operator), formula1, formula2);
				break;
			case DataValidationConstraint.ValidationType.LIST:
				if (explicitList.toString() != "null" && !explicitList.isEmpty()) {
					String[] explicitValues = explicitList.substring(1, explicitList.length() - 1).split(",");
					constraint = dvHelper.createExplicitListConstraint(explicitValues);
				}
				break;

			case DataValidationConstraint.ValidationType.FORMULA:
				if (formula1.isEmpty()) {
					break;
				}
				// Assuming dvHelper.createCustomConstraint accepts a formula
				String formula = formula1; // You can concatenate formula2 or handle it based on your requirements
				constraint = dvHelper.createCustomConstraint(formula);
				break;
			default:
				// For other cases, use the custom constraint
				// constraint = dvHelper.createCustomConstraint("");
				break;
			}

			if (constraint != null) {
				// Create the data validation object
				DataValidation dataValidation = dvHelper.createValidation(constraint, addressList);

				// Set the error message for invalid data
				dataValidation.createErrorBox("Invalid Entry", "Please enter valid data.");

				// Set the error style
				dataValidation.setShowErrorBox(true);

				// Apply the validation to the cell
				sheet.addValidationData(dataValidation);
			}

		} catch (Exception e) {
			// Handle exception
			e.printStackTrace();
		}
	}

	private static int getOperatorType(String operatorString) {
		switch (operatorString) {
		case "0":
			return DataValidationConstraint.OperatorType.BETWEEN;
		case "1":
			return DataValidationConstraint.OperatorType.NOT_BETWEEN;
		case "2":
			return DataValidationConstraint.OperatorType.EQUAL;
		case "3":
			return DataValidationConstraint.OperatorType.NOT_EQUAL;
		case "4":
			return DataValidationConstraint.OperatorType.GREATER_THAN;
		case "5":
			return DataValidationConstraint.OperatorType.LESS_THAN;
		case "6":
			return DataValidationConstraint.OperatorType.GREATER_OR_EQUAL;
		case "7":
			return DataValidationConstraint.OperatorType.LESS_OR_EQUAL;
		default:
			return DataValidationConstraint.OperatorType.IGNORED;
		}
	}

	private static void setCellType(XSSFCell cell, String cellType, String value) {
		if (!value.equals("")) {
			switch (cellType.toUpperCase()) {
			case "NUMERIC":
				if (isDateString(value)) {
					Date date = parseDateString(value);
					double dateValue = convertToExcelDateValue(date);
					cell.setCellValue(dateValue);
				} else {
					try {
						cell.setCellValue(Double.parseDouble(value));

					} catch (NumberFormatException e) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(value);
					}
				}
				break;
			case "STRING":
				cell.setCellType(CellType.STRING);
				cell.setCellValue(value);
				break;
			case "FORMULA":
//                cell.setCellType(CellType.FORMULA);
				break;
			case "BOOLEAN":
//			cell.setCellType(CellType.BOOLEAN);
				cell.setCellValue(Boolean.getBoolean(value));
				break;
			case "BLANK":
//			cell.setCellType(CellType.BLANK);
				break;
			default:
				break;
			}
		}

	}

	private static boolean isDateString(String value) {
		String[] dateFormats = { "yyyy-MM-dd", "dd/MM/yyyy", "dd-MMM-yyyy" };
		for (String format : dateFormats) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			dateFormat.setLenient(false);
			try {
				dateFormat.parse(value);
				return true;
			} catch (ParseException e) {

			}
		}
		return false;
	}

	private static Object cellFormulaEvaluator(Workbook workbook, Cell cell) {
		// Evaluate the formula cell
		FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		CellValue cellValue = evaluator.evaluate(cell);

		Object value = "";

		// Get the value of the cell
		switch (cellValue.getCellType()) {
		case NUMERIC:
			value = cellValue.getNumberValue();
			break;
		case STRING:
			value = cellValue.getStringValue();
			break;
		case BOOLEAN:
			value = cellValue.getBooleanValue();
			break;
		case ERROR:
			value = "#ERROR";
			break;
		default:
			//
		}

		return value;
	}

	public static Object validateFormula(Workbook workbook, String sheetName, String formula) {
		Sheet sheet = workbook.getSheet(sheetName);
		if (sheet == null) {
			throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in the workbook.");
		}

		for (Row row : sheet) {
			for (Cell cell : row) {
				if (cell.getCellType() == CellType.STRING) {
					return cell.getStringCellValue();

				} else if (cell.getCellType() == CellType.NUMERIC) {
					return cell.getNumericCellValue();
				} else if (cell.getCellType() == CellType.BOOLEAN) {
					return cell.getBooleanCellValue();
				}
			}

		}

		return "Formula not found in the specified worksheet.";
	}

	public static Object evaluateDataValidationFormula(Workbook workbook, String sheetName, int rowIndex, int cellIndex,
			String validationFormula) {
		Object result = null;
		Sheet sheetObj = workbook.getSheet(sheetName);
		Row rowObj = sheetObj.getRow(rowIndex);
		rowObj = rowObj != null ? rowObj : sheetObj.createRow(rowIndex);
		if (rowObj != null) {
			Cell cellObj = rowObj.getCell(cellIndex);
			cellObj = cellObj != null ? cellObj : rowObj.createCell(cellIndex);
			if (validationFormula != null && !validationFormula.isEmpty()) {
				result = evaluateValidationFormula(workbook, sheetObj, validationFormula);
			}
		}
		return result;
	}

	private static Object evaluateValidationFormula(Workbook workbook, Sheet sheet, String validationFormula) {
		Object value = null;
		FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

		Row tempRow = sheet.createRow(0); // Create a temporary row
		Cell tempCell = tempRow.createCell(0); // Create a temporary cell in the first column of the temporary row
		tempCell.setCellFormula(validationFormula); // Set the formula to the temporary cell

		CellValue cellValue = evaluator.evaluate(tempCell);

		if (cellValue != null) {
			switch (cellValue.getCellType()) {
			case NUMERIC:
				value = cellValue.getNumberValue();
				break;
			case STRING:
				value = cellValue.getStringValue();
				break;
			case BOOLEAN:
				value = cellValue.getBooleanValue();
				break;
			case ERROR:
				value = "#ERROR";
				break;
			default:
				//
			}
		}

		sheet.removeRow(tempRow);

		return value;
	}

	public static Object evaluateDataValidationFormula(Workbook workbook, String sheetName, int rowIndex,
			int cellIndex) {
		Object result = null;
		Sheet sheetObj = workbook.getSheet(sheetName);
		Row rowObj = sheetObj.getRow(rowIndex);
		rowObj = rowObj != null ? rowObj : sheetObj.createRow(rowIndex);
		if (rowObj != null) {
			Cell cellObj = rowObj.getCell(cellIndex);
			cellObj = cellObj != null ? cellObj : rowObj.createCell(cellIndex);
			DataValidation dataValidation = getDataValidation(sheetObj, rowIndex, cellIndex);
			if (dataValidation != null) {
				result = evaluateDataValidation(workbook, sheetObj, dataValidation);
			}
		}
		return result;
	}

	private static DataValidation getDataValidation(Sheet sheet, int rowIndex, int cellIndex) {
		DataValidationHelper dvHelper = sheet.getDataValidationHelper();
		for (DataValidation dataValidation : sheet.getDataValidations()) {
			CellRangeAddressList addressList = dataValidation.getRegions();
			for (CellRangeAddress range : addressList.getCellRangeAddresses()) {
				if (rowIndex >= range.getFirstRow() && rowIndex <= range.getLastRow()
						&& cellIndex >= range.getFirstColumn() && cellIndex <= range.getLastColumn()) {
					return dataValidation;
				}
			}
		}
		return null; // No data validation found for the specified cell
	}

	private static Object evaluateDataValidation(Workbook workbook, Sheet sheet, DataValidation dataValidation) {
		Object value = null;
		DataValidationConstraint constraint = dataValidation.getValidationConstraint();
		if (constraint != null) {
			if (constraint.getFormula1() != null) {
				// If there's a formula, you might want to handle it accordingly
				value = constraint.getFormula1();
			}
		}
		return value;
	}

	public static Date parseDateString(String dateString) {

		if (dateString.matches(".*\\b\\d{1,2}(st|nd|rd|th)\\b.*")) {
			dateString = dateString.replaceAll("(?<=\\d)(st|nd|rd|th)\\b", "");
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
			sdf.setLenient(false);
			return sdf.parse(dateString);
		} catch (ParseException e) {
			SimpleDateFormat[] alternativeFormats = { new SimpleDateFormat("dd/MM/yyyy"),
					new SimpleDateFormat("dd/MM/yy"), new SimpleDateFormat("dd-MM-yy"),
					new SimpleDateFormat("dd/MMM/yyyy"), new SimpleDateFormat("dd/MMM/yy"),

					new SimpleDateFormat("dd/MMM/yy"),

					new SimpleDateFormat("dd MMMM yyyy"), new SimpleDateFormat("dd-MMMM-yyyy"),

					new SimpleDateFormat("dd.MM.yyyy"), new SimpleDateFormat("dd/MMM/yyyy"),
					new SimpleDateFormat("dd MMM yyyy"), new SimpleDateFormat("dd-MM-yyyy"),

					new SimpleDateFormat("dd/MM/yy"), new SimpleDateFormat("dd.MM.yy"),
					new SimpleDateFormat("dd/MMM/yy"), new SimpleDateFormat("dd-MM-yy"),
					new SimpleDateFormat("dd/M/yy"), new SimpleDateFormat("dd-M-yy"), new SimpleDateFormat("dd.M.yy"),

					new SimpleDateFormat("d/M/yy"), new SimpleDateFormat("d.M.yy"), new SimpleDateFormat("d-M-yy"),
					new SimpleDateFormat("d/MM/yy"), new SimpleDateFormat("d-MM-yy"), new SimpleDateFormat("d.MM.yy"),
					new SimpleDateFormat("d MMM yyyy"), new SimpleDateFormat("dd MMMM yyyy"),
					new SimpleDateFormat("dd-MMMM-yyyy"),

					new SimpleDateFormat("yyyy/MM/dd"), new SimpleDateFormat("yyyy-MM-dd"),
					new SimpleDateFormat("yyyy.MM.dd"), new SimpleDateFormat("yyyy/MMM/dd"),
					new SimpleDateFormat("yyyy/MMMM/dd"), new SimpleDateFormat("yyyy/MM/dd"),
					new SimpleDateFormat("yy/MM/dd"), new SimpleDateFormat("yy-MM-dd"),
					new SimpleDateFormat("yy.MM.dd"), new SimpleDateFormat("yy/MMM/dd"), new SimpleDateFormat("yy/M/d"),
					new SimpleDateFormat("yy-M-d"), new SimpleDateFormat("yy/MM/dd"),
					new SimpleDateFormat("yyyy.MM.dd"), new SimpleDateFormat("yy/MM/dd"),
					new SimpleDateFormat("yy-MM-dd"), new SimpleDateFormat("yyyy/MMM/dd"),
					new SimpleDateFormat("yy/MMM/dd"), new SimpleDateFormat("yy/M/d"), new SimpleDateFormat("yy-M-d"),
					new SimpleDateFormat("yy.MM.dd"), new SimpleDateFormat("yy.M.d"),
					new SimpleDateFormat("yyyy/MMMM/dd"),

					new SimpleDateFormat("yyyy/dd/MM"), new SimpleDateFormat("yyyy-dd-MM"),
					new SimpleDateFormat("yyyy-dd-M"), new SimpleDateFormat("yyyy-d-MM"),
					new SimpleDateFormat("yyyy.dd.MM"), new SimpleDateFormat("yyyy/dd/MMM"),
					new SimpleDateFormat("yyyy/dd/MMMM"), new SimpleDateFormat("yyyy/d/MM"),
					new SimpleDateFormat("yyyy/dd/M"), new SimpleDateFormat("yy/dd/MM"),
					new SimpleDateFormat("yy/d/MM"), new SimpleDateFormat("yy/dd/M"), new SimpleDateFormat("yy-dd-MM"),
					new SimpleDateFormat("yy-d-MM"), new SimpleDateFormat("yy-dd-M"), new SimpleDateFormat("yy.dd.MM"),
					new SimpleDateFormat("yy/dd/MMM"), new SimpleDateFormat("yy/d/M"), new SimpleDateFormat("yy-d-M"),
					new SimpleDateFormat("yy/dd/MM"), new SimpleDateFormat("yyyy.dd.MM"),
					new SimpleDateFormat("yy/dd/MM"), new SimpleDateFormat("yy-dd-MM"),
					new SimpleDateFormat("yyyy/dd/MMM"), new SimpleDateFormat("yy/dd/MMM"),
					new SimpleDateFormat("yy/d/M"), new SimpleDateFormat("yy-d-M"), new SimpleDateFormat("yy.dd.MM"),
					new SimpleDateFormat("yy.M.d"), new SimpleDateFormat("yyyy/dd/MMMM"),
					new SimpleDateFormat("MM/dd/yyyy"), new SimpleDateFormat("MM-dd-yyyy"),
					new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("MM-dd-yy"), new SimpleDateFormat("MM/d/yy"),
					new SimpleDateFormat("M/d/yy"), new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("MM-dd-yy"),
					new SimpleDateFormat("MM/d/yy"), new SimpleDateFormat("M/d/yy"), new SimpleDateFormat("MM/dd/yyyy"),
					new SimpleDateFormat("MM-dd-yyyy"), new SimpleDateFormat("MM/dd/yy"),
					new SimpleDateFormat("MM-dd-yy"), new SimpleDateFormat("M/d/yy"), new SimpleDateFormat("M-d-yy"),

			};

			for (SimpleDateFormat format : alternativeFormats) {
				try {
					format.setLenient(false);
					Date parsedDate = format.parse(dateString);

					SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy");
					return outputFormat.parse(outputFormat.format(parsedDate));
				} catch (ParseException ex) {
				}
			}

			return null;
		}
	}

	public static double convertToExcelDateValue(Date javaDate) {
		// Excel base date (January 1, 1900)
		Calendar baseDate = Calendar.getInstance();
		baseDate.set(1900, Calendar.JANUARY, 1);

		// Create a calendar object and set it to the base date
		Calendar calendar = (Calendar) baseDate.clone();
		calendar.setTime(javaDate);

		// Calculate the number of days between the base date and the given date
		long millisecondsDifference = calendar.getTimeInMillis() - baseDate.getTimeInMillis();
		int daysDifference = (int) (millisecondsDifference / (1000 * 60 * 60 * 24));

		// Add 2 because Excel incorrectly considers 1900 as a leap year
		return daysDifference + 2;
	}

	public static Date convertFromExcelDateValue(int excelDateValue) {
		// Excel base date (January 1, 1900)
		Calendar baseDate = Calendar.getInstance();
		baseDate.set(1900, Calendar.JANUARY, 1);

		// Subtract 2 because Excel incorrectly considers 1900 as a leap year
		int daysDifference = excelDateValue - 2;

		// Create a calendar object and set it to the base date
		Calendar calendar = (Calendar) baseDate.clone();
		calendar.add(Calendar.DATE, daysDifference);

		return calendar.getTime();
	}

}
