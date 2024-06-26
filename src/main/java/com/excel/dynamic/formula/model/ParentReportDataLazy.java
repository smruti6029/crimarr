package com.excel.dynamic.formula.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "request_data")
public class ParentReportDataLazy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "created_at")
	private Date createdAt;

	@Column(name = "excel_file_name")
	private String excelFileName;

	@Column(name = "updated_at")
	private Date updatedAt;

	@Column(name = "is_active")
	private Boolean isActive;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getExcelFileName() {
		return excelFileName;
	}

	public void setExcelFileName(String excelFileName) {
		this.excelFileName = excelFileName;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public ParentReportDataLazy(Long id, Date createdAt, String excelFileName, Date updatedAt, Boolean isActive) {
		super();
		this.id = id;
		this.createdAt = createdAt;
		this.excelFileName = excelFileName;
		this.updatedAt = updatedAt;
		this.isActive = isActive;
	}

	public ParentReportDataLazy() {
		super();
		// TODO Auto-generated constructor stub
	}

	
	
	

}
