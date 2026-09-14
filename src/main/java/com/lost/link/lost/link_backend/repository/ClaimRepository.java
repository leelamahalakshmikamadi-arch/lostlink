package com.lost.link.lost.link_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lost.link.lost.link_backend.model.Claim;

public interface ClaimRepository extends MongoRepository<Claim, String> {
    List<Claim> findByItemId(String itemId);
    Optional<Claim> findByItemIdAndOtp(String itemId, String otp);
    Optional<Claim> findByQrCode(String qrCode);
}

