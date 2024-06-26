package com.excel.dynamic.formula.dto;

import org.apache.poi.ss.usermodel.DataValidationConstraint;

public class DataValidationConstraintImpl implements DataValidationConstraint {
	

		private int validationType;
	    private int operator;
	    private String formula1;
	    private String formula2;
	    private String[] explicitListArray;
		public int getValidationType() {
			return validationType;
		}
		public void setValidationType(int validationType) {
			this.validationType = validationType;
		}
		public int getOperator() {
			return operator;
		}
		public void setOperator(int operator) {
			this.operator = operator;
		}
		public String getFormula1() {
			return formula1;
		}
		public void setFormula1(String formula1) {
			this.formula1 = formula1;
		}
		public String getFormula2() {
			return formula2;
		}
		public void setFormula2(String formula2) {
			this.formula2 = formula2;
		}
		public String[] getExplicitListArray() {
			return explicitListArray;
		}
		public void setExplicitListArray(String[] explicitListArray) {
			this.explicitListArray = explicitListArray;
		}
		public DataValidationConstraintImpl(int validationType, int operator, String formula1, String formula2,
				String[] explicitListArray) {
			super();
			this.validationType = validationType;
			this.operator = operator;
			this.formula1 = formula1;
			this.formula2 = formula2;
			this.explicitListArray = explicitListArray;
		}
		public DataValidationConstraintImpl() {
	
		}
		@Override
		public String[] getExplicitListValues() {
			// TODO Auto-generated method stub
			return explicitListArray;
		}
		@Override
		public void setExplicitListValues(String[] explicitListValues) {
			this.explicitListArray = explicitListArray;
			
		}
	    
	    





	

}
