package com.excel.dynamic.formula.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.excel.dynamic.formula.constant.Constant;
import com.excel.dynamic.formula.model.Configuration;
import com.excel.dynamic.formula.repository.ConfigurationRepository;

@Component
public class TypeValidation {

	@Autowired
	private ConfigurationRepository configurationRepository;

	private static final Pattern pattern = Pattern.compile(Constant.EMAIL_CHECK_REGEX);

	public static boolean isValidEmail(String email) {
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}

	public static boolean isValidDateFormat(String dateInString, String datePartten) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(datePartten);
		dateFormat.setLenient(false);

		try {
			Date date = dateFormat.parse(dateInString);
			return true;
		} catch (ParseException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
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

	// Check The Regix Is valid Or Not
	public static boolean isValidRegex(String inputString) {
		try {
			Pattern.compile(inputString);
			return true;
		} catch (PatternSyntaxException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isValidPhone(String phoneString) {
		Optional<Configuration> configurationPhone = configurationRepository.findByKey("PHONE_REGEX");
		String phonePattern = configurationPhone.isPresent() && configurationPhone.get().getValue() != null
				&& !configurationPhone.get().getValue().equals("") ? configurationPhone.get().getValue()
						: "^(?!0)[0-9]+$";
		return Pattern.matches(phonePattern, phoneString);
	}

	public boolean isValidStd(String stdNumber) {
		Optional<Configuration> configurationPhone = configurationRepository.findByKey("STD_REGEX");
		String phoneNumberPattern = configurationPhone.isPresent() && configurationPhone.get().getValue() != null
				&& !configurationPhone.get().getValue().equals("") ? configurationPhone.get().getValue():"\\b(0(?!0)\\d*|[^0]\\d*)\\b";
		Pattern pattern = Pattern.compile(phoneNumberPattern);
		Matcher matcher = pattern.matcher(stdNumber);
		return matcher.matches();
	}

}
