package com.charity.Donaskuy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.Model.DonationProgram.ProgramStatus;
import com.charity.Donaskuy.Model.User;

@Repository
public interface DonationProgramRepository extends JpaRepository<DonationProgram, Long> {

    List<DonationProgram> findByStatus(DonationProgram.ProgramStatus status);

    List<DonationProgram> findByUser(User user);

    List<DonationProgram> findByStatusAndDonationStatus(
            DonationProgram.ProgramStatus status,
            DonationProgram.DonationStatus donationStatus
    );

    List<DonationProgram> findByStatusAndDonationStatusAndCategoryId(
            DonationProgram.ProgramStatus status,
            DonationProgram.DonationStatus donationStatus,
            Long categoryId
    );



    long countByStatus(ProgramStatus status);

    List<DonationProgram> findByCategory_Id(Long categoryId);

}
