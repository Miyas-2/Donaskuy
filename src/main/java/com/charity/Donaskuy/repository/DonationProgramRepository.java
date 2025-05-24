package com.charity.Donaskuy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charity.Donaskuy.Model.DonationProgram;


@Repository
public interface DonationProgramRepository extends JpaRepository<DonationProgram, Long> {
    List<DonationProgram> findByStatus(DonationProgram.ProgramStatus status);
}
