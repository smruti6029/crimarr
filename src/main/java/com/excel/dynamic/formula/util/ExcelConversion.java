package com.excel.dynamic.formula.util;

import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType;

import com.google.gson.Gson;

public class ExcelConversion {

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

	public static String Base64ToJson(String base64Encoded) {

		base64Encoded = base64Encoded.replace(':', '=');

		byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);

		String json = new String(decodedBytes);

		return json;
	}

	public static String dataValidationConstraintToBase64(DataValidationConstraint constraint) {

		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");

		String[] explicitListValues = constraint.getExplicitListValues();
		String explicitListValuesJson;
		Gson gson = new Gson();

		if (explicitListValues != null) {
			jsonBuilder.append("\"formula1\":").append(constraint.getFormula1().toString()).append(",");

			explicitListValuesJson = gson.toJson(explicitListValues);
		} else {
			if (constraint.getValidationType() == ValidationType.FORMULA) {

				String escapeJsonString = ConversionUtility.escapeJsonString(constraint.getFormula1().toString());

				jsonBuilder.append("\"formula1\":\"").append(escapeJsonString).append("\",");

			} else {
				// jsonBuilder.append("\"formula1\":\"").append(constraint.getFormula1().toString()).append("\",");
				jsonBuilder.append("\"formula1\":\"");
				if (constraint.getFormula1() != null) {
					jsonBuilder.append(constraint.getFormula1().toString());
				} else {
					jsonBuilder.append("null");
				}
				jsonBuilder.append("\",");
			}
			explicitListValuesJson = "[null]"; // Represent an empty array
		}

		jsonBuilder.append("\"formula2\":\"").append(constraint.getFormula2()).append("\",");
		jsonBuilder.append("\"operator\":\"").append(constraint.getOperator()).append("\",");
		jsonBuilder.append("\"ValidationType\":\"").append(constraint.getValidationType()).append("\",");

		jsonBuilder.append("\"ExplicitListValues\":").append(explicitListValuesJson).append(",");

		if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
			jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
		}

		jsonBuilder.append("}");

		String json = jsonBuilder.toString();

		String base64Encoded = ConversionUtility.jsonToBase64(json);

		return base64Encoded;

	}

	public static String getColorFamily(String hexColor) {

		if (hexColor.isEmpty() || hexColor.equals("N/A") || hexColor.equals("")) {
			return "Complex Color";
		}

		// Convert Hex to RGB
		int rgb = Integer.parseInt(hexColor.substring(1), 16);
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;

		// Determine the color family
		if (red == green && green == blue) {
			if (red == 0) {
				return "Black";
			} else if (red == 255) {
				return "White";
			} else {
				return "Gray";
			}
		} else if (red > green && green > blue && red > 180 && green > 100 && blue < 100) {
			return "Orange"; // Specific condition to detect orange
		} else if (red > green && red > blue) {
			return "Red";
		} else if (green > red && green > blue) {
			return "Green";
		} else if (blue > red && blue > green) {
			return "Blue";
		} else if (red == green && red > blue) {
			return "Yellow";
		} else if (green == blue && green > red) {
			return "Cyan";
		} else if (red == blue && red > green) {
			return "Magenta";
		} else {
			return "Complex Color";
		}
	}

}
