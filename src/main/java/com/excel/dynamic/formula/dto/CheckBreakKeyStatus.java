package com.excel.dynamic.formula.dto;

public class CheckBreakKeyStatus {
	private String breakKey;
	private boolean status;
	private String columnName;
	private String startIngCell;
	private String endingIngCell;
	private int startingRowNumber;
	private int endingRowNumber;
	private int newRowAdded;
	
	
	private int rowNumber;

	public String getBreakKey() {
		return breakKey;
	}

	public void setBreakKey(String breakKey) {
		this.breakKey = breakKey;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}


	public String getStartIngCell() {
		return startIngCell;
	}

	public void setStartIngCell(String startIngCell) {
		this.startIngCell = startIngCell;
	}

	public String getEndingIngCell() {
		return endingIngCell;
	}

	public void setEndingIngCell(String endingIngCell) {
		this.endingIngCell = endingIngCell;
	}

	public int getNewRowAdded() {
		return newRowAdded;
	}

	public void setNewRowAdded(int newRowAdded) {
		this.newRowAdded = newRowAdded;
	}

	public CheckBreakKeyStatus() {
		super();
	}

	public int getStartingRowNumber() {
		return startingRowNumber;
	}

	public void setStartingRowNumber(int startingRowNumber) {
		this.startingRowNumber = startingRowNumber;
	}

	public int getEndingRowNumber() {
		return endingRowNumber;
	}

	public void setEndingRowNumber(int endingRowNumber) {
		this.endingRowNumber = endingRowNumber;
	}
	

	
}
