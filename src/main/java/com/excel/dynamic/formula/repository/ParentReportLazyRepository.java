package com.excel.dynamic.formula.repository;

import java.util.Optional;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.excel.dynamic.formula.dto.PaginatedRequestDto;
import com.excel.dynamic.formula.model.ParentReportDataLazy;

@Repository
public interface ParentReportLazyRepository extends JpaRepository<ParentReportDataLazy, Long> {

	Optional<ParentReportDataLazy> findByExcelFileName(String excelFileName);

	Page<ParentReportDataLazy> findByExcelFileNameContaining(String excelFileName, Pageable pageable);

	static Specification<ParentReportDataLazy> search(PaginatedRequestDto paginatedRequest) {
	    return (root, cq, cb) -> {
	        Predicate p = cb.conjunction();

	        if (paginatedRequest.getParentFileName() != null) {
	            String searchString = "%" + paginatedRequest.getParentFileName().toLowerCase() + "%";
	            p = cb.and(p, cb.like(cb.lower(root.get("excelFileName")), searchString));
	        }

	        if (paginatedRequest.getListOfParentFileName() != null && !paginatedRequest.getListOfParentFileName().isEmpty()) {
	            // Construct an "in" clause for the list of parent file names
	            Predicate inClause = root.get("excelFileName").in(paginatedRequest.getListOfParentFileName());
	            p = cb.and(p, inClause);
	        }

	        p = cb.and(p, cb.equal(root.get("isActive"), true));
	        cq.orderBy(cb.desc(root.get("updatedAt")));
	        return p;
	    };
	}

	Page<ParentReportDataLazy> findAll(Specification<ParentReportDataLazy> parentData, Pageable pageable);

	default Page<ParentReportDataLazy> findAll(PaginatedRequestDto paginatedRequest, Pageable pageable) throws Exception {
		try {
			return findAll(search(paginatedRequest), pageable);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error occurred while fetching data.");

		}
	}

}
