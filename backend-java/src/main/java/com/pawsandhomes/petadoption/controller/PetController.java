package com.pawsandhomes.petadoption.controller;

import com.pawsandhomes.petadoption.entity.Pet;
import com.pawsandhomes.petadoption.service.PetService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public Map<String, List<Pet>> findAll(Authentication authentication) {
        return Map.of("pets", petService.findAll(isAdmin(authentication)));
    }

    @GetMapping("/{id}")
    public Map<String, Pet> findById(@PathVariable Long id, Authentication authentication) {
        return Map.of("pet", petService.findById(id, isAdmin(authentication)));
    }

    @PostMapping
    public ResponseEntity<Pet> create(@Valid @RequestBody Pet pet) {
        Pet createdPet = petService.create(pet);
        return ResponseEntity.created(URI.create("/api/pets/" + createdPet.getId())).body(createdPet);
    }

    @PutMapping("/{id}")
    public Pet update(@PathVariable Long id, @Valid @RequestBody Pet pet) {
        return petService.update(id, pet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
