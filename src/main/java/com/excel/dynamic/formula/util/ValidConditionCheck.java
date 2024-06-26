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

public class ValidConditionCheck {

	static Set<String> conditionalOperators = new HashSet<>(Arrays.asList(">", "<", "==", "=", "!=", ">=", "<="));

	public static Boolean checkValidCondition(JsonArray element, Map<String, Object> subObjectMap, String sheetNmae,
			String currentCellName, String currentCellValue, Map<String, Object> parentObjectMap) {

		List<String> allComponents = new ArrayList<>();
		String currentCellDateFormater = null;
		boolean isCurrentCellIsDateType = false;

		List<String> allOperand = new ArrayList<>();
		if (element.isJsonNull() || element == null || element.getAsJsonArray().isEmpty()
				|| element.getAsJsonArray().size() == 0) {

			return true;
		}
		for (JsonElement jsonElement : element) {

			if (jsonElement.getAsString().trim().equals("")) {
				allComponents.add("\"\"");
			} else {
				allComponents.add(jsonElement.getAsString());
			}
		}
		if (allComponents.size() > 0 && !isConditionalOperator(allComponents.get(0))) {
			return false;
		}

		if (allComponents.size() >= 2) {
			if (!(allComponents.get(0).equals("!=") && allComponents.get(1).trim().equals(""))
					&& currentCellValue.equals("")) {
				return true;
			}
		}
		allComponents.add(0, currentCellName);

		for (String operands : allComponents) {
			if (!Operator.isOperator(operands)) {
				allOperand.add(operands);
			}
		}
		int checkValidcell = 0;
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
								if (asJsonObject.has("value") && !asJsonObject.get("value").getAsString().isEmpty()) {
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
								JsonObject asJsonObject2 = asJsonObject.get("extraFormula").getAsJsonObject();
								if (asJsonObject2.has("type") && !asJsonObject2.get("type").getAsString().isEmpty()
										&& !asJsonObject2.get("type").getAsString().equals("")) {
									String asString = asJsonObject2.get("type").getAsString();
									if (CustomCellType.DATE.name().equalsIgnoreCase(asString)) {
										if (asJsonObject2.has("dateFormat")) {
											String dateFormater = asJsonObject2.get("dateFormat").getAsString();
											String dateParttan = dateFormater;
											DateFormat dateFormatEnum = Arrays
													.stream(DateFormat.values())
													.filter(format -> format.getKey()
															.equals(dateParttan))
													.findFirst().orElse(null);
											dateFormater = dateFormatEnum!=null?dateFormatEnum.getFormat():"";
											String convertDateAsParentPattern = dateFormatEnum != null
													? ConversionUtility.convertDateFormatAsPattern(
															valueAsString,
															DateFormat.DD_MM_YYYY.getFormat(),
															dateFormatEnum.getFormat())
													: null;
											
											if(convertDateAsParentPattern!=null) {
												valueAsString = convertDateAsParentPattern;
											}
											// set the current cell date formater for further use
											if (currentCellName.equals(operand)) {
												isCurrentCellIsDateType = true;
												currentCellDateFormater = dateFormater;
											}
											long convertValue = (convertDateToMilliseconds(valueAsString,
													dateFormater));

											if (isCurrentCellIsDateType && convertValue == -1) {
												return false;
											}
											valueAsString = convertValue != -1 ? Long.toString(convertValue)
													: valueAsString;
										}

										

									} else if (isCurrentCellIsDateType) {
										return false;
									}

								} else if (isCurrentCellIsDateType) {
									return false;
								}

							}

						}
						if (valueAsString.isEmpty()) {
							valueAsString = "\"\"";
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
					String[] parts = operand.split("!");
					if (parts.length != 2) {
						continue;
					}
					JsonObject parentJsonObject = JsonParser
							.parseString(new Gson().toJson(parentObjectMap.get(sheetNmae + operand.split("!")[1])))
							.getAsJsonObject();
					if (parentJsonObject.has("cellDetails")) {
						JsonObject asJsonObject = parentJsonObject.get("cellDetails").getAsJsonObject();
						if (asJsonObject.has("extraFormula")) {
							JsonObject asJsonObject2 = asJsonObject.get("extraFormula").getAsJsonObject();
							if (asJsonObject2.has("type") && !asJsonObject2.get("type").getAsString().isEmpty()
									&& !asJsonObject2.get("type").getAsString().equals("")) {
								String asString = asJsonObject2.get("type").getAsString();
								if (CustomCellType.DATE.name().equalsIgnoreCase(asString)) {
									if (asJsonObject2.has("dateFormat")) {
										String dateFormater = asJsonObject2.get("dateFormat").getAsString();
										String dateParttan = dateFormater;
										DateFormat dateFormatEnum = Arrays
												.stream(DateFormat.values())
												.filter(format -> format.getKey()
														.equals(dateParttan))
												.findFirst().orElse(null);
										dateFormater = dateFormatEnum!=null?dateFormatEnum.getFormat():"";
										String convertDateAsParentPattern = dateFormatEnum != null
												? ConversionUtility.convertDateFormatAsPattern(
														valueAsString,
														DateFormat.DD_MM_YYYY.getFormat(),
														dateFormatEnum.getFormat())
												: null;
										
										if(convertDateAsParentPattern!=null) {
											valueAsString = convertDateAsParentPattern;
										}
										Long convertValue = (convertDateToMilliseconds(valueAsString, dateFormater));
										if (isCurrentCellIsDateType && convertValue == -1) {
											return false;
										}
										valueAsString = convertValue != -1 ? Long.toString(convertValue)
												: valueAsString;
									}

								} else if (isCurrentCellIsDateType) {
									return false;
								}

							} else if (isCurrentCellIsDateType) {
								return false;
							}

						}

					}
					if (valueAsString.isEmpty()) {
						valueAsString = "\"\"";
					}
					operandMap.put(operand, valueAsString);
				}

			} else {

				if (!operand.trim().equals("\"\"") && currentCellDateFormater != null) {
					Long convertValue = (convertDateToMilliseconds(operand, currentCellDateFormater));
					if (convertValue == -1) {
						return false;
					}
					operandMap.put(operand, Long.toString(convertValue));
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
			concatenatedString.append(component);
		}
//		
		if (evaluateCondition(concatenatedString.toString())) {
			return true;
		}
		return false;
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

	public static boolean isValidCellName(String cellName) {
		try {
//			if (cellName.equals("!")) {
//				return false;
//			}
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

	public static boolean isConditionalOperator(String componet) {
		return conditionalOperators.contains(componet);
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
