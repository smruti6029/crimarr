package com.excel.dynamic.formula.enums;


public enum ExcelFormula {
    SQRT("SQRT", 1, ParameterType.NUMBER),
    QUOTIENT("QUOTIENT", 2, ParameterType.NUMBER),
    POWER("POWER", 2, ParameterType.NUMBER),
    LCM("LCM", 1, ParameterType.NUMBER, -1), //-1 FOR ANY NUMBER WHICH IS GREATER THAN 1
    GCD("GCD", 1, ParameterType.NUMBER, -1),
    AVERAGE("AVERAGE", 1, ParameterType.NUMBER, -1),
    ISLEAPYEAR("ISLEAPYEAR", 1, ParameterType.DATE),
    SYD("SYD", 1, ParameterType.NUMBER, -1),
    UNIQUE("UNIQUE", 1, ParameterType.ALL, -1),
    XNPV("XNPV", 3, ParameterType.ALL),
    NORM_DIST("NORMDIST", 4, ParameterType.ALL),
    NORM_INV("NORM.INV", 3, ParameterType.ALL),
    LEN("LEN", 1, ParameterType.STRING),
    LEFT("LEFT", 2, ParameterType.STRING),
    RIGHT("RIGHT", 2, ParameterType.STRING),
    MID("MID", 3, ParameterType.STRING),
    CONCATENATE("CONCATENATE", 2, ParameterType.STRING, 5),
    VLOOKUP("VLOOKUP", 2, ParameterType.ALL, 4),
    INDEX("INDEX", 1, ParameterType.ALL, -1),
    SUBSTITUTE("SUBSTITUTE", 1, ParameterType.STRING, 3),
    RANDBETWEEN("RANDBETWEEN", 2, ParameterType.NUMBER),
    SUMIF("SUMIF", 2, ParameterType.ALL, 3),
    INDIRECT("INDIRECT", 1, ParameterType.STRING),
    WORKDAY("WORKDAY", 2, ParameterType.DATE, 3),
    STDEV("STDEV", 1, ParameterType.NUMBER, -1),
    MAX("MAX", 1, ParameterType.NUMBER, -1),
    MIN("MIN", 1, ParameterType.NUMBER, -1),
    LOG("LOG", 1, ParameterType.NUMBER, 2),
    MOD("MOD", 2, ParameterType.NUMBER),
    FIND("FIND", 2, ParameterType.ALL),
    ACOS("ACOS", 1, ParameterType.NUMBER),
    MODESNGL("MODE.SNGL", 1, ParameterType.NUMBER, -1),
    MODEMULT("MODE.MULT", 1, ParameterType.NUMBER, -1),
    ADDRESS("ADDRESS", 2, ParameterType.NUMBER),
    TEXT_FORMAT("TEXT_FORMAT", 2, ParameterType.ALL),
    XIRR("XIRR", 2, ParameterType.ALL),
    DAY("DAY", 1, ParameterType.DATE),
    SUMPRODUCT("SUMPRODUCT", 1, ParameterType.ALL),
    STDEVP("STDEVP", 1, ParameterType.NUMBER),
    XOR("XOR", 2, ParameterType.NUMBER),
    ARABIC("ARABIC", 1, ParameterType.STRING),
    DATE("DATE", 3, ParameterType.NUMBER),
    IF("IF", 3, ParameterType.ALL),
    WEEKDAY("WEEKDAY", 1, ParameterType.DATE),
    HLOOKUP("HLOOKUP", 3, ParameterType.ALL),
    INDEXMATCH("INDEXMATCH", 3, ParameterType.ALL),
    CHOOSE("CHOOSE", 2, ParameterType.ALL),
    FV("FV", 4, ParameterType.ALL),
    ABS("ABS", 1, ParameterType.NUMBER),
    TRIM("TRIM", 1, ParameterType.STRING),
    SINE("SINE", 1, ParameterType.NUMBER),
    COSINE("COSINE", 1, ParameterType.NUMBER),
    TANGENT("TANGENT", 1, ParameterType.NUMBER),
    COMBINATION("COMBINATION", 2, ParameterType.NUMBER),
    PERMUTATION("PERMUTATION", 2, ParameterType.NUMBER),
    MEAN("MEAN", 1, ParameterType.NUMBER),
    MEDIAN("MEDIAN", 1, ParameterType.NUMBER),
    PRODUCT("PRODUCT", 2, ParameterType.NUMBER, -1),
    TRANSPOSE("TRANSPOSE", 1, ParameterType.ALL),
    ROW("ROW", 0, ParameterType.ALL, 1),
    ROWS("ROWS", 1, ParameterType.ALL),
    COLUMN("COLUMN", 0, ParameterType.ALL, 1),
    COS("COS", 1, ParameterType.NUMBER),
    TAN("TAN", 1, ParameterType.NUMBER),
    SIN("SIN", 1, ParameterType.NUMBER),
    EXP("EXP", 1, ParameterType.NUMBER),
    EXACT("EXACT", 2, ParameterType.ALL),
    HOUR("HOUR", 1, ParameterType.DATE),
    MINUTE("MINUTE", 1, ParameterType.DATE),
    SECOND("SECOND", 1, ParameterType.DATE),
    DAYS("DAYS", 2, ParameterType.DATE),
    MONTHS("MONTHS", 2, ParameterType.DATE),
    YEARS("YEARS", 2, ParameterType.DATE),
    FACT("FACT", 1, ParameterType.NUMBER);

    private final String formulaName;
    private final int minParameters;
    private final int maxParameters;
    private final ParameterType[] parameterTypes;

    ExcelFormula(String formulaName, int minParams, ParameterType[] types) {
        this.formulaName = formulaName;
        this.minParameters = minParams;
        this.maxParameters = minParams;
        this.parameterTypes = types;
    }

    ExcelFormula(String formulaName, int minParams, ParameterType type) {
        this(formulaName, minParams, new ParameterType[] { type });
    }

    ExcelFormula(String formulaName, int minParams, ParameterType type, int maxParameter) {
        this.formulaName = formulaName;
        this.minParameters = minParams;
        this.maxParameters = maxParameter; // Since it only accepts one ParameterType
        this.parameterTypes = new ParameterType[] { type };
//        this.defaultValue = defaultValue;
    }
	public String getFormulaName() {
        return formulaName;
    }

    public int getMinParameters() {
        return minParameters;
    }

    public int getMaxParameters() {
        return maxParameters;
    }

    public ParameterType[] getParameterTypes() {
        return parameterTypes;
    }
}
   

