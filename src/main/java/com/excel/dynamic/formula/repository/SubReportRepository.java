package com.excel.dynamic.formula.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.excel.dynamic.formula.dto.SubReportDataDto;
import com.excel.dynamic.formula.model.SubReportData;

@Repository
public interface SubReportRepository extends JpaRepository<SubReportData, Long> {

	@Query(value = "SELECT * FROM sub_request_data where parent_report_id=?1 and is_active=1", nativeQuery = true)
	List<SubReportData> findAllByParentReportId(Long id);

	@Query(value = "SELECT * FROM sub_request_data where parent_report_id=?1 and report_name=?2 and is_active=1", nativeQuery = true)
	Optional<SubReportData> findByParentReportIdAndSubReportName(Long parentReportId, String subReportName);
	
	
	Optional<SubReportData> findByReportName(String subReportName);

	@Query(value = "SELECT * FROM sub_request_data where parent_report_id in ?1 and is_active=1", nativeQuery = true)
	List<SubReportData> findAllByParentReportDataIn(List<Long> parentIdList);

	@Query(value = "SELECT * FROM sub_request_data where id=?1 and parent_report_id=?2 and is_active=1 limit 1", nativeQuery = true)
	Optional<SubReportData> findByIdAndParentId(Long subReportId, Long parentId);

}
