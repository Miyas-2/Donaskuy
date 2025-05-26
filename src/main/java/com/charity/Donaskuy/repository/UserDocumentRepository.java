package com.charity.Donaskuy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.Model.UserDocument;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    List<UserDocument> findAllByUser(User user);
    Optional<UserDocument> findTopByUserOrderByIdDesc(User user);
}