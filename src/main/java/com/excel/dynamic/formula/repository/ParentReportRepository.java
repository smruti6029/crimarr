package com.excel.dynamic.formula.repository;

import java.util.Optional;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.excel.dynamic.formula.dto.PaginatedRequestDto;
import com.excel.dynamic.formula.model.ParentReportData;

@Repository
public interface ParentReportRepository extends JpaRepository<ParentReportData, Long> {

	Optional<ParentReportData> findByExcelFileName(String excelFileName);

	Page<ParentReportData> findByExcelFileNameContaining(String excelFileName, Pageable pageable);

	static Specification<ParentReportData> search(PaginatedRequestDto paginatedRequest) {
		return (root, cq, cb) -> {
			Predicate p = cb.conjunction();
			if (paginatedRequest.getParentFileName() != null) {
				String searchString = "%" + paginatedRequest.getParentFileName().toLowerCase() + "%";
				p = cb.and(p, cb.like(cb.lower(root.get("excelFileName")), searchString));
			}
			p = cb.and(p, cb.equal(root.get("isActive"), true));
			cq.orderBy(cb.desc(root.get("updatedAt")));
			return p;

		};
	}

	Page<ParentReportData> findAll(Specification<ParentReportData> parentData, Pageable pageable);

	default Page<ParentReportData> findAll(PaginatedRequestDto paginatedRequest, Pageable pageable) throws Exception {
		try {
			return findAll(search(paginatedRequest), pageable);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error occurred while fetching data.");

		}
	}

}
