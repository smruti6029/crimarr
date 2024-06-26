package com.excel.dynamic.formula.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.excel.dynamic.formula.model.Configuration;

public interface ConfigurationRepository extends JpaRepository<Configuration, Integer> {

	Optional<Configuration> findByKey(String string);
	
	

}
