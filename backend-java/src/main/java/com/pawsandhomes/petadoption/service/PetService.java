package com.pawsandhomes.petadoption.service;

import com.pawsandhomes.petadoption.entity.Pet;
import com.pawsandhomes.petadoption.repository.PetRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PetService {

    private final PetRepository petRepository;

    public PetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public List<Pet> findAll() {
        return findAll(false);
    }

    @Transactional(readOnly = true)
    public List<Pet> findAll(boolean includeUnavailable) {
        return includeUnavailable
                ? petRepository.findAll()
                : petRepository.findAllByStatusOrderByIdAsc("AVAILABLE");
    }

    @Transactional(readOnly = true)
    public Pet findById(Long id) {
        return findById(id, false);
    }

    @Transactional(readOnly = true)
    public Pet findById(Long id, boolean includeUnavailable) {
        return petRepository.findById(id)
                .filter(pet -> includeUnavailable || "AVAILABLE".equals(pet.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found."));
    }

    public Pet create(Pet pet) {
        pet.setId(null);
        pet.setAdoptionFee(AdoptionPricing.contributionForBreed(pet.getBreed()));
        if (pet.getStatus() == null || pet.getStatus().isBlank()) {
            pet.setStatus("AVAILABLE");
        }
        return petRepository.save(pet);
    }

    public Pet update(Long id, Pet updatedPet) {
        Pet pet = findById(id, true);
        pet.setName(updatedPet.getName());
        pet.setBreed(updatedPet.getBreed());
        pet.setAge(updatedPet.getAge());
        pet.setGender(updatedPet.getGender());
        pet.setDescription(updatedPet.getDescription());
        pet.setImageUrl(updatedPet.getImageUrl());
        pet.setLocation(updatedPet.getLocation());
        pet.setAdoptionFee(AdoptionPricing.contributionForBreed(updatedPet.getBreed()));
        pet.setStatus(updatedPet.getStatus());
        return petRepository.save(pet);
    }

    public void delete(Long id) {
        Pet pet = findById(id, true);
        petRepository.delete(pet);
    }
}
