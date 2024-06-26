package com.excel.dynamic.formula.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.util.CellReference;

import com.excel.dynamic.formula.enums.ExcelFormula;
import com.excel.dynamic.formula.enums.ParameterType;

public class ValueErrorChecking {
	public static Boolean vallidFormulaCheck(String formula) {
		List<String> components = new ArrayList<>();

		Pattern partten_2 = Pattern.compile("\\w+\\([^()]+\\)");
		if (partten_2.matcher(formula).matches()) {
			components.add(formula);

		} else {
			components.addAll(extractComponents(formula));
		}
		Pattern formulaPattern = Pattern.compile("\\w+\\([^()]+\\)");
		Pattern cellNamePattern = Pattern.compile("[A-Z]+\\d+");

//		

//		ExcelFormula[] allFormulas = ExcelFormula.values();

		int count = 0;

		for (String component : components) {
			if (formulaPattern.matcher(component).matches()) {
				for (ExcelFormula allFormulas : ExcelFormula.values()) {
					if (component.toLowerCase().contains(allFormulas.getFormulaName().toLowerCase())) {
						count++;

						int maxParameters = allFormulas.getMaxParameters() == -1 ? Integer.MAX_VALUE
								: allFormulas.getMaxParameters();
						int minParameters = allFormulas.getMinParameters();

						if (component.contains(":") && maxParameters == 2 && minParameters == 2) {
							
						} else if (component.contains(":")) {
							List<String> extractComponents = extractComponents(component);
							if (extractComponents.size() >= minParameters
									&& extractComponents.size() <= maxParameters) {
								for (String cell : extractComponents) {
									if (!isValidCellName(cell)) {
										return false;
									}

								}

							} else {

								
								return false;
							}

						} else {

							// Do it in crimmar extract cell names function
							List<String> extractComponents2 = extractComponents(component);
							if (extractComponents2.size() >= minParameters
									&& extractComponents2.size() <= maxParameters) {
								for (String cellnames : extractComponents2) {

									if (!isValidCellName(cellnames)) {
										ParameterType[] parameterTypes = allFormulas.getParameterTypes();
										String string = Arrays.toString(allFormulas.getParameterTypes());
										ParameterType parameterType = ParameterType
												.valueOf(string = string.replace("[", "").replace("]", ""));

										if (checkParameterType(cellnames, parameterType)) {
											return true;
										}
										return false;
									}

								}

							} else {
								
								return false;
							}

						}

//							

						
						
						
						
						
						System.err.println(allFormulas + "   " + component);
					}
				}
			}
		}

		if (count == 0) {
			return null;
		}
		return true;

	}

	public static boolean isValidCellName(String cellName) {
		try {

			try {
				long longValue = Long.parseLong(cellName);
				return true;
			} catch (Exception e) {

			}

			CellReference cellReference = new CellReference(cellName);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static int countParameters(String component) {
		// Counting parameters by counting commas and adding 1
		int count = 0;
		for (char c : component.toCharArray()) {
			if (c == ',') {
				count++;
			}
		}
		// Add 1 to count because there's always one more parameter than commas
		return count + 1;
	}

	public static List<String> extractComponents(String formula) {
		List<String> components = new ArrayList<>();

		int parenthesisCounter = 0;
		StringBuilder currentComponent = new StringBuilder();

		for (char c : formula.toCharArray()) {

//			
			if (c == '(') {
				parenthesisCounter++;
				if (parenthesisCounter > 1) {
					currentComponent.append(c);
				}
			} else if (c == ')') {
				parenthesisCounter--;
				if (parenthesisCounter == 0) {
					String component = currentComponent.toString().trim();
					components.add(component);
					currentComponent.setLength(0);
				} else {
					currentComponent.append(c);
				}
			} else if (c == ',' && parenthesisCounter == 1) {
				String component = currentComponent.toString().trim();
				components.add(component);
				currentComponent.setLength(0);
			} else {

				if (parenthesisCounter != 0) {
					currentComponent.append(c);
				}
			}
		}

		if (components.size() == 1 && components.get(0).contains(":")) {

			List<String> cellNames = new ArrayList<>();
			String[] rangeParts = components.get(0).split(":");
			String startCell = rangeParts[0];
			int startRow = Integer.parseInt(startCell.replaceAll("[A-Z]", ""));
			String endCell = rangeParts[1];
			int endRow = Integer.parseInt(endCell.replaceAll("[A-Z]", ""));

			for (int row = startRow; row <= endRow; row++) {
				for (char col = startCell.charAt(0); col <= endCell.charAt(0); col++) {
					cellNames.add("" + col + row);
				}
			}
			components = cellNames;

		} else if (components.size() > 1) {

			List<String> nestedComponents = new ArrayList<>();
			for (String x : components) {
				if (x.matches("\\w+\\([^(),]+(,[^(),]+)*\\)")) {
				} else {
					nestedComponents.addAll(extractComponents(x));
				}
			}
			components.addAll(nestedComponents);

		}

		return components;
	}

	private static boolean checkParameterType(String param, ParameterType expectedType) {
		switch (expectedType) {
		case NUMBER:
			return isNumeric(param);
		case STRING:
			return isString(param);
		case DATE:
			return isDate(param);
		case ALL:
			return true;
		default:
			return false;
		}
	}

	private static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isString(String str) {
		return str.startsWith("\"") && str.endsWith("\"");
	}

	private static boolean isDate(String str) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			sdf.setLenient(false);
			sdf.parse(str);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

}
