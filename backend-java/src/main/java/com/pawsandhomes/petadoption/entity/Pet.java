package com.pawsandhomes.petadoption.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String breed;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer age;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String gender;

    @Size(max = 5000)
    @NotNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description = "";

    @Size(max = 2048)
    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Size(max = 120)
    @Column(length = 120)
    private String location;

    @Column(name = "adoption_fee", precision = 10, scale = 2)
    private BigDecimal adoptionFee = BigDecimal.valueOf(5000);

    @NotBlank
    @Pattern(regexp = "AVAILABLE|PENDING|ADOPTED", message = "Status must be AVAILABLE, PENDING, or ADOPTED.")
    @Column(nullable = false, length = 20)
    private String status = "AVAILABLE";

    public Pet() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getAdoptionFee() {
        return adoptionFee;
    }

    public void setAdoptionFee(BigDecimal adoptionFee) {
        this.adoptionFee = adoptionFee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
