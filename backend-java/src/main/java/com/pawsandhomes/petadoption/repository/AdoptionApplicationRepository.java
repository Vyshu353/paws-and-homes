package com.pawsandhomes.petadoption.repository;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {

    List<AdoptionApplication> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<AdoptionApplication> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByPet_IdAndUser_IdAndStatus(Long petId, Long userId, String status);
}
