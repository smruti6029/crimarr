package com.excel.dynamic.formula.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.poi.ss.util.CellReference;

import com.excel.dynamic.formula.enums.CustomCellType;
import com.excel.dynamic.formula.enums.DateFormat;
import com.excel.dynamic.formula.enums.Operator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class IfConditionChecking {

	static Set<String> allOperators = new HashSet<>(
			Arrays.asList(">", "<", "==", "=", "!", "!=", ">=", "<=", "&&", "||", "+", "-", "*", "/", "%", "(", ")"));
	static Set<String> conditionalOperators = new HashSet<>(Arrays.asList(">", "<", "==", "=", "!=", ">=", "<="));
	static Set<String> logicalOperators = new HashSet<>(Arrays.asList("&&", "||"));

	public static Boolean checkIfcondition(JsonArray asJsonArray, Map<String, Object> subObjectMap, String sheetNmae) {

		int checkEmptyCount = 0;

		for (JsonElement element : asJsonArray) {

//			String condition = element.getAsString();
//			String regex = "\\b[A-Z][a-zA-Z0-9_]*!\\$?[A-Z]+\\$?[0-9]+\\b|\\b[A-Z]+\\$?[0-9]+\\b|[-+*/>=<!()&|]|\\b\\d+\\b";
//			Pattern pattern = Pattern.compile(regex);
//			Matcher matcher = pattern.matcher(condition);
//			List<String> Allcomponents = new ArrayList<>();
//			while (matcher.find()) {
//				Allcomponents.add(matcher.group());
//			}
//			String regix2 = "\\b[A-Z][a-zA-Z0-9_]*!\\$?[A-Z]+\\$?[0-9]+\\b|\\b[A-Z]+\\$?[0-9]+\\b|\\b\\d+\\b";
//			Pattern pattern2 = Pattern.compile(regix2);
//			Matcher matcher2 = pattern2.matcher(condition);
//			List<String> allOperand = new ArrayList<>();
//
//			// Extract all components
//			while (matcher2.find()) {
//				allOperand.add(matcher2.group());
//			}
			List<String> allComponents = new ArrayList<>();

			List<String> allOperand = new ArrayList<>();
			if (element.isJsonNull() || element == null || element.getAsJsonArray().isEmpty()
					|| element.getAsJsonArray().size() == 0) {
				checkEmptyCount++;
				continue;
			}
			JsonArray asJsonArray2 = element.getAsJsonArray();

			for (JsonElement jsonElement : asJsonArray2) {

				if (jsonElement.getAsString().trim().equals("")) {
					allComponents.add("");
				} else {
					allComponents.add(jsonElement.getAsString());
				}
			}

			// convert allComponents list to array
			String[] componetArray = new String[allComponents.size()];

			for (int i = 0; i < allComponents.size(); i++) {
				componetArray[i] = allComponents.get(i);
			}
			// Convert the cellname to conditions
			allComponents = replaceAndPrint(componetArray);

			for (String operands : allComponents) {
				if (!Operator.isOperator(operands)) {
					allOperand.add(operands);
				}
			}

			Map<String, String> operandMap = new HashMap<>();
			for (String operand : allOperand) {

				String valueAsString = null;
				Boolean flag = false;
				if (operand.contains(":")) {
					String cellPattern = "[A-Z]+[0-9]+";
					Pattern pattern = Pattern.compile(cellPattern);
					Matcher matcher = pattern.matcher(operand);

					while (matcher.find()) {
						String cell = matcher.group();
						if (!isValidCellName(cell)) {
							flag = true;
						}
					}

				}
				if (isValidCellName(operand) || !flag && operand.contains(":")) {

					String operandPattern = "^[A-Z]+[0-9]+(:[A-Z]+[0-9]+)?$";
					if (operand.matches(operandPattern)) {
						String x = sheetNmae + "!" + operand;
						if (subObjectMap.containsKey(x)) {
							Object value = subObjectMap.get(x);
							JsonObject jsonObject = JsonParser.parseString(new Gson().toJson(subObjectMap.get(x)))
									.getAsJsonObject();
							if (jsonObject.has("cellDetails")) {
								JsonObject asJsonObject = jsonObject.get("cellDetails").getAsJsonObject();
								if (jsonObject.has("headerName")) {
									valueAsString = jsonObject.get("headerName").getAsString();
									if (asJsonObject.has("value")
											&& !asJsonObject.get("value").getAsString().isEmpty()) {
										valueAsString = asJsonObject.get("value").getAsString();
									}

								}

							}
							if (valueAsString.isEmpty()) {
								valueAsString = "";
							}
							operandMap.put(operand, valueAsString);
						}

					}

					else if (subObjectMap.containsKey(operand)) {
						Object value = subObjectMap.get(operand);
						JsonObject jsonObject = JsonParser.parseString(new Gson().toJson(subObjectMap.get(operand)))
								.getAsJsonObject();
						if (jsonObject.has("cellDetails")) {
							JsonObject asJsonObject = jsonObject.get("cellDetails").getAsJsonObject();

							if (jsonObject.has("headerName")) {
								valueAsString = jsonObject.get("headerName").getAsString();
								if (asJsonObject.has("value") && !asJsonObject.get("value").getAsString().isEmpty()) {
									valueAsString = asJsonObject.get("value").getAsString();
								}

							}
						}
						if (valueAsString.isEmpty()) {
							valueAsString = "";
						}
						operandMap.put(operand, valueAsString);
					}

				}
			}

			List<String> allcompontes2 = new ArrayList<>();

			for (int i = 0; i < allComponents.size(); i++) {
				String operand = allComponents.get(i);
				if (operand.equals("=")) {
					if (i > 0) {
						String previousComponent = allComponents.get(i - 1);
						String nextCompont = allComponents.get(i + 1);
						if (!isOperator(previousComponent) && !nextCompont.equals("=")) {
							allcompontes2.add("=");
						}
					}
				}
				if (operandMap.containsKey(operand)) {
					String value = operandMap.get(operand);
					allcompontes2.add(String.valueOf(value));
				} else {
					allcompontes2.add(operand);
				}
			}
			StringBuilder concatenatedString = new StringBuilder();
			for (String component : allcompontes2) {
				component = isNumber(component) ? component
						: allOperators.contains(component) ? component : "'" + component + "'";
				concatenatedString.append(component);
			}
			
			if (evaluateCondition(concatenatedString.toString())) {
				return true;
			}

		}

		if (asJsonArray.size() == checkEmptyCount) {
			return true;
		}

		return false;
	}

	public static Boolean checkIfcondition(JsonArray asJsonArray, Map<String, Object> subObjectMap, String sheetNmae,
			Map<String, Object> parentObjectMap) {

		int checkEmptyCount = 0;

		for (JsonElement element : asJsonArray) {

			String currentCellFormatter = null;
			Boolean isCurrentCellIsDateType = false;
			int vallidDateFormat = 0;
			int countEmptyOperand = 0;

//			String condition = element.getAsString();
//			String regex = "\\b[A-Z][a-zA-Z0-9_]*!\\$?[A-Z]+\\$?[0-9]+\\b|\\b[A-Z]+\\$?[0-9]+\\b|[-+*/>=<!()&|]|\\b\\d+\\b";
//			Pattern pattern = Pattern.compile(regex);
//			Matcher matcher = pattern.matcher(condition);
//			List<String> Allcomponents = new ArrayList<>();
//			while (matcher.find()) {
//				Allcomponents.add(matcher.group());
//			}
//			String regix2 = "\\b[A-Z][a-zA-Z0-9_]*!\\$?[A-Z]+\\$?[0-9]+\\b|\\b[A-Z]+\\$?[0-9]+\\b|\\b\\d+\\b";
//			Pattern pattern2 = Pattern.compile(regix2);
//			Matcher matcher2 = pattern2.matcher(condition);
//			List<String> allOperand = new ArrayList<>();
//
//			// Extract all components
//			while (matcher2.find()) {
//				allOperand.add(matcher2.group());
//			}
			List<String> allComponents = new ArrayList<>();

			List<String> allOperand = new ArrayList<>();
			if (element.isJsonNull() || element == null || element.getAsJsonArray().isEmpty()
					|| element.getAsJsonArray().size() == 0) {
				checkEmptyCount++;
				continue;
			}
			JsonArray asJsonArray2 = element.getAsJsonArray();

			for (JsonElement jsonElement : asJsonArray2) {

				if (jsonElement.getAsString().trim().equals("")) {
					allComponents.add("");
				} else {
					allComponents.add(jsonElement.getAsString());
				}
			}

			// convert allComponents list to array
			String[] componetArray = new String[allComponents.size()];

			for (int i = 0; i < allComponents.size(); i++) {
				componetArray[i] = allComponents.get(i);
			}
			// Convert the cellname to conditions
			allComponents = replaceAndPrint(componetArray);

			for (String operands : allComponents) {
				if (!Operator.isOperator(operands)) {
					allOperand.add(operands);
				}
			}

			Map<String, String> operandMap = new HashMap<>();
			for (String operand : allOperand) {

				String valueAsString = null;
				Boolean flag = false;
				if (operand.contains(":")) {
					String cellPattern = "[A-Z]+[0-9]+";
					Pattern pattern = Pattern.compile(cellPattern);
					Matcher matcher = pattern.matcher(operand);

					while (matcher.find()) {
						String cell = matcher.group();
						if (!isValidCellName(cell)) {
							flag = true;
						}
					}

				}
				if (isValidCellName(operand) || !flag && operand.contains(":")) {

					String operandPattern = "^[A-Z]+[0-9]+(:[A-Z]+[0-9]+)?$";
					if (operand.matches(operandPattern)) {
						String x = sheetNmae + "!" + operand;
						if (subObjectMap.containsKey(x)) {
							Object value = subObjectMap.get(x);
							JsonObject jsonObject = JsonParser.parseString(new Gson().toJson(subObjectMap.get(x)))
									.getAsJsonObject();
							if (jsonObject.has("cellDetails")) {
								JsonObject asJsonObject = jsonObject.get("cellDetails").getAsJsonObject();
								if (jsonObject.has("headerName")) {
									valueAsString = jsonObject.get("headerName").getAsString();
									if (asJsonObject.has("value")
											&& !asJsonObject.get("value").getAsString().isEmpty()) {
										valueAsString = asJsonObject.get("value").getAsString();
									}

								}

							}

							JsonObject parentJsonObject = JsonParser
									.parseString(new Gson().toJson(parentObjectMap.get(sheetNmae + operand)))
									.getAsJsonObject();
							if (parentJsonObject.has("cellDetails")) {
								JsonObject asJsonObject = parentJsonObject.get("cellDetails").getAsJsonObject();
								if (asJsonObject.has("extraFormula")) {
									JsonObject parentextraFormula = asJsonObject.get("extraFormula").getAsJsonObject();
									if (parentextraFormula.has("type")
											&& !parentextraFormula.get("type").getAsString().isEmpty()
											&& !parentextraFormula.get("type").getAsString().equals("")) {
										String asString = parentextraFormula.get("type").getAsString();
										if (CustomCellType.DATE.name().equalsIgnoreCase(asString)) {
											if (parentextraFormula.has("dateFormat")) {
												String dateFormater = parentextraFormula.get("dateFormat")
														.getAsString();
												String dateParttan = dateFormater;
												DateFormat dateFormatEnum = Arrays.stream(DateFormat.values())
														.filter(format -> format.getKey().equals(dateParttan))
														.findFirst().orElse(null);
												dateFormater = dateFormatEnum != null ? dateFormatEnum.getFormat() : "";
												String convertDateAsParentPattern = dateFormatEnum != null
														? ConversionUtility.convertDateFormatAsPattern(valueAsString,
																DateFormat.DD_MM_YYYY.getFormat(),
																dateFormatEnum.getFormat())
														: null;

												if (convertDateAsParentPattern != null) {
													valueAsString = convertDateAsParentPattern;
												}
												// set the current cell date formater for further use
												if (!isCurrentCellIsDateType) {
													isCurrentCellIsDateType = true;
													currentCellFormatter = dateFormater;
												}
												vallidDateFormat++;
												long convertValue = (convertDateToMilliseconds(valueAsString,
														dateFormater));

												if (isCurrentCellIsDateType && convertValue == -1) {
//													return false;
													break;
												}
												valueAsString = convertValue != -1 ? Long.toString(convertValue)
														: valueAsString;
											}
										} else if (isCurrentCellIsDateType) {
//											return false;
											break;
										}

									} else if (isCurrentCellIsDateType) {
										break;
									}

								}

							}

							if (valueAsString.isEmpty()) {
								valueAsString = "";
							}
							operandMap.put(operand, valueAsString);
						}

					}

					else if (subObjectMap.containsKey(operand)) {
						Object value = subObjectMap.get(operand);
						JsonObject jsonObject = JsonParser.parseString(new Gson().toJson(subObjectMap.get(operand)))
								.getAsJsonObject();
						if (jsonObject.has("cellDetails")) {
							JsonObject asJsonObject = jsonObject.get("cellDetails").getAsJsonObject();

							if (jsonObject.has("headerName")) {
								valueAsString = jsonObject.get("headerName").getAsString();
								if (asJsonObject.has("value") && !asJsonObject.get("value").getAsString().isEmpty()) {
									valueAsString = asJsonObject.get("value").getAsString();
								}

							}
						}
						JsonObject parentJsonObject = JsonParser
								.parseString(new Gson().toJson(parentObjectMap.get(sheetNmae + operand.split("!")[1])))
								.getAsJsonObject();
						if (parentJsonObject.has("cellDetails")) {
							JsonObject asJsonObject = parentJsonObject.get("cellDetails").getAsJsonObject();
							if (asJsonObject.has("extraFormula")) {
								JsonObject parentextraFormula = asJsonObject.get("extraFormula").getAsJsonObject();
								if (parentextraFormula.has("type")
										&& !parentextraFormula.get("type").getAsString().isEmpty()
										&& !parentextraFormula.get("type").getAsString().equals("")) {
									String asString = parentextraFormula.get("type").getAsString();
									if (CustomCellType.DATE.name().equalsIgnoreCase(asString)) {
										if (parentextraFormula.has("dateFormat")) {
											String dateFormater = parentextraFormula.get("dateFormat").getAsString();
											String dateParttan = dateFormater;
											DateFormat dateFormatEnum = Arrays.stream(DateFormat.values())
													.filter(format -> format.getKey().equals(dateParttan)).findFirst()
													.orElse(null);
											dateFormater = dateFormatEnum != null ? dateFormatEnum.getFormat() : "";
											String convertDateAsParentPattern = dateFormatEnum != null
													? ConversionUtility.convertDateFormatAsPattern(valueAsString,
															DateFormat.DD_MM_YYYY.getFormat(),
															dateFormatEnum.getFormat())
													: null;

											if (convertDateAsParentPattern != null) {
												valueAsString = convertDateAsParentPattern;
											}

											if (!isCurrentCellIsDateType) {
												isCurrentCellIsDateType = true;
												currentCellFormatter = dateFormater;
											}
											vallidDateFormat++;

											Long convertValue = (convertDateToMilliseconds(valueAsString,
													dateFormater));
											if (isCurrentCellIsDateType && convertValue == -1) {
												break;
											}
											valueAsString = convertValue != -1 ? Long.toString(convertValue)
													: valueAsString;
										}

									} else if (isCurrentCellIsDateType) {
										break;
									}

								} else if (isCurrentCellIsDateType) {
									break;
								}

							}

						}
						if (valueAsString.isEmpty()) {
							valueAsString = "";
						}
						operandMap.put(operand, valueAsString);
					}

				} else {
					if (operand.equals("")) {
						countEmptyOperand++;

					}
					if (!operand.trim().equals("") && currentCellFormatter != null) {

						Long convertValue = (convertDateToMilliseconds(operand, currentCellFormatter));
						if (convertValue == -1 && !isCurrentCellIsDateType) {
							vallidDateFormat++;
							break;
						} else if (isCurrentCellIsDateType && convertValue == -1) {
							break;
						} else if (convertValue != -1) {
							operandMap.put(operand, Long.toString(convertValue));
							vallidDateFormat++;
						}
					} else {
						vallidDateFormat++;
					}

				}
			}

			if (isCurrentCellIsDateType) {
				vallidDateFormat += countEmptyOperand;
				if (vallidDateFormat != allOperand.size()) {
					continue;
				}

			}

			List<String> allcompontes2 = new ArrayList<>();

			for (int i = 0; i < allComponents.size(); i++) {
				String operand = allComponents.get(i);
				if (operand.equals("=")) {
					if (i > 0) {
						String previousComponent = allComponents.get(i - 1);
						String nextCompont = allComponents.get(i + 1);
						if (!isOperator(previousComponent) && !nextCompont.equals("=")) {
							allcompontes2.add("=");
						}
					}
				}
				if (operandMap.containsKey(operand)) {
					String value = operandMap.get(operand);
					allcompontes2.add(String.valueOf(value));
				} else {
					allcompontes2.add(operand);
				}
			}
			StringBuilder concatenatedString = new StringBuilder();
			for (String component : allcompontes2) {
				component = isNumber(component) ? component
						: allOperators.contains(component) ? component : "'" + component + "'";
				concatenatedString.append(component);
			}
			
			if (evaluateCondition(concatenatedString.toString())) {
				return true;
			}

		}

		if (asJsonArray.size() == checkEmptyCount) {
			return true;
		}

		return false;
	}

	public static boolean isValidCellName(String cellName) {
		try {
			if (cellName.equals("!")) {
				return false;
			}
			try {
				long longValue = Long.parseLong(cellName);
				return false;
			} catch (Exception e) {

			}
			String cellPattern = "[A-Z]+[0-9]+";
			Pattern pattern = Pattern.compile(cellPattern);
			Matcher matcher = pattern.matcher(cellName);
			if (matcher.matches()) {
				CellReference cellReference = new CellReference(cellName);
				return true;
			}
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static boolean evaluateCondition(String condition) {

		

		try {
			JexlEngine jexl = new JexlBuilder().create();
			JexlExpression expr = jexl.createExpression(condition);
			JexlContext context = new MapContext();
			Object evalResult = expr.evaluate(context);
			if (evalResult instanceof Boolean) {
				return (Boolean) evalResult;
			} else {
				return false;
			}
		} catch (JexlException e) {
//			e.printStackTrace();
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isOperator(String str) {
		// Define your set of operators
		String[] operators = { "+", "-", "*", "/", "=", "<", ">", "<=", ">=", "!=", "!" };

		// Check if the given string is in the set of operators
		for (String operator : operators) {
			if (operator.equals(str)) {
				return true;
			}
		}
		return false;
	}

	private static void replaceCellValue(List<String> allComponents) {

		for (String componet : allComponents) {

			if (componet.equals("!")) {

			}

		}

	}

	private static void replaceDynamicSequence(List<String> allComponents) {
		for (int i = 0; i < allComponents.size() - 2; i++) {
			if (allComponents.get(i).equals("!")) {
				if (allComponents.get(i + 1).equals("&&") || allComponents.get(i + 1).equals("||")) {
					// Get the replacement components dynamically
					List<String> replacement = getReplacement(allComponents.subList(i, i + 4));
					allComponents.subList(i, i + replacement.size()).clear();
					allComponents.addAll(i, replacement);
				}
			}
		}
	}

	private static List<String> getReplacement(List<String> sequence) {
		List<String> replacement = new ArrayList<>();
//
//		for (String seq : sequence) {
//
//			if (isValidCellName(seq)) {
//				int currentIndex = seq.indexOf(seq);
//				int firstIndex = currentIndex;
//				int lastIndex = currentIndex;
//				while (true) {
//					if (firstIndex != 0) {
//						if (sequence.get(firstIndex).equals("(")) {
//							firstIndex = firstIndex - 1;
//						} else if (sequence.get(firstIndex).equals("!")) {
//							
//						}
//					} else {
//						break;
//					}
//				}
//			}
//		}

		for (String seq : sequence) {
			if (isValidCellName(seq)) {
				int currentIndex = sequence.indexOf(seq);
				if (currentIndex == 0 && sequence.size() == 1) {
					replacement.add(seq); // "="
					replacement.add("!");
					replacement.add("="); // "A1"
					replacement.add("");
				} else if (currentIndex == 1 && sequence.size() == 2 && sequence.get(currentIndex - 1).equals("!")) {
					replacement.add(seq); // "="
					replacement.add("="); // "A1"
					replacement.add("");
				}
			}
		}
		return replacement;
	}

	private static Boolean getIndex(int currentIndex, List<String> sequence) {
		int previousIndex = currentIndex - 1;

		if (sequence.get(previousIndex).equals("(")) {
			currentIndex -= 1;
			return getIndex(currentIndex, sequence);
		} else if (sequence.get(previousIndex).equals("!")) {
			return true;
		}
		return true;
	}

	// Replace the single cellname with the condition
	private static List<String> replaceAndPrint(String[] array) {
		List<String> replacedArray = replaceCellName(array, 0, false);
//		
//		for (int i = 0; i < replacedArray.size(); i++) {
//			
//			if (i < replacedArray.size() - 1) {
//				
//			}
//		}
//		
		return replacedArray;
	}

	private static List<String> replaceCellName(String[] array, int startIndex, boolean negateOutsideParentheses) {
		List<String> replacedArray = new ArrayList<>();
		boolean negate = false;
		for (int i = startIndex; i < array.length; i++) {
			
			if (i > 0 && array[i].equals("!") && array[i - 1].matches("^[A-Z]+[0-9]+(:[A-Z]+[0-9]+)?$")) {
				replacedArray.add("!");
			}
			if (array[i].equals("(")) {
				if (i != 0 && array[i].equals("(") && array[i - 1].equals("!")) {
					replacedArray.add("!");
				}
				int closingParenIndex = findClosingParenIndex(array, i + 1);
				if (closingParenIndex != -1) {
					List<String> innerReplacedArray = replaceCellName(array, i + 1, negate);
					replacedArray.add("(");
					replacedArray.addAll(innerReplacedArray);
					replacedArray.add(")");
					i = closingParenIndex;
				}
			} else if (array[i].equals("!") && !negateOutsideParentheses
					&& !isInsideParentheses(array, i, startIndex)) {
				negate = true;
			} else if (array[i].equals(")")) {
				
				break; // Ignore closing parentheses
			} else if (array[i].matches("^[A-Z]+[0-9]+(:[A-Z]+[0-9]+)?$")) {
				if (!negate && !findConditionalOperatorOrLogicalOperator(array, i)) {
					replacedArray.add(array[i]);
					replacedArray.add("!");
				} else {
					replacedArray.add(array[i]);
				}
				if (!findConditionalOperatorOrLogicalOperator(array, i)) {
					replacedArray.add("=");
					replacedArray.add("");
				}
				negate = false;
			} else {
				replacedArray.add(array[i]);
			}
		}
		return replacedArray;
	}

	private static boolean isInsideParentheses(String[] array, int currentIndex, int startIndex) {
		int openParenCount = 0;
		for (int i = startIndex; i < currentIndex; i++) {
			if (array[i].equals("(")) {
				openParenCount++;
			} else if (array[i].equals(")")) {
				openParenCount--;
			}
		}
		return openParenCount > 0;
	}

	private static int findClosingParenIndex(String[] array, int startIndex) {
		int openParenCount = 1;
		for (int i = startIndex; i < array.length; i++) {
			if (array[i].equals("(")) {
				openParenCount++;
			} else if (array[i].equals(")")) {
				openParenCount--;
				if (openParenCount == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	private static boolean findConditionalOperatorOrLogicalOperator(String[] array, int startIndex) {
		int arrayLength = array.length;
//		boolean forward = arrayLength == 3 ? startIndex < arrayLength - 2 : startIndex < arrayLength - 3;
		boolean forward = startIndex < arrayLength - startIndex;
		boolean checkResponse = false;
		// Check forward
		for (int i = startIndex; i < arrayLength; i++) {
			if (conditionalOperators.contains(array[i])) {
				checkResponse = true;
				break; // Exit the loop if condition met
			} else if (logicalOperators.contains(array[i])) {
				break;
			}
		}

		// Check backward if forward didn't meet the condition
		if (!checkResponse) {
			for (int i = startIndex - 1; i >= 0; i--) {
				if (conditionalOperators.contains(array[i])) {
					checkResponse = true;
					break; // Exit the loop if condition met
				} else if (logicalOperators.contains(array[i])) {
					break;
				}
			}
		}
		return checkResponse;
	}

	public static boolean isNumber(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e1) {
			try {
				Long.parseLong(str);
				return true;
			} catch (NumberFormatException e2) {
				try {
					Float.parseFloat(str);
					return true;
				} catch (NumberFormatException e3) {
					try {
						Double.parseDouble(str);
						return true;
					} catch (NumberFormatException e4) {
						return false;
					}
				}
			}
		}
	}

	public static long convertDateToMilliseconds(String dateString, String dateFormat) {
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);

		try {
			Date date = format.parse(dateString);

			return date.getTime();
		} catch (ParseException e) {
//			e.printStackTrace();
			return -1;
		}
	}

}
