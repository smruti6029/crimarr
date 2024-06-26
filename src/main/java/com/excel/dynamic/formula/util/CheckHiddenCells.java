package com.excel.dynamic.formula.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

public class CheckHiddenCells {

	public static int checkHiddenColumns(List<String> cellNames, Sheet sheet) {
		List<String> cellNameList = new ArrayList<>();
		int noOfHiddenColumn = 0;
		for (String cellName : cellNames) {
			String processCellName = cellName.replaceAll("[^A-Za-z]", "");
			int columnIndex = CellReference.convertColStringToIndex(processCellName);
			if (sheet.isColumnHidden(columnIndex)) {
				if (!cellNameList.contains(processCellName)) {
					cellNameList.add(processCellName);
					noOfHiddenColumn++;
				}
//				cellNameList.add(cellName);
			}
		}

		return noOfHiddenColumn;
	}

	public static int checkHiddenRows(List<String> cellNames, Sheet sheet) {
		List<Integer> cellNameList = new ArrayList<>();
		int noOfHiddenRow = 0;
		for (String cellName : cellNames) {
			int rowIndex = Integer.parseInt(cellName.replaceAll("[A-Za-z]", "")) - 1;
			Integer processCellNo = rowIndex;
			if (sheet.getRow(rowIndex).getZeroHeight()) {
				if (!cellNameList.contains(processCellNo)) {
					cellNameList.add(processCellNo);
					noOfHiddenRow++;
				}
//				cellNameList.add(cellName);
			}
		}

		return noOfHiddenRow;
	}

}
