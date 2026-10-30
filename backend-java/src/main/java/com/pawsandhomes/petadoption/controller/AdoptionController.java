package com.pawsandhomes.petadoption.controller;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import com.pawsandhomes.petadoption.entity.User;
import com.pawsandhomes.petadoption.repository.UserRepository;
import com.pawsandhomes.petadoption.service.AdoptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/adoptions")
public class AdoptionController {

    private final AdoptionService adoptionService;
    private final UserRepository userRepository;

    public AdoptionController(AdoptionService adoptionService, UserRepository userRepository) {
        this.adoptionService = adoptionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody CreateAdoptionRequest request,
            Authentication authentication) {
        User user = currentUser(authentication);
        AdoptionApplication adoption = adoptionService.create(user, request.petId(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("adoption", response(adoption)));
    }

    @GetMapping("/mine")
    public Map<String, List<Map<String, Object>>> findMine(Authentication authentication) {
        User user = currentUser(authentication);
        return Map.of("adoptions", adoptionService.findMine(user.getId()).stream()
                .map(AdoptionController::response)
                .toList());
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is no longer valid."));
    }

    private static Map<String, Object> response(AdoptionApplication adoption) {
        return Map.of(
                "id", adoption.getId(),
                "pet_id", adoption.getPet().getId(),
                "pet_name", adoption.getPet().getName(),
                "image_url", adoption.getPet().getImageUrl() == null ? "" : adoption.getPet().getImageUrl(),
                "notes", adoption.getNotes(),
                "status", adoption.getStatus().toLowerCase(),
                "created_at", adoption.getCreatedAt().toString(),
                "adoption_fee", adoption.getPet().getAdoptionFee());
    }

    public record CreateAdoptionRequest(
            @NotNull @Positive Long petId,
            @NotBlank @Size(min = 10, max = 1000) String notes) {
    }
}
