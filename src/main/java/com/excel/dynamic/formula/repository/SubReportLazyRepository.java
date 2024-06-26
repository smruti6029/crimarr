package com.excel.dynamic.formula.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.excel.dynamic.formula.model.SubReportDataLazy;

@Repository
public interface SubReportLazyRepository extends JpaRepository<SubReportDataLazy, Long> {

	@Query(value = "SELECT * FROM sub_request_data where parent_report_id=?1 and is_active=1", nativeQuery = true)
	List<SubReportDataLazy> findAllByParentReportId(Long id);

	@Query(value = "SELECT * FROM sub_request_data where parent_report_id=?1 and report_name=?2 and is_active=1", nativeQuery = true)
	Optional<SubReportDataLazy> findByParentReportIdAndSubReportName(Long parentReportId, String subReportName);

	@Query(value = "SELECT * FROM sub_request_data where parent_report_id in ?1 and is_active=1", nativeQuery = true)
	List<SubReportDataLazy> findAllByParentReportDataIn(List<Long> parentIdList);

}
