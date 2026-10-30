package com.pawsandhomes.petadoption.repository;

import com.pawsandhomes.petadoption.entity.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findAllByStatusOrderByIdAsc(String status);

    boolean existsByNameAndBreed(String name, String breed);
}
