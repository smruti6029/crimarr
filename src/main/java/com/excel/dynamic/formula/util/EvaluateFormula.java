package com.excel.dynamic.formula.util;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import com.excel.dynamic.formula.dto.Response;

public class EvaluateFormula {

	public Response<?> evaluateCellFormula(JSONArray requestJsonArray) {
		try {
			generateExcel(requestJsonArray);
			return new Response<>(HttpStatus.OK.value(), "success", null);
		} catch (Exception e) {
			e.printStackTrace();
			return new Response<>(HttpStatus.BAD_REQUEST.value(), "Error", null);
		}
	}

	private void generateExcel(JSONArray jsonArray) {
		try {
			Workbook workbook = new XSSFWorkbook();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject sheetData = jsonArray.getJSONObject(i);
				String sheetName = sheetData.getString("sheetName");
				Sheet sheet = workbook.createSheet(sheetName);

				JSONArray sheetRows = sheetData.getJSONArray("sheetData");
				for (int j = 0; j < sheetRows.length(); j++) {
					JSONObject rowData = sheetRows.getJSONObject(j);
					JSONArray cells = rowData.getJSONArray("rowData");

					Row row = sheet.createRow(j);
					for (int k = 0; k < cells.length(); k++) {
						JSONObject cellData = cells.getJSONObject(k);
						String cellName = cellData.getString("cellName");
						String headerName = cellData.getString("headerName");
						Cell cell = row.createCell(k);
						if (!(cellData.get("cellDetails") instanceof String)) {
							JSONObject cellDetails = cellData.getJSONObject("cellDetails");

							if (cellDetails.has("hasFormula") && cellDetails.getBoolean("hasFormula")) {
								String formula = cellDetails.getString("formula");
								if (!formula.equals("")) {
									cell.setCellFormula(formula);
									setCellProperties(cell, cellDetails);
								}
							} else {
								cell.setCellValue(headerName);
								setCellProperties(cell, cellDetails);
							}

						} else {
							String cellDetails = cellData.getString("cellDetails");
							cell.setCellValue(headerName);
//							setCellProperties(cell, null);
						}

					}
				}
			}

			// Evaluate formulas
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateAll();

			String filePath = "/home/rapidosft/Github/CRIMARR/src/main/java/excel-data/GeneratedExcel.xlsx";
			try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
				workbook.write(outputStream);
				workbook.close();
				// 
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setCellProperties(Cell cell, JSONObject cellDetails) {
		CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
		if (cellDetails.has("textAlignment")) {
			String textAlignment = cellDetails.getString("textAlignment");
			if ("CENTER".equals(textAlignment)) {
				cellStyle.setAlignment(HorizontalAlignment.CENTER);
			} else if ("RIGHT".equals(textAlignment)) {
				cellStyle.setAlignment(HorizontalAlignment.RIGHT);
			} else if ("LEFT".equals(textAlignment)) {
				cellStyle.setAlignment(HorizontalAlignment.LEFT);
			}
		}
		if (cellDetails.has("verticalAlignment")) {
			String verticalAlignment = cellDetails.getString("verticalAlignment");
			if ("CENTER".equals(verticalAlignment)) {
				cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			} else if ("TOP".equals(verticalAlignment)) {
				cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
			} else if ("BOTTOM".equals(verticalAlignment)) {
				cellStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);
			}
		}
		cell.setCellStyle(cellStyle);
	}

//	public static List<String> extractCellNames(String formula) {
//        List<String> cellNames = new ArrayList<>();
//        
//        Pattern pattern = Pattern.compile("[A-Z]+\\d+(:[A-Z]+\\d+)?");
//        Matcher matcher = pattern.matcher(formula);
//
//     
//        while (matcher.find()) {
//            String cellRef = matcher.group();
//            if (cellRef.contains(":")) {
//               
//                String[] range = cellRef.split(":");
//                String start = range[0];
//                String end = range[1];
//                cellNames.addAll(expandRange(start, end));
//            } else {
//               
//                cellNames.add(cellRef);
//            }
//        }
//
//        return cellNames;
//    }
//
//    private static List<String> expandRange(String start, String end) {
//        List<String> cellNames = new ArrayList<>();
//     
//        String startCol = start.replaceAll("[0-9]", "");
//        int startRow = Integer.parseInt(start.replaceAll("[A-Z]", ""));
//      
//        String endCol = end.replaceAll("[0-9]", "");
//        int endRow = Integer.parseInt(end.replaceAll("[A-Z]", ""));
//
//        
//        for (int row = startRow; row <= endRow; row++) {
//            for (char col = startCol.charAt(0); col <= endCol.charAt(0); col++) {
//                cellNames.add("" + col + row);
//            }
//        }
//        return cellNames;
//    }

	public static List<String> extractCellNames(String formula) {
		List<String> cellNames = new ArrayList<>();

		Pattern pattern = Pattern.compile("([$]?[A-Za-z0-9_]+!)?[$]?[A-Za-z]+[$]?[0-9]+(:[$]?[A-Za-z]+[$]?[0-9]+)?");

		Matcher matcher = pattern.matcher(formula);

		while (matcher.find()) {
			String cellRef = matcher.group();
			if (cellRef.contains(":")) {

				String[] range = cellRef.split(":");
				String start = range[0];
				String end = range[1];
				cellNames.addAll(expandRange(start, end));
			} else {

				cellNames.add(cellRef);
			}
		}

		return cellNames;
	}

//	private static List<String> expandRange(String start, String end) {
//		List<String> cellNames = new ArrayList<>();
//
//		String startCol = start.replaceAll("[0-9]", "");
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

	public static Boolean checkFormulaForValueError(String formula) {

		Pattern pattern = Pattern.compile("[^=+]+");

		Matcher matcher = pattern.matcher(formula);

		while (matcher.find()) {

			String cell = matcher.group();
			try {
				Long value = Long.parseLong(cell);
				return false;
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return false;

	}

	public static List<String> expandRange(String start, String end) {
		List<String> cellNames = new ArrayList<>();

		// Parse start and end coordinates
		CellCoordinates startCoord = parseCellCoordinates(start);
		CellCoordinates endCoord = parseCellCoordinates(end);

		// Generate cell names
		for (int row = startCoord.getRow(); row <= endCoord.getRow(); row++) {
			for (int col = startCoord.getColumn(); col <= endCoord.getColumn(); col++) {
				cellNames.add(getCellName(col, row));
			}
		}

		return cellNames;
	}

	private static CellCoordinates parseCellCoordinates(String cell) {
		StringBuilder columnBuilder = new StringBuilder();
		StringBuilder rowBuilder = new StringBuilder();

		int i = 0;
		while (i < cell.length() && Character.isLetter(cell.charAt(i))) {
			columnBuilder.append(cell.charAt(i));
			i++;
		}

		while (i < cell.length() && Character.isDigit(cell.charAt(i))) {
			rowBuilder.append(cell.charAt(i));
			i++;
		}

		return new CellCoordinates(columnToIndex(columnBuilder.toString()), Integer.parseInt(rowBuilder.toString()));
	}

	private static String getCellName(int column, int row) {
		return indexToColumn(column) + row;
	}

	private static int columnToIndex(String column) {
		int index = 0;
		for (int i = 0; i < column.length(); i++) {
			index = index * 26 + (column.charAt(i) - 'A' + 1);
		}
		return index - 1;
	}

	private static String indexToColumn(int index) {
		StringBuilder columnName = new StringBuilder();
		while (index >= 0) {
			columnName.insert(0, (char) ('A' + index % 26));
			index = (index / 26) - 1;
		}
		return columnName.toString();
	}

	private static class CellCoordinates {
		private final int column;
		private final int row;

		public CellCoordinates(int column, int row) {
			this.column = column;
			this.row = row;
		}

		public int getColumn() {
			return column;
		}

		public int getRow() {
			return row;
		}
	}

}
