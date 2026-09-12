package com.resjk.restaurant.repository;

import com.resjk.restaurant.model.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, String> {
}
