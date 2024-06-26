package com.excel.dynamic.formula.util;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;

import com.excel.dynamic.formula.enums.DateFormat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ConversionUtility {

	public static String escapeJsonString(String jsonString) {
		// Replace special characters in the JSON string
		jsonString = jsonString.replace("\\", "\\\\"); // escape backslashes
		jsonString = jsonString.replace("\"", "\\\""); // escape double quotes
		jsonString = jsonString.replace("\b", "\\b"); // escape backspace
		jsonString = jsonString.replace("\f", "\\f"); // escape formfeed
		jsonString = jsonString.replace("\n", "\\n"); // escape newline
		jsonString = jsonString.replace("\r", "\\r"); // escape carriage return
		jsonString = jsonString.replace("\t", "\\t"); // escape tab
		// Add more replacements if necessary

		return jsonString;
	}

	public static String base64ToJson(String base64Encoded) {

		base64Encoded = base64Encoded.replace(':', '=');

		byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);

		String json = new String(decodedBytes);

		return json;
	}

	public static String jsonToBase64(String json) {

		return Base64.getEncoder().encodeToString(json.getBytes());
	}

	public static Object convertByteToObject(byte[] bs) {
		try {
			String jsonString = new String(bs, StandardCharsets.UTF_8);
			Gson gson = new Gson();

			return gson.fromJson(jsonString, Object.class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static Object convertObjectFormulaValidation(Object convertByteToObject) {
		Gson gson = new Gson();

		if (convertByteToObject != null) {
			String jsonArrayString = gson.toJson(convertByteToObject);
			JsonArray jsonArray = gson.fromJson(jsonArrayString, JsonArray.class);

			try {
				for (JsonElement jsonElement : jsonArray) {

					JsonObject asJsonObject = jsonElement.getAsJsonObject();

					if (asJsonObject.has("sheetData")) {
						JsonArray sheetDataArray = asJsonObject.get("sheetData").getAsJsonArray();
						for (JsonElement sheetDatas : sheetDataArray) {

							JsonObject sheetData = sheetDatas.getAsJsonObject();
							if (sheetData.has("rowData")) {
								JsonArray rowDatas = sheetData.get("rowData").getAsJsonArray();
								for (JsonElement celldatas : rowDatas) {
									JsonObject celldataobject = celldatas.getAsJsonObject();
									if (celldataobject.has("cellDetails")) {
										JsonObject cellObject;
										JsonElement cellDetailsElement = celldataobject.get("cellDetails");
										if (!cellDetailsElement.isJsonNull() && cellDetailsElement.isJsonObject()) {
											cellObject = cellDetailsElement.getAsJsonObject();
											if (cellObject.has("hasValidation")) {
												boolean asBoolean = cellObject.get("hasValidation").getAsBoolean();
												if (asBoolean) {
													if (cellObject.has("validation")) {
														String asString = cellObject.get("validation").getAsString();

														String base64ToJson = base64ToJson(asString);
														JsonObject formulaObject = gson.fromJson(base64ToJson,
																JsonObject.class);
														cellObject.add("validation", formulaObject);

													}

												}

											}

										}

									}

								}

							}

						}

					}

				}

				String jsonArrayAsString = jsonArray.toString();
				Object convertedObject = gson.fromJson(jsonArrayAsString, Object.class);
				return convertedObject;

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	public static String convertStringToJson(String string) {
		try {

			String cleanedString = string.replace("\\", "");
			return cleanedString;
		} catch (Exception e) {
			e.printStackTrace(); // Handle or log the exception as needed
			return null;
		}
	}

	public static String convertStringToJsonString(String string) {
		try {

			String cleanedString = string.replace("\\\"", "\"");
			return cleanedString;
		} catch (Exception e) {
			e.printStackTrace(); // Handle or log the exception as needed
			return null;
		}
	}

	public static String RemoveBackslashes(String replaceSlach) {

		replaceSlach = replaceSlach.replace("\\", ""); // escape backslashes
		replaceSlach = replaceSlach.replace("\"", "'"); // escape double quotes
		replaceSlach = replaceSlach.replace("\b", ""); // escape backspace
		replaceSlach = replaceSlach.replace("\f", ""); // escape formfeed
		replaceSlach = replaceSlach.replace("\n", ""); // escape newline
		replaceSlach = replaceSlach.replace("\r", ""); // escape carriage return
		replaceSlach = replaceSlach.replace("\t", ""); // escape tab

		return replaceSlach;

	}

	public static String convertByteToString(byte[] bs) {
		try {
			String jsonString = new String(bs, StandardCharsets.UTF_8);
			return jsonString;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static String convertDateFormat(String dateString) {
		SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MMM-yyyy");
		SimpleDateFormat outputFormat = new SimpleDateFormat(DateFormat.DD_MM_YYYY.getFormat());

		try {
			// Parsing the input date string
			Date date = inputFormat.parse(dateString);

			// Formatting the date in the desired output format
			String formattedDate = outputFormat.format(date);

			// Output the formatted date
			return formattedDate;
		} catch (ParseException e) {
//			e.printStackTrace();
			return null;
		}
	}

	public static String convertDateFormatAsPattern(String dateString, String previousDatePattern,
			String requiredDatePattern) {
		SimpleDateFormat inputFormat = new SimpleDateFormat(previousDatePattern);
		SimpleDateFormat outputFormat = new SimpleDateFormat(requiredDatePattern);

		if (!TypeValidation.isValidDateFormat(dateString, previousDatePattern)) {
			return null;
		}

		try {
			// Parsing the input date string
			Date date = inputFormat.parse(dateString);

			// Formatting the date in the desired output format
			String formattedDate = outputFormat.format(date);

			// Output the formatted date
			return formattedDate;
		} catch (ParseException e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	public static Date convertDateFormatAsPatternToDate(String dateString, String previousDatePattern,
			String requiredDatePattern) {
		SimpleDateFormat inputFormat = new SimpleDateFormat(previousDatePattern);
		SimpleDateFormat outputFormat = new SimpleDateFormat(requiredDatePattern);

		if (!TypeValidation.isValidDateFormat(dateString, previousDatePattern)) {
			return null;
		}

		try {
			// Parsing the input date string
			Date date = inputFormat.parse(dateString);

			// Formatting the date in the desired output format
			String formattedDate = outputFormat.format(date);

			// Output the formatted date
			return outputFormat.parse(formattedDate);
		} catch (ParseException e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	public static Date parseDateString(String dateString, String dateFormat) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			return sdf.parse(dateString);
		} catch (Exception e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String fixDecimal(String input) {
        // Regular expression to match decimal numbers
        String pattern = "^[-+]?[0-9]*\\.?[0-9]+$";

        // Compile the regular expression
        Pattern regex = Pattern.compile(pattern);

        // Check if the string matches the pattern
        if (regex.matcher(input).matches()) {
            // Check if the number is a whole number
            if (input.matches("\\d+")) {
                return input; // Return the input as is, without formatting
            } else {
                // Convert string to double and round it to 2 decimal places
                double fixedNumber = Double.parseDouble(input);
                return String.format("%.2f", fixedNumber);
            }
        } else {
            return input;
        }
    }

}
