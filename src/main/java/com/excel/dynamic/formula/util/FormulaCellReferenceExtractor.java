package com.excel.dynamic.formula.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.util.CellReference;

public class FormulaCellReferenceExtractor {
	
	public static String adjustFormula(String formula, int countRowFromFormula) {
		Pattern pattern = Pattern.compile("[A-Z]+\\d+");
		Matcher matcher = pattern.matcher(formula);
		StringBuffer result = new StringBuffer();

		while (matcher.find()) {
			String cellName = matcher.group();
			int row = Integer.parseInt(cellName.replaceAll("[^0-9]", "")); // Extract row number
			if (countRowFromFormula != 0) {
				row += countRowFromFormula;
			}

			matcher.appendReplacement(result, cellName.replaceAll("\\d+", Integer.toString(row)));
		}
		matcher.appendTail(result);

		return result.toString();
	}

	public static String addValueToCell(String formula, int valueToAdd) {
		Pattern pattern = Pattern.compile(":([A-Z]+)([0-9]+)");
		Matcher matcher = pattern.matcher(formula);
		StringBuffer newFormula = new StringBuffer();

		while (matcher.find()) {
			String column = matcher.group(1);
			int row = Integer.parseInt(matcher.group(2));
			int newRow = row + valueToAdd;
			matcher.appendReplacement(newFormula, ":" + column + newRow);
		}
		matcher.appendTail(newFormula);

		return newFormula.toString();
	}

	public static String[] extractCellsAndSheetNamesAfterColon(String formula) {
		String[] cellsAndSheetNames = new String[2];

		// Define a regex pattern to match cell references and sheet names after a colon
		Pattern pattern = Pattern.compile("(?<=:)(\\w+!)?([A-Za-z]+\\d+)");
		Matcher matcher = pattern.matcher(formula);

		// Find the last match and store it in the cellsAndSheetNames array
		while (matcher.find()) {
			// If a sheet name is provided, store it in cellsAndSheetNames[0]
			cellsAndSheetNames[0] = matcher.group(1) != null ? matcher.group(1) : "";

			// Store the cell reference in cellsAndSheetNames[1]
			cellsAndSheetNames[1] = matcher.group(2);
		}

		return cellsAndSheetNames;
	}

	public static List<String> extractComponents(String formula) {
		List<String> components = new ArrayList<>();

		// Define a regex pattern to match function name, parentheses, and cell range
		Pattern pattern = Pattern.compile("([A-Za-z]+)\\((.*?)\\)");
		Matcher matcher = pattern.matcher(formula);

		// Check if the formula matches the pattern
		if (matcher.matches()) {
			// Add function name to components list
			components.add(matcher.group(1));

			// Add opening parenthesis to components list
			components.add("(");

			// Split cell references by colon to get start and end cells
			String[] cellRange = matcher.group(2).split(":");
			if (cellRange.length == 2) {
				// Extract individual cell names from start and end cells
				String startCell = cellRange[0];
				String endCell = cellRange[1];

				// Add start cell to components list
				components.add(startCell);

				// Add colon to components list
				components.add(":");

				// Add end cell to components list
				components.add(endCell);
			} else {
				// If only one cell is provided, add it to components list
				components.add(matcher.group(2));
			}

			// Add closing parenthesis to components list
			components.add(")");
		}

		return components;
	}

	public static String updateEndingCell(List<String> components, String endingCell, String updateCellName) {
		// Find the index of ":" in the components list

		StringBuilder concatenatedString = new StringBuilder();

		int colonIndex = components.indexOf(":");
		colonIndex++;

		String cellAfterColumn = components.get(colonIndex);
		if (cellAfterColumn.equals(endingCell)) {

			components.set(colonIndex, updateCellName);

		}
		return components.stream().collect(Collectors.joining());
	}

	public static List<String> extractOperandsAndOperators(String formula) {
		List<String> operandsAndOperators = new ArrayList<>();

		// Define a regex pattern to match operands (cell references), commas, and
		// parentheses
		 Pattern pattern = Pattern.compile("\\b[A-Z]+\\b|[A-Za-z]+\\d+|[,()]");
		Matcher matcher = pattern.matcher(formula);

		// Iterate through matches and add them to the operandsAndOperators list
		while (matcher.find()) {
			operandsAndOperators.add(matcher.group());
		}

		return operandsAndOperators;
	}
	
//	public static List<String> extractCellNamesForFormulaUpdate(String formula) {
//        List<String> cellNames = new ArrayList<>();
//        // Regular expression to match cell references like A1, B2, etc.
//        String regex = "[A-Z]+\\d+";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(formula);
//
//        while (matcher.find()) {
//            String cellName = matcher.group();
//            cellNames.add(cellName);
//        }
//        return cellNames;
//    }
	 public static List<String> extractCellNamesForFormulaUpdate(String formula) {
	        List<String> cellNames = new ArrayList<>();
	        // Regular expression to match cell references like Sheet!A1, Sheet!B2, A1, B2, etc.
	        String regex = "[A-Za-z]+![A-Za-z]+\\d+|[A-Za-z]+\\d+"; // Regular expression to match "Sheet!A1", "Sheet!B2", "A1", "B2", etc.
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(formula);

	        while (matcher.find()) {
	            String cellName = matcher.group();
	            cellNames.add(cellName);
	        }
	        return cellNames;
	    }
	
	
	 public static boolean isValidCellName(String cellName) {
			try {
				CellReference cellReference = new CellReference(cellName);
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		}
	
	
	 
	 public static List<String> extractOperandsForInterReport(String formula) {
	        List<String> operands = new ArrayList<>();
	        
//	        Pattern pattern = Pattern.compile("~[^~]+~|\\w+|\\p{Punct}");
	        
	        Pattern pattern = Pattern.compile( "\\b[A-Za-z]+(?=\\()|~[^~]*~|\\b\\w+!\\w+\\b|\\b[A-Z]+\\d+\\b|\\d*\\.\\d+|\\d+|[+\\-*/(),]");
	        Matcher matcher = pattern.matcher(formula);
	        
	        // Iterate through matches and add them to the list
	        while (matcher.find()) {
	            String operand = matcher.group().trim();
	            operands.add(operand);
	        }
	        
	        return operands;
	    }
	
	
	 public static String[] extractTextInTildes(String input) {
	        // Regular expression pattern to match text inside tildes (~)
	        Pattern pattern = Pattern.compile("~(.*?)~");
	        Matcher matcher = pattern.matcher(input);

	        // Initialize result array
	        String[] result = new String[2];

	        // If match found, extract text and split it into two parts
	        if (matcher.find()) {
	            String textInsideTildes = matcher.group(1); // Get text inside tildes
	            String[] parts = textInsideTildes.split("#", 2); // Split text into two parts using '#'
	            if (parts.length == 2) {
	                result[0] = parts[0].trim(); // First part
	                result[1] = parts[1].trim(); // Second part
	            }
	        }

	        return result;
	    }
	
	
	

}
