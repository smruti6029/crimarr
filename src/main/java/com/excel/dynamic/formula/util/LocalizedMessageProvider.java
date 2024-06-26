package com.excel.dynamic.formula.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class LocalizedMessageProvider {
	
	@Autowired
	private MessageSource messageSource;
	
	public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, null);
    }

}
