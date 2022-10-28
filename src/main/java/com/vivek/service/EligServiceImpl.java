package com.vivek.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vivek.model.CitizenApplicationModel;
import com.vivek.model.CoTriggers;
import com.vivek.model.DcCasesModel;
import com.vivek.model.DcChildrensModel;
import com.vivek.model.DcEducationModel;
import com.vivek.model.DcIncomeModel;
import com.vivek.model.EligDtls;
import com.vivek.model.PlanModel;
import com.vivek.repo.CitizenApplicationRepo;
import com.vivek.repo.CoTriggerRepo;
import com.vivek.repo.DcCasesRepo;
import com.vivek.repo.DcChildrensRepo;
import com.vivek.repo.DcEducationRepo;
import com.vivek.repo.DcIncomeRepo;
import com.vivek.repo.EligDtlsRepo;
import com.vivek.repo.PlanRepo;
import com.vivek.response.EligResponse;

@Service
public class EligServiceImpl implements EligService {

	@Autowired
	private DcCasesRepo casesRepo;

	@Autowired
	private PlanRepo planrepo;

	@Autowired
	private DcChildrensRepo childRepo;

	@Autowired
	private DcIncomeRepo incomeRepo;

	@Autowired
	private DcEducationRepo educationRepo;

	@Autowired
	private CitizenApplicationRepo citizenRepo;

	@Autowired
	private EligDtlsRepo eligRepo;

	@Autowired
	private CoTriggerRepo triggerRepo;

	@Override
	public EligResponse eligibilityCheck(Long caseNum) {

		Integer appId = null;
		Integer planId = null;
		String planName = null;

		Optional<DcCasesModel> dcCase = casesRepo.findById(caseNum);
		if (dcCase.isPresent()) {
			DcCasesModel dcCasesModel = dcCase.get();

			appId = dcCasesModel.getAppId();
			planId = dcCasesModel.getPlanId();

		}
		Optional<PlanModel> planModel = planrepo.findById(planId);
		if (planModel.isPresent()) {
			PlanModel plan = planModel.get();
			planName = plan.getPlanName();
		}

		Optional<CitizenApplicationModel> app = citizenRepo.findById(appId);
		Integer age = 0;
		CitizenApplicationModel citizenApplicationModel = null;
		if (app.isPresent()) {
			citizenApplicationModel = app.get();
			LocalDate dob = citizenApplicationModel.getDob();
			LocalDate now = LocalDate.now();
			age = Period.between(dob, now).getYears();
		}
		EligResponse eligResponse = planCondition(caseNum, planName, age);
		// Store date to data base
		EligDtls eligDtls = new EligDtls();
		BeanUtils.copyProperties(eligResponse, eligDtls);

		eligDtls.setCaseNum(caseNum);
		eligDtls.setHolderName(citizenApplicationModel.getFullName());
		eligDtls.setHolderSsn(citizenApplicationModel.getSsn());

		eligRepo.save(eligDtls);

		CoTriggers coTriggers = new CoTriggers();
		coTriggers.setCaseNum(caseNum);
		coTriggers.setTrgStatus("Pending");

		triggerRepo.save(coTriggers);

		return eligResponse;
	}

	private EligResponse planCondition(Long caseNum, String planName, Integer age) {
		EligResponse eligResponse = new EligResponse();
		eligResponse.setPlanName(planName);

		DcIncomeModel income = incomeRepo.findByCaseNum(caseNum);
		if ("SNAP".equals(planName)) {
			Double empIncome = income.getEmpIncome();
			if (empIncome <= 300) {
				eligResponse.setPlanStatus("AP");
			} else {
				eligResponse.setPlanStatus("DN");
				eligResponse.setDenialReason("HIGH INCOME");
			}
		} else if ("CCAP".equals(planName)) {
			boolean ageCondi = true;
			boolean kidCount = false;

			List<DcChildrensModel> listOfChild = childRepo.findByCaseNum(caseNum);
			if (!listOfChild.isEmpty()) {
				kidCount = true;
				for (DcChildrensModel dcChildrensModel : listOfChild) {
					Integer childrenAge = dcChildrensModel.getChildrenAge();
					if (childrenAge > 16) {
						ageCondi = false;
						break;
					}

				}
			}
			if (income.getEmpIncome() <= 300 && kidCount && ageCondi) {
				eligResponse.setPlanStatus("AP");
			} else {
				eligResponse.setPlanStatus("NP");
				eligResponse.setDenialReason("CHILDREN AGE NOT MATCH..!!");

			}
		} else if ("Medicaid".equals(planName)) {
			Double empIncome = income.getEmpIncome();
			Integer propertyIncome = income.getPropertyIncome();
			if (empIncome <= 300 && propertyIncome == 0) {

				eligResponse.setPlanStatus("AP");
			} else {
				eligResponse.setPlanStatus("NP");
				eligResponse.setDenialReason("HIGH INCOME");
			}
		} else if ("Medicare".equals(planName)) {
			if (age >= 65) {
				eligResponse.setPlanStatus("AP");
			} else {
				eligResponse.setPlanStatus("NP");
				eligResponse.setDenialReason("YOUR AGE IS UNDER:- 65");
			}
		} else if ("NJW".equals(planName)) {
			DcEducationModel dcEducationModel = educationRepo.findByCaseNum(caseNum);
			Integer graduationYear = dcEducationModel.getGraduationYear();
			int currentYear = LocalDate.now().getYear();

			if (income.getEmpIncome() <= 0 && graduationYear < currentYear) {

				eligResponse.setPlanStatus("AP");
			} else {
				eligResponse.setPlanStatus("NP");
				eligResponse.setDenialReason("RULES NOT MATCH...!!");
			}
		}

		if (eligResponse.getPlanStatus().equals("AP")) {
			eligResponse.setPlanStartDate(LocalDate.now());
			eligResponse.setPlanEndDate(LocalDate.now().plusMonths(6));
			eligResponse.setBenefitAmount(300);
		}

		return eligResponse;
	}

}
