package com.vivek.response;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EligResponse {
	private String planName;
	private String planStatus;
	private LocalDate planStartDate;
	private LocalDate planEndDate;
	private Integer benefitAmount;
	private String denialReason;

}
