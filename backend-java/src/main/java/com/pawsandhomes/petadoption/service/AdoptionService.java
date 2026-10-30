package com.pawsandhomes.petadoption.service;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import com.pawsandhomes.petadoption.entity.Pet;
import com.pawsandhomes.petadoption.entity.User;
import com.pawsandhomes.petadoption.repository.AdoptionApplicationRepository;
import com.pawsandhomes.petadoption.repository.PetRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdoptionService {

    private final AdoptionApplicationRepository adoptionRepository;
    private final PetRepository petRepository;

    public AdoptionService(AdoptionApplicationRepository adoptionRepository, PetRepository petRepository) {
        this.adoptionRepository = adoptionRepository;
        this.petRepository = petRepository;
    }

    public AdoptionApplication create(User user, Long petId, String notes) {
        Pet pet = petRepository.findById(petId)
                .filter(candidate -> "AVAILABLE".equals(candidate.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This pet is not available for adoption."));

        if (adoptionRepository.existsByPet_IdAndUser_IdAndStatus(petId, user.getId(), "PENDING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a pending application for this pet.");
        }

        AdoptionApplication adoption = new AdoptionApplication();
        adoption.setUser(user);
        adoption.setPet(pet);
        adoption.setNotes(notes.trim());
        return adoptionRepository.save(adoption);
    }

    @Transactional(readOnly = true)
    public List<AdoptionApplication> findMine(Long userId) {
        return adoptionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
    }
}
