package com.pawsandhomes.petadoption.config;

import com.pawsandhomes.petadoption.entity.Pet;
import com.pawsandhomes.petadoption.service.AdoptionPricing;
import com.pawsandhomes.petadoption.repository.PetRepository;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SamplePetDataInitializer implements ApplicationRunner {

    private final PetRepository petRepository;

    public SamplePetDataInitializer(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    public void initialize() {
        samplePets().stream()
                .filter(pet -> !petRepository.existsByNameAndBreed(pet.getName(), pet.getBreed()))
                .forEach(petRepository::save);

        petRepository.findAll().stream()
                .filter(pet -> pet.getLocation() == null || pet.getLocation().isBlank()
                        || pet.getAdoptionFee() == null
                        || pet.getAdoptionFee().compareTo(
                                AdoptionPricing.contributionForBreed(pet.getBreed())) != 0)
                .forEach(pet -> {
                    if (pet.getLocation() == null || pet.getLocation().isBlank()) {
                        pet.setLocation(indiaLocation(pet.getName()));
                    }
                    pet.setAdoptionFee(AdoptionPricing.contributionForBreed(pet.getBreed()));
                    petRepository.save(pet);
                });
    }

    private List<Pet> samplePets() {
        return List.of(
                pet("Daisy", "Beagle", 2, "Female",
                        "A cheerful Beagle who loves sniffing trails and meeting new friends.",
                        "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=800&q=80"),
                pet("Milo", "Siberian Husky", 3, "Male",
                        "An energetic Husky who enjoys outdoor adventures and gentle play.",
                        "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=800&q=80"),
                pet("Archie", "Poodle", 1, "Male",
                        "A bright, affectionate Poodle ready to learn and bond with a family.",
                        "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=800&q=80"),
                pet("Luna", "Siamese Cat", 2, "Female",
                        "A curious Siamese cat who loves sunny windowsills and quiet company.",
                        "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=800&q=80"),
                pet("Oliver", "Maine Coon", 4, "Male",
                        "A gentle Maine Coon with a relaxed nature and a fondness for people.",
                        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=800&q=80"),
                pet("Cleo", "Bengal Cat", 1, "Female",
                        "A playful Bengal cat who likes interactive toys and exploring new spaces.",
                        "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?auto=format&fit=crop&w=800&q=80"),
                pet("Pepper", "Persian Cat", 3, "Female",
                        "A calm Persian cat who enjoys cozy naps and gentle affection.",
                        "https://images.unsplash.com/photo-1495360010541-f48722b34f7d?auto=format&fit=crop&w=800&q=80"),
                pet("Nala", "Ragdoll Cat", 2, "Female",
                        "A sweet Ragdoll who loves relaxing near her people and being brushed.",
                        "https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=800&q=80"),
                pet("Tofu", "Calico Cat", 1, "Male",
                        "A curious Calico kitten who is happiest playing and exploring.",
                        "https://images.unsplash.com/photo-1543852786-1cf6624b9987?auto=format&fit=crop&w=800&q=80"),
                pet("Peanut", "Chihuahua", 2, "Female",
                        "A tiny, confident Chihuahua who loves warm laps and neighborhood strolls.",
                        "https://images.unsplash.com/photo-1551232864-3f0890e580d9?auto=format&fit=crop&w=800&q=80"),
                pet("Mochi", "Pomeranian", 3, "Male",
                        "A fluffy Pomeranian with a big personality and a playful spirit.",
                        "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=800&q=80"),
                pet("Waffles", "Dachshund", 4, "Female",
                        "A friendly little Dachshund who enjoys sniffing about and curling up nearby.",
                        "https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?auto=format&fit=crop&w=800&q=80"),
                pet("Pip", "Yorkshire Terrier", 1, "Male",
                        "An affectionate Yorkshire Terrier puppy ready for gentle play and cuddles.",
                        "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=800&q=80"));
    }

    private Pet pet(String name, String breed, int age, String gender, String description, String imageUrl) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setBreed(breed);
        pet.setAge(age);
        pet.setGender(gender);
        pet.setDescription(description);
        pet.setImageUrl(imageUrl);
        pet.setLocation(indiaLocation(name));
        pet.setAdoptionFee(AdoptionPricing.contributionForBreed(breed));
        pet.setStatus("AVAILABLE");
        return pet;
    }

    private String indiaLocation(String name) {
        return Map.ofEntries(
                Map.entry("Bruno", "Mumbai, Maharashtra, India"),
                Map.entry("Max", "Bengaluru, Karnataka, India"),
                Map.entry("Daisy", "Pune, Maharashtra, India"),
                Map.entry("Milo", "New Delhi, India"),
                Map.entry("Archie", "Hyderabad, Telangana, India"),
                Map.entry("Luna", "Chennai, Tamil Nadu, India"),
                Map.entry("Oliver", "Kolkata, West Bengal, India"),
                Map.entry("Cleo", "Jaipur, Rajasthan, India"),
                Map.entry("Pepper", "Kochi, Kerala, India"),
                Map.entry("Nala", "Ahmedabad, Gujarat, India"),
                Map.entry("Tofu", "Lucknow, Uttar Pradesh, India"),
                Map.entry("Peanut", "Goa, India"),
                Map.entry("Mochi", "Surat, Gujarat, India"),
                Map.entry("Waffles", "Indore, Madhya Pradesh, India"),
                Map.entry("Pip", "Chandigarh, India"))
                .getOrDefault(name, "Mumbai, Maharashtra, India");
    }
}
