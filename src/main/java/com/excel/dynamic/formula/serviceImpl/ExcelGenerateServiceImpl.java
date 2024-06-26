package com.excel.dynamic.formula.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.excel.dynamic.formula.constant.Constant;
import com.excel.dynamic.formula.enums.DateFormat;
import com.excel.dynamic.formula.model.ParentReportData;
import com.excel.dynamic.formula.model.SubReportData;
import com.excel.dynamic.formula.repository.ParentReportRepository;
import com.excel.dynamic.formula.repository.SubReportRepository;
import com.excel.dynamic.formula.service.ExcelGenerateService;
import com.excel.dynamic.formula.util.ConversionUtility;
import com.excel.dynamic.formula.util.ExcelConversion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class ExcelGenerateServiceImpl implements ExcelGenerateService {

	@Autowired
	private ParentReportRepository parentReportRepository;

	@Autowired
	private SubReportRepository subReportRepository;

	private String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
	private boolean autoShredColumns = false;

	@Override
	public void excelGenerateFromObject(String fileName, String json, HttpServletResponse response) {

		try {
//			json = json.replace("\\\"", "\"");	
			JsonElement element = JsonParser.parseString(json);
			JsonArray sheetsArray = element.getAsJsonArray();

			Workbook workbook = new SXSSFWorkbook();

			for (JsonElement sheet : sheetsArray) {
				if (sheet != null) {
					createExcelSheet(workbook, sheet);
				}
			}

			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
				workbook.write(byteArrayOutputStream);

				// Set response headers
				response.setContentType(EXCEL_MIME_TYPE);
				response.setHeader(CONTENT_DISPOSITION_HEADER, "attachment; filename=" + fileName + ".xlsx");

				response.getOutputStream().write(byteArrayOutputStream.toByteArray());
			} catch (IOException e) {
				handleIOException(e);
			}
		} catch (Exception e) {
			handleException(e);
		}
	}

	private void createExcelSheet(Workbook workbook, JsonElement sheetData) {

		JsonObject sheetObject = sheetData.getAsJsonObject();
		String sheetName = sheetObject.get("sheetName").getAsString();
		JsonArray rowsArray = sheetObject.getAsJsonArray("sheetData");

		Sheet excelSheet = workbook.createSheet(sheetName);

		for (JsonElement rowData : rowsArray) {
			if (rowData != null) {
				createExcelRow(excelSheet, rowData, workbook);
			}
		}
	}

	private void createExcelRow(Sheet excelSheet, JsonElement rowData, Workbook workbook) {
		JsonObject rowObject = rowData.getAsJsonObject();
		int rowNumber = rowObject.get("rowNumber").getAsInt();
		JsonArray cellArray = rowObject.getAsJsonArray("rowData");
		boolean isRowHidden = rowObject.has("isRowHidden") ? rowObject.get("isRowHidden").getAsBoolean() : false;

		Row excelRow = excelSheet.createRow(rowNumber);

		for (JsonElement cell : cellArray) {
			JsonObject cellObject = cell.getAsJsonObject();
			if (cellObject != null && cellObject.get("cellDetails") != null
					&& (cellObject.get("cellDetails").isJsonObject() || cellObject.get("cellDetails").isJsonNull())) {
				createExcelCell(excelRow, cellObject, workbook);
			}
		}
		if (isRowHidden) {
			excelRow.setZeroHeight(isRowHidden);
		}
	}

	private void createExcelCell(Row excelRow, JsonObject cellData, Workbook workbook) {

		JsonObject cellValue = cellData.get("cellDetails").getAsJsonObject();
		String headerName = cellData.get("headerName").getAsString();
		boolean isColumnHidden = cellData.has("isColumnHidden") ? cellData.get("isColumnHidden").getAsBoolean() : false;

		String value = cellValue.has("value") && !cellValue.get("value").getAsString().trim().equals("")
				? cellValue.get("value").getAsString()
				: null;
		if (value != null) {
			headerName = value;
		}
		if (cellValue != null) {
			if (cellValue.has("index") && cellValue.has("rowIndex")) {
				int cellIndex = Integer.parseInt(cellValue.get("index").getAsString());
				int rowIndex = Integer.parseInt(cellValue.get("rowIndex").getAsString());
				excelRow.setRowNum(rowIndex);
				Cell excelCell = excelRow.createCell(cellIndex);
				excelCell.setCellValue(headerName);

				String cellType = cellValue.get("cellType") != null
						&& !cellValue.get("cellType").getAsString().equals("") ? cellValue.get("cellType").getAsString()
								: isNumeric(headerName) ? CellType.NUMERIC.name() : CellType.STRING.name();

				if (isNumeric(headerName)) {
					cellType = CellType.NUMERIC.name();
				}

				String dateFormat = null;

				if (cellValue.has("hasExtraFormula") && cellValue.get("hasExtraFormula").getAsBoolean()
						&& cellValue.has("extraFormula")) {
					dateFormat = cellValue.get("extraFormula").getAsJsonObject().has("dateFormat")
							? cellValue.get("extraFormula").getAsJsonObject().get("dateFormat").getAsString()
							: null;
					if (dateFormat != null) {
						String dateParttan = dateFormat;
						DateFormat dateFormatEnum = Arrays.stream(DateFormat.values())
								.filter(format -> format.getKey().equals(dateParttan)).findFirst().orElse(null);
						dateFormat = dateFormatEnum != null ? dateFormatEnum.getFormat() : null;
					}
				}

				setCellType(excelCell, cellType, headerName, dateFormat);
				applyCellStyle(excelCell, cellValue, workbook);
				handleMergedCells(excelCell, cellValue);
				handleCellFormula(excelCell, cellValue);
				handleCellValidation(excelCell, cellValue);

				boolean hasOptions = handleCellValidationForOption(excelCell, cellValue);

				if (isColumnHidden) {
					excelRow.getSheet().setColumnHidden(cellIndex, isColumnHidden);
				}

				//// It's Use for Set row Height and column width
//				if (cellData.has("cellHeight")) {
//			        float cellHeight = cellData.get("cellHeight").getAsFloat();
//			        excelRow.setHeightInPoints(cellHeight);
//			    }
//
//			    if (cellData.has("cellWidth")) {
//			        int cellWidth = cellData.get("cellWidth").getAsInt();
//			        excelRow.getSheet().setColumnWidth(cellIndex, cellWidth);
//			    }

				if (!hasOptions)
					handleCellValidation(excelCell, cellValue);

				// Auto-size the column based on the content
				if (autoShredColumns) {
					excelRow.getSheet().autoSizeColumn(cellIndex);
				}
			}
		}

	}

	private void applyCellStyle(Cell excelCell, JsonObject cellValue, Workbook workbook) {
		Font font = workbook.createFont();
		CellStyle cellStyle = workbook.createCellStyle();

		boolean isBold = cellValue.has("isBold") ? !"false".equalsIgnoreCase(cellValue.get("isBold").getAsString())
				: false;

		font.setBold(isBold);

		boolean isItalic = cellValue.has("isItalic")
				? !"false".equalsIgnoreCase(cellValue.get("isItalic").getAsString())
				: false;

		font.setItalic(isItalic);

		String fontName = cellValue.has("textStyle") && !cellValue.get("textStyle").getAsString().equals("")
				? cellValue.get("textStyle").getAsString()
				: "Arial";

		font.setFontName(fontName);

		short fontSize = cellValue.has("fontSize") && !cellValue.get("fontSize").getAsString().equals("")
				? Short.parseShort(cellValue.get("fontSize").getAsString())
				: 10;

		font.setFontHeightInPoints(fontSize);

		cellStyle.setFont(font);

		// Font color
		XSSFColor fontColor = getColorFromCellData(cellValue, "fontColor");
		if (fontColor != null) {
			((XSSFCellStyle) cellStyle).getFont().setColor(fontColor);
		}

		// Background color
		XSSFColor bgColor = getColorFromCellData(cellValue, "bgColor");
		if (bgColor != null) {
			((XSSFCellStyle) cellStyle).setFillForegroundColor(bgColor);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}

		// Apply Border
		boolean hasBorders = cellValue.has("hasBorders")
				? !"false".equalsIgnoreCase(cellValue.get("hasBorders").getAsString())
				: false;
		if (hasBorders) {

			JsonObject borderDetails = cellValue.has("borderDetails") ? cellValue.get("borderDetails").getAsJsonObject()
					: null;

			cellStyle.setBorderBottom(BorderStyle.valueOf(borderDetails != null && borderDetails.has("borderBottom")
					&& !borderDetails.get("borderBottom").getAsString().equals("")
					&& !borderDetails.get("borderBottom").getAsString().equals("HAIR")
							? (borderDetails.get("borderBottom").getAsString())
							: BorderStyle.THIN.name()));
			cellStyle.setBorderTop(BorderStyle
					.valueOf(borderDetails.has("borderTop") && !borderDetails.get("borderTop").getAsString().equals("")
							&& !borderDetails.get("borderTop").getAsString().equals("HAIR")
									? (borderDetails.get("borderTop").getAsString())
									: BorderStyle.THIN.name()));
			cellStyle.setBorderLeft(BorderStyle.valueOf(
					borderDetails.has("borderLeft") && !borderDetails.get("borderLeft").getAsString().equals("")
							&& !borderDetails.get("borderLeft").getAsString().equals("HAIR")
									? (borderDetails.get("borderLeft").getAsString())
									: BorderStyle.THIN.name()));
			cellStyle.setBorderRight(BorderStyle.valueOf(
					borderDetails.has("borderRight") && !borderDetails.get("borderRight").getAsString().equals("")
							&& !borderDetails.get("borderRight").getAsString().equals("HAIR")
									? (borderDetails.get("borderRight").getAsString())
									: BorderStyle.THIN.name()));
		}

		// Text Alignment
		String textAlignment = cellValue.has("textAllignment")
				&& !cellValue.get("textAllignment").getAsString().equals("")
						? (cellValue.get("textAllignment").getAsString())
						: "GENERAL";
		cellStyle.setAlignment(HorizontalAlignment.valueOf(textAlignment.toUpperCase()));

		excelCell.setCellStyle(cellStyle);
	}

	private XSSFColor getColorFromCellData(JsonObject cellValue, String colorKey) {
		String color = cellValue.has(colorKey) && !cellValue.get(colorKey).getAsString().equals("")
				&& !cellValue.get(colorKey).getAsString().equals("N/A") ? cellValue.get(colorKey).getAsString() : null;

		return color != null ? getXssfColorFromHex(color) : null;
	}

	private void handleMergedCells(Cell excelCell, JsonObject cellValue) {
		boolean isMerged = cellValue.has("isMerged")
				? !"false".equalsIgnoreCase(cellValue.get("isMerged").getAsString())
				: false;

		if (isMerged) {
			int numRows = Integer.parseInt(cellValue.get("numberOfRowsMerged").getAsString());
			int numCols = Integer.parseInt(cellValue.get("numberOfColumnsMerged").getAsString());
			CellRangeAddress mergedRegion = new CellRangeAddress(excelCell.getRowIndex(),
					excelCell.getRowIndex() + numRows - 1, excelCell.getColumnIndex(),
					excelCell.getColumnIndex() + numCols - 1);

			try {
				excelCell.getSheet().addMergedRegion(mergedRegion);
			} catch (Exception e) {
				// e.printStackTrace();
				// Handle exception if merging fails
			}
		}
	}

	private void handleCellFormula(Cell excelCell, JsonObject cellValue) {
		boolean hasFormula = cellValue.has("hasFormula")
				? !"false".equalsIgnoreCase(cellValue.get("hasFormula").getAsString())
				: false;

		boolean isInterReportFormula = cellValue.has("isInterReportFormula")
				? !"false".equalsIgnoreCase(cellValue.get("isInterReportFormula").getAsString())
				: false;
		if (hasFormula && !isInterReportFormula) {
			String formula = cellValue.has("formula") && !cellValue.get("formula").getAsString().equals("")
					? cellValue.get("formula").getAsString()
					: null;

			if (formula != null) {
				if (!formula.isEmpty() && formula.startsWith("=")) {
					formula = formula.substring(1);
				}
				excelCell.setCellFormula(formula);
			}
		}
	}

	private void handleCellValidation(Cell excelCell, JsonObject cellValue) {

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

	private void applyValidation(Cell excelCell, JsonObject validationMap) {
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
					break;
				}

			case DataValidationConstraint.ValidationType.FORMULA:
				if (formula1.isEmpty()) {
					break;
				}
				formula1 = validationMap.get("formula1").getAsString();

				// Assuming dvHelper.createCustomConstraint accepts a formula
				String formula = formula1;
				constraint = dvHelper.createCustomConstraint(formula);
				break;
			default:
				// For other cases, use the custom constraint
				// constraint = dvHelper.createCustomConstraint("");
				break;
			}

			// Create the data validation object
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

	private int getOperatorType(String operatorString) {
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

	private void handleIOException(IOException e) {
		e.printStackTrace();
		// TODO: Handle IOException
	}

	private void handleException(Exception e) {
		e.printStackTrace();
		// TODO: Handle other exceptions
	}

	private static XSSFColor getXssfColorFromHex(String hexColor) {

		int[] clr = hexToRgb(hexColor);
		XSSFColor color = createXssfColor(clr[0], clr[1], clr[2]);
		return color;

	}

	private static XSSFColor createXssfColor(int red, int green, int blue) {
		XSSFColor xssfColor = new XSSFColor();
		xssfColor.setRGB(new byte[] { (byte) red, (byte) green, (byte) blue });
		xssfColor.setAuto(true);

		return xssfColor;
	}

	private static int[] hexToRgb(String hexColor) {
		// Remove the leading '#' character
		hexColor = hexColor.replace("#", "");

		// Parse the hex color to RGB components
		int red = Integer.parseInt(hexColor.substring(0, 2), 16);
		int green = Integer.parseInt(hexColor.substring(2, 4), 16);
		int blue = Integer.parseInt(hexColor.substring(4, 6), 16);

		return new int[] { red, green, blue };
	}

//	private static void setCellType(XSSFCell cell, String cellType, String value) {
//		
//		if(!isNumeric(value) && cellType.equals("NUMERIC")){
//			cellType = "STRING";
//		}
//			
//		
//		switch (cellType.toUpperCase()) {
//		case "NUMERIC":
////			cell.setCellType(CellType.NUMERIC);
//			if(isNumeric(value))
//			cell.setCellValue(Double.parseDouble(value));
//			break;
//		case "STRING":
////			cell.setCellType(CellType.STRING);
//			break;
//		case "FORMULA":
////                cell.setCellType(CellType.FORMULA);
//			break;
//		case "BOOLEAN":
////			cell.setCellType(CellType.BOOLEAN);
//			cell.setCellValue(Boolean.getBoolean(value));
//			break;
//		case "BLANK":
////			cell.setCellType(CellType.BLANK);
//			break;
//		default:
//			break;
//		}
//	}

	private static void setCellType(Cell cell, String cellType, String value, String dateFormat) {

		if (!value.equals("")) {

			dateFormat = dateFormat != null && !dateFormat.equals("") ? dateFormat : DateFormat.DD_MM_YYYY.getFormat();

			switch (cellType.toUpperCase()) {
			case "NUMERIC":
				if (isDateString(value)) {
//					Date date = ConversionUtility.convertDateFormatAsPatternTo(value, dateFormat,"dd-MMM-yyyy");
//					double dateValue = convertToExcelDateValue(date);
					cell.setCellValue(value);
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

	private static boolean isNumeric(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public void getJsonBYIDS(Long parentId, Long subReportId, HttpServletResponse response) {
		String json = null;
		String fileName = "Report";
		if (parentId != null && subReportId == null) {
			Optional<ParentReportData> parentReportData = parentReportRepository.findById(parentId);
			json = parentReportData.isPresent() && parentReportData.get().getRequestData() != null
					? (String) ConversionUtility.convertByteToString(parentReportData.get().getRequestData())
					: null;
			fileName = parentReportData.isPresent() ? parentReportData.get().getExcelFileName() : fileName;
		} else if (parentId != null && subReportId != null) {
			Optional<SubReportData> subReportData = subReportRepository.findByIdAndParentId(subReportId, parentId);
			json = subReportData.isPresent() && subReportData.get().getRequestObject() != null
					? (String) ConversionUtility.convertByteToString(subReportData.get().getRequestObject())
					: null;
			fileName = subReportData.isPresent() ? subReportData.get().getReportName() : fileName;
		}
		if (json != null) {
			excelGenerateFromObject(fileName, json, response);
		}
	}

//	public static Date parseDateString(String dateString) {
//		try {
//			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
//			return sdf.parse(dateString);
//		} catch (ParseException e) {
////			e.printStackTrace();
//			return null;
//		}
//	}

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

	private boolean handleCellValidationForOption(Cell excelCell, JsonObject cellValue) {

		JsonElement extraFormulaElement = cellValue.get("extraFormula");

		if (extraFormulaElement != null && extraFormulaElement.isJsonObject() && !extraFormulaElement.isJsonNull()) {

			JsonObject jsonObjectExtraFormula = extraFormulaElement.getAsJsonObject();

			//

			String hasOpt = jsonObjectExtraFormula.has("hasOptions") && jsonObjectExtraFormula.get("hasOptions") != null
					? jsonObjectExtraFormula.get("hasOptions").getAsString()
					: null;

			boolean hasOptions = hasOpt != null ? !"false".equalsIgnoreCase(hasOpt) : false;

			if (hasOptions) {
				applyValidationOptions(excelCell, jsonObjectExtraFormula);
				return hasOptions;
			} else {
				return hasOptions;
			}
		}
		return false;
	}

	private void applyValidationOptions(Cell excelCell, JsonObject validationMap) {
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
			int validationType = DataValidationConstraint.ValidationType.LIST;
			String explicitList = "";

			JsonElement explicitListElement = validationMap.get("optionList");
			//
			if (validationType == DataValidationConstraint.ValidationType.LIST) {

				explicitList = "";
				if (explicitListElement != null && !explicitListElement.isJsonNull()) {
					if (explicitListElement.isJsonArray()) {
						JsonArray jsonArray = explicitListElement.getAsJsonArray();
						StringBuilder sb = new StringBuilder();
						for (JsonElement element : jsonArray) {
							if (!element.isJsonNull()) { // Check if element is not null
								if ((sb.toString() + element.getAsString() + ",").toString().length() > 255) {
									break;
								}
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
			case DataValidationConstraint.ValidationType.LIST:

				if (explicitList != "null" && !explicitList.isEmpty()) {
					String[] explicitValues = explicitList.split(",");
					constraint = dvHelper.createExplicitListConstraint(explicitValues);
				}
				break;
			default:
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

}
