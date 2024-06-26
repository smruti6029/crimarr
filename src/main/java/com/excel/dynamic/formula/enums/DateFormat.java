package com.excel.dynamic.formula.enums;

public enum DateFormat {
	DD_MM_YYYY("DD/MM/YYYY", "dd/MM/yyyy"), DD_MM_YYYY_HYPHEN("DD-MM-YYYY", "dd-MM-yyyy"),
	MM_DD_YYYY("MM/DD/YYYY", "MM/dd/yyyy"), MM_DD_YYYY_HYPHEN("MM-DD-YYYY", "MM-dd-yyyy"),
	YYYY_MM_DD("YYYY/MM/DD", "yyyy/MM/dd"), YYYY_MM_DD_HYPHEN("YYYY-MM-DD", "yyyy-MM-dd");

	private final String key;

	private final String format;

	DateFormat(String key, String format) {
		this.key = key;
		this.format = format;
	}

	public String getKey() {
		return key;
	}

	public String getFormat() {
		return format;
	}

}
