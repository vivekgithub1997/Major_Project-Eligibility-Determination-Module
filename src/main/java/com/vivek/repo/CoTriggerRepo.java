package com.vivek.repo;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vivek.model.CoTriggers;

public interface CoTriggerRepo extends JpaRepository<CoTriggers, Serializable> {

}
