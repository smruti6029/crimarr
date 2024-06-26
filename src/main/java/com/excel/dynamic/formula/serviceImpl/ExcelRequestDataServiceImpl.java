package com.excel.dynamic.formula.serviceImpl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.excel.dynamic.formula.dto.Response;
import com.excel.dynamic.formula.enums.DateFormat;
import com.excel.dynamic.formula.model.Configuration;
import com.excel.dynamic.formula.repository.ConfigurationRepository;
import com.excel.dynamic.formula.service.ExcelRequestDataService;
import com.excel.dynamic.formula.util.CheckHiddenCells;
import com.excel.dynamic.formula.util.ConversionUtility;
import com.excel.dynamic.formula.util.EvaluateFormula;
import com.excel.dynamic.formula.util.ExcelConversion;
import com.excel.dynamic.formula.util.ExcelValidation;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExcelRequestDataServiceImpl implements ExcelRequestDataService {

	private Set<String> processedCells = new HashSet<>();

	@Autowired
	private ConfigurationRepository configurationRepository;

	@Override
	public Response<?> getExcelResponseObject(MultipartFile file) {
		try {

			if (file == null) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Please provide a valid excel file of format '.xls' or '.xlsx'!!", null);
			}

			double fileMaxSize = 0l;

			Optional<Configuration> fileMaxSizeFromDb = configurationRepository.findByKey("FILE_MAX_SIZE");

			if (fileMaxSizeFromDb.isPresent()) {
				fileMaxSize = fileMaxSizeFromDb.get().getValue() != null
						&& !fileMaxSizeFromDb.get().getValue().equals("")
								? Double.parseDouble(fileMaxSizeFromDb.get().getValue())
								: 1.5D;
			}

			double fileSize = file.getSize() / (Math.pow(1000d, 2));
			if (fileSize > fileMaxSize) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Please provide a valid excel file of format '.xls' or '.xlsx' upto size " + fileMaxSize
								+ " MB !!",
						null);
			}

			boolean isValidExcel = ExcelValidation.checkExcelFormat(file);
			if (!isValidExcel) {
				return new Response<>(HttpStatus.BAD_REQUEST.value(),
						"Please provide the excel file of format '.xls' or '.xlsx'!!", null);
			}

			ZipSecureFile.setMinInflateRatio(0);

			try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
				List<Map<String, Object>> sheetDataList = new java.util.concurrent.CopyOnWriteArrayList<>();

				ForkJoinPool pool = new ForkJoinPool();
				pool.submit(new ProcessSheetTask(workbook, 0, workbook.getNumberOfSheets(), sheetDataList)).join();

				Collections.sort(sheetDataList, Comparator.comparing(m -> (Integer) m.get("sheetSequence")));

				String responseData = generateResponseData(sheetDataList);

				processedCells.clear();

				return new Response<>(HttpStatus.OK.value(), "Success.", responseData);
			} catch (Exception e) {
				e.printStackTrace();
				return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error processing Excel file.", null);
			}
		} catch (Exception e) {
			return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong.", null);
		}
	}

//	private String generateResponseData(List<Map<String, Object>> sheetDataList) {
//		try {
//			ObjectMapper objectMapper = new ObjectMapper();
//			String jsonData = objectMapper.writeValueAsString(sheetDataList);
//			return jsonData;
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//			return "Error generating response data.";
//		}
//	}

	private String generateResponseData(List<Map<String, Object>> sheetDataList) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();

			StringWriter stringWriter = new StringWriter();
			JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(stringWriter);
			jsonGenerator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			objectMapper.writeValue(jsonGenerator, sheetDataList);
			System.gc();

			jsonGenerator.close();

			return stringWriter.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "Error generating response data.";
		}
	}

	private class ProcessSheetTask extends RecursiveAction {
		private final Workbook workbook;
		private final int startSheetIndex;
		private final int endSheetIndex;
		private final List<Map<String, Object>> sheetDataList;

		public ProcessSheetTask(Workbook workbook, int startSheetIndex, int endSheetIndex,
				List<Map<String, Object>> sheetDataList) {
			this.workbook = workbook;
			this.startSheetIndex = startSheetIndex;
			this.endSheetIndex = endSheetIndex;
			this.sheetDataList = sheetDataList;
		}

		@Override
		protected void compute() {
			if (endSheetIndex - startSheetIndex <= 1) {
				processSheet(workbook, startSheetIndex, sheetDataList, startSheetIndex + 1);
			} else {
				int mid = startSheetIndex + (endSheetIndex - startSheetIndex) / 2;
				ProcessSheetTask leftTask = new ProcessSheetTask(workbook, startSheetIndex, mid, sheetDataList);
				ProcessSheetTask rightTask = new ProcessSheetTask(workbook, mid, endSheetIndex, sheetDataList);
				invokeAll(leftTask, rightTask);
			}
		}

//		private void processSheet(Workbook workbook, int sheetIndex, List<Map<String, Object>> sheetDataList,
//				int sequenceNumber) {
//			Sheet sheet = workbook.getSheetAt(sheetIndex);
//			Map<String, Object> sheetData = new HashMap<>();
//			sheetData.put("sheetSequence", sequenceNumber);
//			sheetData.put("sheetName", sheet.getSheetName());
//
//			List<Map<String, Object>> rowDataList = new java.util.concurrent.CopyOnWriteArrayList<>();
//
//			AtomicInteger lastColumnNumber = new AtomicInteger(0);
//			sheet.forEach(sheetRow -> {
//				int lastCell = sheetRow.getLastCellNum();
//				if (lastCell > lastColumnNumber.get()) {
//					lastColumnNumber.set(lastCell);
//				}
//			});
//			int lastColumn = lastColumnNumber.get();
//			int lastRowNumber = sheet.getLastRowNum() + 1;
//
//			if (lastRowNumber < 5) {
//				lastRowNumber = 10;
//			}
//			for (int rowIndex = 0; rowIndex < lastRowNumber; rowIndex++) {
//				Row row = sheet.createRow(rowIndex);
//				Map<String, Object> rowData = new HashMap<>();
//				rowData.put("rowNumber", row.getRowNum() + 1);
//
//				List<Map<String, Object>> cellDataList = new java.util.concurrent.CopyOnWriteArrayList<>();
//
//				for (int cellIndex = 0; cellIndex < lastColumn; cellIndex++) {
//					Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//					Map<String, Object> cellData = new HashMap<>();
//
//					CellRangeAddress mergedRegion = getMergedRegion(sheet, row.getRowNum(), cellIndex);
//					if (mergedRegion != null && !isFirstCellInMergedRegion(mergedRegion, row.getRowNum(), cellIndex)) {
//
//					} else {
////						cellData.put("headerName", "" + ((cell != null && cell.getCellType() == CellType.FORMULA) ? ""
////								: (cell != null ? cell.toString() : "")) + "");
////						cellData.put("uniqueId", +(row.getRowNum() * row.getLastCellNum() + cellIndex + 1));
//
//						String cellValue = "";
//						if (cell != null) {
//							if (cell.getCellType() == CellType.FORMULA) {
//								cellValue = evaluateFormula(cell, workbook);
//							} else {
//								cellValue = cell.toString(); // Get the value
//							}
//						}
//						cellData.put("headerName", cellValue);
//						cellData.put("uniqueId", row.getRowNum() * row.getLastCellNum() + cellIndex + 1);
//
//						if (mergedRegion != null
//								&& isFirstCellInMergedRegion(mergedRegion, row.getRowNum(), cellIndex)) {
//							String cellName = CellReference.convertNumToColString(mergedRegion.getFirstColumn())
//									+ (mergedRegion.getFirstRow() + 1) + ":"
//									+ CellReference.convertNumToColString(mergedRegion.getLastColumn())
//									+ (mergedRegion.getLastRow() + 1);
//							cellData.put("cellName", "" + cellName + "");
//						} else {
//							String cellName = CellReference.convertNumToColString(cellIndex) + (row.getRowNum() + 1);
//							cellData.put("cellName", "" + cellName + "");
//						}
//
//						cellData.put("cellDetails", getCellDetails(cell, sheet, row.getRowNum(), cellIndex, row));
//
//						cellDataList.add(cellData);
//					}
//				}
//
//				rowData.put("rowData", cellDataList);
//				rowDataList.add(rowData);
//			}
//
//			sheetData.put("sheetData", rowDataList);
//			sheetDataList.add(sheetData);
//
//		}

		private void processSheet(Workbook workbook, int sheetIndex, List<Map<String, Object>> sheetDataList,
				int sequenceNumber) {
			Sheet sheet = workbook.getSheetAt(sheetIndex);
			Map<String, Object> sheetData = new HashMap<>();
			sheetData.put("sheetSequence", sequenceNumber);

			Optional<Configuration> borderColour = configurationRepository.findByKey("BORDER_GREEN_COLOR");

			String borderColourCode = "Green";
			if (borderColour.isPresent()) {

				String colorFamily = ExcelConversion.getColorFamily(borderColour.get().getValue());
				borderColourCode = colorFamily;
			}

			String sheetName = sheet.getSheetName();

//			if (sheetName.matches(".*[+\\-*/].*")) {
//				sheetName = sheetName.replaceAll("[+\\-*/]", "_");
//			}

			sheetData.put("sheetName", sheetName);
//			sheetData.put("sheetName", sheet.getSheetName());

			List<Map<String, Object>> rowDataList = new java.util.concurrent.CopyOnWriteArrayList<>();

			int lastRowNumber = sheet.getLastRowNum() + 1;

			if (lastRowNumber < 5) {
				lastRowNumber = 10;
			}

			AtomicInteger lastColumnNumber = new AtomicInteger(0);
			boolean isRowEnd = false;
			for (Row sheetRow : sheet) {
				int lastColumnCount = 0;
				int lastCell = sheetRow.getLastCellNum();
				int countRowBreakColour = 0;

				for (Cell cellValue : sheetRow) {
					int modifyedColumnNo = 0;
					String backgroundColorString = getRGBString(cellValue.getCellStyle() instanceof XSSFCellStyle
							? ((XSSFCellStyle) cellValue.getCellStyle()).getFillForegroundXSSFColor()
							: null);

					backgroundColorString = backgroundColorString != null
							? ExcelConversion.getColorFamily(backgroundColorString)
							: backgroundColorString;
					if (backgroundColorString.equals(borderColourCode)) {
						modifyedColumnNo = cellValue.getColumnIndex();
					}
					if (modifyedColumnNo > 0) {
						lastCell = modifyedColumnNo + 1;
						lastColumnCount = modifyedColumnNo + 1;
					}
					if (backgroundColorString != null && backgroundColorString.equals(borderColourCode)) {
						countRowBreakColour++;
					}
					if (lastCell > 0 && countRowBreakColour > 0 && lastCell == countRowBreakColour) {
						isRowEnd = true;
					}
					if (isRowEnd) {
						lastCell = lastColumnCount;
					}

				}

				if (lastCell > lastColumnNumber.get()) {
					lastColumnNumber.set(lastCell);
				}
			}
			int lastColumn = lastColumnNumber.get();
			if (lastColumn < 5) {
				lastColumn = 10;
			}

//			

			
			for (int rowIndex = 0; rowIndex < lastRowNumber; rowIndex++) {
				Row row = sheet.getRow(rowIndex);

				List<String> columnNameList = new ArrayList<>();

				row = row != null ? row : sheet.createRow(rowIndex);

				Map<String, Object> rowData = new HashMap<>();
				rowData.put("rowNumber", row.getRowNum() + 1);

				try {
					if (row.getZeroHeight()) {
						rowData.put("isRowHidden", "true");
					} else {
						rowData.put("isRowHidden", "false");
					}
				} catch (Exception e) {
					rowData.put("isRowHidden", "false");
				}

				List<Map<String, Object>> cellDataList = new java.util.concurrent.CopyOnWriteArrayList<>();

				int countRowBreakColour = 0;
				for (int cellIndex = 0; cellIndex < lastColumn; cellIndex++) {

					Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

					String backgroundColorString = getRGBString(cell.getCellStyle() instanceof XSSFCellStyle
							? ((XSSFCellStyle) cell.getCellStyle()).getFillForegroundXSSFColor()
							: null);
					backgroundColorString = backgroundColorString != null
							? ExcelConversion.getColorFamily(backgroundColorString)
							: backgroundColorString;

					if (backgroundColorString != null && backgroundColorString.equals(borderColourCode)) {
						countRowBreakColour++;
					}

					Map<String, Object> cellData = new HashMap<>();

//					cellData.put("columnName", cell.getAddress().toString().replaceAll("[^A-Za-z]", ""));

					if (sheet.isColumnHidden(cellIndex)) {
						cellData.put("isColumnHidden", "true");
					} else {
						cellData.put("isColumnHidden", "false");
						columnNameList.add(cell.getAddress().toString().replaceAll("[^A-Za-z]", ""));
					}

					//// It's Use for get row Height and column width
//					cellData.put("cellWidth", sheet.getColumnWidth(cellIndex));
//					cellData.put("cellHeight", row.getHeight());

					CellRangeAddress mergedRegion = getMergedRegion(sheet, row.getRowNum(), cellIndex);
					if (mergedRegion != null && !isFirstCellInMergedRegion(mergedRegion, row.getRowNum(), cellIndex)) {

					} else {
//						cellData.put("headerName", "" + ((cell != null && cell.getCellType() == CellType.FORMULA) ? ""
//								: (cell != null ? cell.toString() : "")) + "");
//						cellData.put("uniqueId", +(row.getRowNum() * row.getLastCellNum() + cellIndex + 1));

//						String cellValue = "";
//						if (cell != null) {
//							if (cell.getCellType() == CellType.FORMULA) {
//								String formulavlaue = evaluateFormula(cell, workbook);
//								if(!formulavlaue.equals("#ERROR")) {
//									cellValue = formulavlaue;
//								}
//							} else {
//								cellValue = cell.toString(); // Get the value
//							}
//						}

// percentage case handled

						int noOfColumnHidden = 0;
						int noOfRowHidden = 0;

						String cellValue = "";
						if (cell != null) {
							if (cell.getCellType() == CellType.FORMULA) {
								String formulavlaue = evaluateFormula(cell, workbook);
								if (!formulavlaue.equals("#ERROR")) {
									cellValue = formulavlaue;
								}
							} else if (cell.toString().contains(".")) {
								DataFormatter dataFormatter = new DataFormatter();
								String formattedValue = dataFormatter.formatCellValue(cell);
								cellValue = formattedValue;
							} else {
								cellValue = cell.toString();// Get the value
							}
						}

						String dateValueOfCell = null;

						if (cell != null) {
							dateValueOfCell = ConversionUtility.convertDateFormat(cell.toString());
							if (cell.getCellType() == CellType.FORMULA) {
								cellData.put("headerName", "");
							} else if (dateValueOfCell != null) {
								cellData.put("headerName", "");
							} else {
								cellValue = cellValue.contains("â¹")?cellValue.replace("â¹", "₹"):cellValue;
								cellData.put("headerName", cellValue);
							}
						} 
						cellData.put("uniqueId", row.getRowNum() * row.getLastCellNum() + cellIndex + 1);

						if (mergedRegion != null
								&& isFirstCellInMergedRegion(mergedRegion, row.getRowNum(), cellIndex)) {
							String cellName = CellReference.convertNumToColString(mergedRegion.getFirstColumn())
									+ (mergedRegion.getFirstRow() + 1) + ":"
									+ CellReference.convertNumToColString(mergedRegion.getLastColumn())
									+ (mergedRegion.getLastRow() + 1);

							List<String> mergeCellNames = EvaluateFormula.extractCellNames(cellName);

							noOfColumnHidden = CheckHiddenCells.checkHiddenColumns(mergeCellNames, sheet);
							noOfRowHidden = CheckHiddenCells.checkHiddenRows(mergeCellNames, sheet);

//							cellData.put("mergedCellNames", mergeCellNames);

							cellData.put("cellName", "" + cell.getAddress() + "");
						} else {
							String cellName = CellReference.convertNumToColString(cellIndex) + (row.getRowNum() + 1);
							cellData.put("cellName", "" + cellName + "");
						}

						cellData.put("cellDetails", getCellDetails(cell, sheet, row.getRowNum(), cellIndex, row,
								cellValue, dateValueOfCell, noOfColumnHidden, noOfRowHidden));
//						cellData.put("cellDetails", getCellDetails(cell, sheet, row.getRowNum(), cellIndex, row,
//								cellValue, dateValueOfCell));

						cellDataList.add(cellData);
					}
				}

				rowData.put("columnNameList", columnNameList);

				rowData.put("rowData", cellDataList);
				rowDataList.add(rowData);

				if (lastColumn == countRowBreakColour) {
					break;
				}

			}

			sheetData.put("sheetData", rowDataList);
			sheetDataList.add(sheetData);

		}
	}

	public String evaluateFormula(Cell cell, Workbook workbook) {
		String cellValue = "";
		if (cell != null) {
			try {
				FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
				CellValue cellEvaluated = evaluator.evaluate(cell);
				switch (cellEvaluated.getCellType()) {
				case BOOLEAN:
					cellValue = String.valueOf(cellEvaluated.getBooleanValue());
					break;
				case NUMERIC:
					cellValue = String.valueOf(cellEvaluated.getNumberValue());
					break;
				case STRING:
					cellValue = cellEvaluated.getStringValue();
					break;
				case BLANK:
					cellValue = "";
					break;
				case ERROR:
					cellValue = "#ERROR";
					break;
				default:
					cellValue = ""; // handle other types if needed
					break;
				}
			} catch (NotImplementedException e) {
				cellValue = "#NOT_IMPLEMENTED";
			} catch (Exception e) {
				// Handle other exceptions if necessary
				cellValue = "#ERROR";
			}
		}
		return cellValue;
	}

	private boolean isFontBold(Font font) {
		return font != null && font.getBold();
	}

	private boolean isFontItalic(Font font) {
		return font != null && font.getItalic();
	}

	private String getCellKey(Cell cell, String sheetName) {
		return sheetName + "-" + cell.getRowIndex() + "-" + cell.getColumnIndex();
	}

	private boolean isCellProcessed(String cellKey) {
		return processedCells.contains(cellKey);
	}

	// get call background color in rgb string format
	private String getRGBString(XSSFColor color) {
		if (color != null) {
			byte[] rgb = color.getRGB();
			if (rgb != null && rgb.length == 3) {
				return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
			}
		}
		return "N/A";
	}

	private Boolean isCellMerged(Sheet sheet, int row, int column) {
		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
			if (mergedRegion.isInRange(row, column)) {
				return true;
			}
		}
		return false;
	}

	private String getMergedCellDetails(Sheet sheet, int rowNumber, int columnIndex, Map<String, Object> cellDetailsMap,
			int noOfColumnHidden, int noOfRowHidden) {
		boolean isCellMerged = isCellMerged(sheet, rowNumber, columnIndex);

		if (isCellMerged) {
			for (int k = 0; k < sheet.getNumMergedRegions(); k++) {
				CellRangeAddress mergedRegion = sheet.getMergedRegion(k);

				if (mergedRegion.isInRange(rowNumber, columnIndex)) {
					int numberOfMergedRows = mergedRegion.getLastRow() - mergedRegion.getFirstRow() + 1;
					int numberOfMergedColumns = mergedRegion.getLastColumn() - mergedRegion.getFirstColumn() + 1;

					cellDetailsMap.put("isMerged", "" + isCellMerged + "");
					cellDetailsMap.put("numberOfRowsMerged", numberOfMergedRows);
					cellDetailsMap.put("numberOfColumnsMerged", numberOfMergedColumns);
					cellDetailsMap.put("numberOfRowsHidden", noOfRowHidden);
					cellDetailsMap.put("numberOfColumnsHidden", noOfColumnHidden);

					return "isMerged:" + "" + isCellMerged + "" + "," + "numberOfRowsMerged:" + numberOfMergedRows + ","
							+ "numberOfColumnsMerged:" + numberOfMergedColumns + "" + "," + "numberOfRowsHidden:"
							+ noOfRowHidden + "," + "numberOfColumnsHidden:" + noOfColumnHidden + "";
				}
			}
		}

		return "isMerged:" + "false" + "," + "numberOfRowsMerged:0," + "numberOfColumnsMerged:0";
	}

	private boolean hasCellBorders(CellStyle cellStyle) {
		// Check if the cell has any borders
		return cellStyle.getBorderTop() != BorderStyle.NONE || cellStyle.getBorderRight() != BorderStyle.NONE
				|| cellStyle.getBorderBottom() != BorderStyle.NONE || cellStyle.getBorderLeft() != BorderStyle.NONE;
	}

	private Map<String, Object> getBorderDetails(CellStyle cellStyle, boolean hasBorders) {
		Map<String, Object> borderDetails = new HashMap<>();
		if (hasBorders) {

			borderDetails.put("borderTop", cellStyle.getBorderTop());
			borderDetails.put("borderRight", cellStyle.getBorderRight());
			borderDetails.put("borderBottom", cellStyle.getBorderBottom());
			borderDetails.put("borderLeft", cellStyle.getBorderLeft());

		}
		return borderDetails;
	}

	private CellRangeAddress getMergedRegion(Sheet sheet, int rowNum, int colNum) {
		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress merged = sheet.getMergedRegion(i);
			if (merged.isInRange(rowNum, colNum)) {
				return merged;
			}
		}
		return null;
	}

	private boolean isFirstCellInMergedRegion(CellRangeAddress mergedRegion, int rowNumber, int cellIndex) {
		return mergedRegion.getFirstRow() == rowNumber && mergedRegion.getFirstColumn() == cellIndex;
	}

	private DataValidation findDataValidationForCell(Sheet sheet, Cell cell) {
		DataValidation dataValidation = null;
		List<? extends DataValidation> validations = sheet.getDataValidations();
		CellAddress cellAddress = cell.getAddress();
		String validationAddInCell = null;
		String validationPresentInCell = null;
		for (DataValidation validation : validations) {
			validationPresentInCell = validation.getRegions().getCellRangeAddresses()[0].formatAsString();
			CellRangeAddressList addressList = validation.getRegions();
			for (CellRangeAddress rangeAddress : addressList.getCellRangeAddresses()) {
				if (rangeAddress.isInRange(cellAddress)) {
					validationAddInCell = cellAddress.formatAsString();
					dataValidation = validation;
					break;
				}
			}
			if (validationAddInCell != null && validationPresentInCell != null
					&& !validationAddInCell.equals(validationPresentInCell) && dataValidation != null) {
				break;
			}
		}
		return dataValidation;
	}

	private String getTextAlignment(HorizontalAlignment alignment) {
		if (alignment == null) {
			return "GENERAL";
		}
		switch (alignment) {
		case CENTER:
			return "CENTER";
		case LEFT:
			return "LEFT";
		case RIGHT:
			return "RIGHT";
		case JUSTIFY:
			return "JUSTIFY";
		default:
			return "GENERAL";
		}
	}

//	private Map<String, Object> getCellDetails(Cell cell, Sheet sheet, int rowNumber, int cellIndex, Row row,
//			String cellValue, String dateValueOfCell) {
	private Map<String, Object> getCellDetails(Cell cell, Sheet sheet, int rowNumber, int cellIndex, Row row,
			String cellValue, String dateValueOfCell, int noOfColumnHidden, int noOfRowHidden) {

		Map<String, Object> cellDetailsMap = new HashMap<>();

		if (cell == null) {
			cellDetailsMap.put("rowIndex", rowNumber);
			cellDetailsMap.put("index", cellIndex);
			return cellDetailsMap;
		}

		CellStyle cellStyle = cell.getCellStyle();
		Workbook workbook = sheet.getWorkbook();
		Font font = workbook.getFontAt(cellStyle.getFontIndexAsInt());

		String cellKey = getCellKey(cell, sheet.getSheetName());
		if (isCellProcessed(cellKey)) {
			return cellDetailsMap;
		}
		processedCells.add(cellKey);

		// Handle cell alignment
		cellDetailsMap.put("textAlignment", cellStyle.getAlignment());

		// Handle font properties
		if (cellStyle.getFontIndexAsInt() != 0) {
			cellDetailsMap.put("isBold", "" + isFontBold(font) + "");
			cellDetailsMap.put("isItalic", "" + isFontItalic(font) + "");
			cellDetailsMap.put("fontSize", font.getFontHeightInPoints());
			cellDetailsMap.put("textStyle", font.getFontName());
		}

		cellDetailsMap.put("marginLeft", (row.getHeightInPoints() - cellStyle.getIndention()));
		cellDetailsMap.put("marginRight",
				(row.getHeightInPoints() - (row.getHeightInPoints() - cellStyle.getIndention())));

		// Get font color as RGB string
		String fontColorString = getRGBString(
				cellStyle instanceof XSSFCellStyle ? ((XSSFCellStyle) cellStyle).getFont().getXSSFColor() : null);

		cellDetailsMap.put("fontColor", fontColorString);

		// Handle cell background color
		String backgroundColorString = getRGBString(
				cellStyle instanceof XSSFCellStyle ? ((XSSFCellStyle) cellStyle).getFillForegroundXSSFColor() : null);
		cellDetailsMap.put("bgColor", backgroundColorString);

		// Handle cell borders
		boolean hasBorders = hasCellBorders(cellStyle);
		cellDetailsMap.put("hasBorders", "" + hasBorders + "");
		if (hasBorders) {
			cellDetailsMap.put("borderDetails", getBorderDetails(cellStyle, hasBorders));
		}

		// Handle cell formula
		if (cell.getCellType() == CellType.FORMULA) {
			cellDetailsMap.put("hasFormula", "true");
			cellDetailsMap.put("formula", cell.getCellFormula());
			cellDetailsMap.put("value", cellValue);
		} else {
			cellDetailsMap.put("hasFormula", "false");
		}

		// Handle cell validations
		DataValidation dataValidation = findDataValidationForCell(sheet, cell);
		boolean hasValidationData = (dataValidation != null);
		String dataValidationConstraintToBase64 = "";
		if (hasValidationData) {
			DataValidationConstraint validationConstraint = dataValidation.getValidationConstraint();
			if (validationConstraint != null) {
				dataValidationConstraintToBase64 = ExcelConversion
						.dataValidationConstraintToBase64(dataValidation.getValidationConstraint());
			}

		}
		Map<String, Object> extraFormula = new HashMap<>();
		if (dataValidation != null && dataValidation.getValidationConstraint()
				.getValidationType() == DataValidationConstraint.ValidationType.LIST) {
			cellDetailsMap.put("hasExtraFormula", "true");
			extraFormula.put("hasOptions", "true");
			extraFormula.put("optionList", dataValidation.getValidationConstraint().getExplicitListValues());

		}

		if (dateValueOfCell != null) {
			cellDetailsMap.put("value", dateValueOfCell);
			cellDetailsMap.put("hasExtraFormula", "true");
			extraFormula.put("type", "DATE");
			extraFormula.put("dateFormat", DateFormat.DD_MM_YYYY.getKey());
		}
		cellDetailsMap.put("extraFormula", extraFormula);

		if (hasValidationData && !dataValidationConstraintToBase64.equals("")) {
			cellDetailsMap.put("hasValidation", "" + hasValidationData + "");
			cellDetailsMap.put("validation", dataValidationConstraintToBase64);
		} else {
			cellDetailsMap.put("hasValidation", "" + hasValidationData + "");
		}

		// Handle merge
		getMergedCellDetails(sheet, rowNumber, cellIndex, cellDetailsMap, noOfColumnHidden, noOfRowHidden);

		// Handle other cell details
		cellDetailsMap.put("rowIndex", cell.getRowIndex());
		cellDetailsMap.put("index", cell.getColumnIndex());
		cellDetailsMap.put("cellType", cell.getCellType());
		return cellDetailsMap;
	}

}
