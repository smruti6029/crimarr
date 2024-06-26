package com.excel.dynamic.formula.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.excel.dynamic.formula.enums.Status;

@Entity
@Table(name = "sub_request_data")
public class SubReportData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "report_name")
	private String reportName;

	@OneToOne
	@JoinColumn(name = "parent_report_id")
	private ParentReportData parentReportData;

	@Column(name = "request_object")
	private byte[] requestObject;

	@Column(name = "created_at")
	private Date createdAt;
	
	@Column(name = "updated_at")
	private Date updatedAt;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "error_count")
	private Long errorCount;

	@Enumerated(EnumType.STRING)
	private Status status;

	@Column(name = "error_message")
	private String errorMessageList;
	
	@Column(name = "parent_using_interreport")
	private String allParentReportUsnigInInterReport;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public ParentReportData getParentReportData() {
		return parentReportData;
	}

	public void setParentReportData(ParentReportData parentReportData) {
		this.parentReportData = parentReportData;
	}

	public byte[] getRequestObject() {
		return requestObject;
	}

	public void setRequestObject(byte[] requestObject) {
		this.requestObject = requestObject;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public SubReportData() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Long errorCount) {
		this.errorCount = errorCount;
	}
	
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	

	public String getErrorMessageList() {
		return errorMessageList;
	}

	public void setErrorMessageList(String errorMessageList) {
		this.errorMessageList = errorMessageList;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getAllParentReportUsnigInInterReport() {
		return allParentReportUsnigInInterReport;
	}

	public void setAllParentReportUsnigInInterReport(String allParentReportUsnigInInterReport) {
		this.allParentReportUsnigInInterReport = allParentReportUsnigInInterReport;
	}

	public SubReportData(Long id, String reportName, ParentReportData parentReportData, byte[] requestObject,
			Date createdAt, Boolean isActive) {
		super();
		this.id = id;
		this.reportName = reportName;
		this.parentReportData = parentReportData;
		this.requestObject = requestObject;
		this.createdAt = createdAt;
		this.isActive = isActive;
	}

}
