package food_ordering.service;

public enum RevenueGranularity {
	DAY("%Y-%m-%d"),
	MONTH("%Y-%m");

	private final String mysqlFormat;

	RevenueGranularity(String mysqlFormat) {
		this.mysqlFormat = mysqlFormat;
	}

	public String getMysqlFormat() {
		return mysqlFormat;
	}
}
