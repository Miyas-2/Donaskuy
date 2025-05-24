package com.charity.Donaskuy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charity.Donaskuy.Model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
