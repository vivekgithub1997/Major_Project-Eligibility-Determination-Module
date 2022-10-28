package com.vivek.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.vivek.response.EligResponse;
import com.vivek.service.EligService;

@RestController
public class EdController {

	@Autowired
	private EligService eligService;

	@GetMapping("eligibilty/{caseNum}")
	public EligResponse edDetermine(@PathVariable Long caseNum) {

		EligResponse eligResponse = eligService.eligibilityCheck(caseNum);

		return eligResponse;

	}

}
