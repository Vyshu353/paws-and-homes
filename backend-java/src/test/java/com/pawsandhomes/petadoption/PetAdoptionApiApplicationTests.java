package com.pawsandhomes.petadoption;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pawsandhomes.petadoption.config.SamplePetDataInitializer;
import com.pawsandhomes.petadoption.entity.Pet;
import com.pawsandhomes.petadoption.entity.User;
import com.pawsandhomes.petadoption.entity.UserRole;
import com.pawsandhomes.petadoption.repository.PetRepository;
import com.pawsandhomes.petadoption.repository.UserRepository;
import com.pawsandhomes.petadoption.service.EmailService;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class PetAdoptionApiApplicationTests {

	private static final String EMAIL = "phase3-test@example.com";
	private static final String PASSWORD = "Testing-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private SamplePetDataInitializer samplePetDataInitializer;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private EmailService emailService;

	@DynamicPropertySource
	static void jwtProperties(DynamicPropertyRegistry registry) {
	    byte[] key = new byte[48];
	    new SecureRandom().nextBytes(key);
	    registry.add("app.jwt.secret", () -> Base64.getEncoder().encodeToString(key));
	}

	@BeforeEach
	void clearTestDatabase() {
	    petRepository.deleteAll();
	    userRepository.deleteAll();
	}

	@Test
	void samplePetsAreAddedWithoutDuplicates() throws Exception {
	    Pet existingLargeDog = new Pet();
	    existingLargeDog.setName("Milo");
	    existingLargeDog.setBreed("Siberian Husky");
	    existingLargeDog.setAge(3);
	    existingLargeDog.setGender("Male");
	    existingLargeDog.setDescription("Existing pet record");
	    existingLargeDog.setLocation("Existing location, India");
	    existingLargeDog.setAdoptionFee(java.math.BigDecimal.valueOf(7000));
	    petRepository.saveAndFlush(existingLargeDog);

	    samplePetDataInitializer.initialize();
	    samplePetDataInitializer.initialize();

	    org.junit.jupiter.api.Assertions.assertEquals(13, petRepository.count());
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Luna", "Siamese Cat"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Daisy", "Beagle"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Pepper", "Persian Cat"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Nala", "Ragdoll Cat"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Tofu", "Calico Cat"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Peanut", "Chihuahua"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Mochi", "Pomeranian"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Waffles", "Dachshund"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.existsByNameAndBreed("Pip", "Yorkshire Terrier"));
	    org.junit.jupiter.api.Assertions.assertTrue(petRepository.findAll().stream()
	            .allMatch(pet -> pet.getLocation() != null && pet.getLocation().endsWith(", India")));
	    org.junit.jupiter.api.Assertions.assertEquals(0, petRepository.findAll().stream()
	            .filter(pet -> pet.getName().equals("Luna"))
	            .findFirst().orElseThrow().getAdoptionFee().compareTo(java.math.BigDecimal.valueOf(2000)));
	    org.junit.jupiter.api.Assertions.assertEquals(0, petRepository.findAll().stream()
	            .filter(pet -> pet.getName().equals("Peanut"))
	            .findFirst().orElseThrow().getAdoptionFee().compareTo(java.math.BigDecimal.valueOf(3000)));
	    org.junit.jupiter.api.Assertions.assertEquals(0, petRepository.findAll().stream()
	            .filter(pet -> pet.getName().equals("Daisy"))
	            .findFirst().orElseThrow().getAdoptionFee().compareTo(java.math.BigDecimal.valueOf(5000)));
	    org.junit.jupiter.api.Assertions.assertEquals(0, petRepository.findAll().stream()
	            .filter(pet -> pet.getName().equals("Milo"))
	            .findFirst().orElseThrow().getAdoptionFee().compareTo(java.math.BigDecimal.valueOf(5000)));
	    org.junit.jupiter.api.Assertions.assertEquals(
	            "Existing location, India",
	            petRepository.findAll().stream()
	                    .filter(pet -> pet.getName().equals("Milo"))
	                    .findFirst().orElseThrow().getLocation());
	    mockMvc.perform(get("/api/pets"))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.pets.length()").value(13))
	            .andExpect(jsonPath("$.pets[0].location").exists())
	            .andExpect(jsonPath("$.pets[0].adoptionFee").value(5000));
	}

	@Test
	void adoptionApplicationsRequireLoginAndAreVisibleOnlyToTheirOwner() throws Exception {
	    Pet pet = new Pet();
	    pet.setName("Bruno");
	    pet.setBreed("Golden Retriever");
	    pet.setAge(3);
	    pet.setGender("Male");
	    pet.setDescription("Friendly Bruno is ready for a loving home.");
	    pet.setLocation("Mumbai, Maharashtra, India");
	    pet.setAdoptionFee(java.math.BigDecimal.valueOf(5000));
	    pet.setStatus("AVAILABLE");
	    pet = petRepository.saveAndFlush(pet);

	    mockMvc.perform(post("/api/adoptions")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"petId":%d,"notes":"We have a safe, loving home."}
	                            """.formatted(pet.getId())))
	            .andExpect(status().isUnauthorized());

	    MvcResult registration = mockMvc.perform(post("/api/auth/signup")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {
	                              "name":"Adoption Applicant",
	                              "email":"adoption-applicant@example.com",
	                              "password":"Adoption-password-123"
	                            }
	                            """))
	            .andExpect(status().isCreated())
	            .andReturn();
	    String token = objectMapper.readTree(registration.getResponse().getContentAsString())
	            .get("token").stringValue();

	    mockMvc.perform(post("/api/adoptions")
	                    .header("Authorization", "Bearer " + token)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"petId":%d,"notes":"good home"}
	                            """.formatted(pet.getId())))
	            .andExpect(status().isBadRequest())
	            .andExpect(jsonPath("$.detail").value("One or more fields are invalid."))
	            .andExpect(jsonPath("$.fieldErrors.notes").value("size must be between 10 and 1000"));

	    MvcResult created = mockMvc.perform(post("/api/adoptions")
	                    .header("Authorization", "Bearer " + token)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"petId":%d,"notes":"We have a safe, loving home."}
	                            """.formatted(pet.getId())))
	            .andExpect(status().isCreated())
	            .andExpect(jsonPath("$.adoption.pet_name").value("Bruno"))
	            .andExpect(jsonPath("$.adoption.status").value("pending"))
	            .andExpect(jsonPath("$.adoption.pet_id").value(pet.getId()))
	            .andExpect(jsonPath("$.adoption.adoption_fee").value(5000))
	            .andReturn();
	    long adoptionId = objectMapper.readTree(created.getResponse().getContentAsString())
	            .path("adoption").path("id").asLong();

	    mockMvc.perform(get("/api/adoptions/mine")
	                    .header("Authorization", "Bearer " + token))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.adoptions.length()").value(1))
	            .andExpect(jsonPath("$.adoptions[0].pet_name").value("Bruno"));

	    mockMvc.perform(post("/api/payments/checkout")
	                    .header("Authorization", "Bearer " + token)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"adoptionId":%d}
	                            """.formatted(adoptionId)))
	            .andExpect(status().isServiceUnavailable())
	            .andExpect(jsonPath("$.detail").value(
	                    containsString("Set STRIPE_SECRET_KEY")));

	    mockMvc.perform(post("/api/payments/checkout")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"adoptionId":%d}
	                            """.formatted(adoptionId)))
	            .andExpect(status().isUnauthorized());

	    mockMvc.perform(post("/api/payments/razorpay/orders")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"adoptionId":%d}
	                            """.formatted(adoptionId)))
	            .andExpect(status().isUnauthorized());
	    mockMvc.perform(post("/api/payments/razorpay/orders")
	                    .header("Authorization", "Bearer " + token)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"adoptionId":%d}
	                            """.formatted(adoptionId)))
	            .andExpect(status().isServiceUnavailable())
	            .andExpect(jsonPath("$.detail").value(containsString("RAZORPAY_KEY_ID")));
	    mockMvc.perform(post("/api/payments/razorpay/verify")
	                    .header("Authorization", "Bearer " + token)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {
	                              "orderId":"order_test",
	                              "paymentId":"pay_test",
	                              "signature":"invalid"
	                            }
	                            """))
	            .andExpect(status().isBadRequest())
	            .andExpect(jsonPath("$.detail").value(containsString("signature verification failed")));

	    mockMvc.perform(post("/api/adoptions")
	                    .header("Authorization", "Bearer " + token)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"petId":%d,"notes":"We have a safe, loving home."}
	                            """.formatted(pet.getId())))
	            .andExpect(status().isConflict());
	}

	@Test
	void registrationLoginAndPetAccessFollowRoleRules() throws Exception {
	    String registerBody = """
	            {
	              "fullName": "Phase Three Test",
	              "email": "%s",
	              "password": "%s",
	              "phoneNumber": "555-0100"
	            }
	            """.formatted(EMAIL, PASSWORD);

	    MvcResult registration = mockMvc.perform(post("/api/auth/register")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(registerBody))
	            .andExpect(status().isCreated())
	            .andExpect(jsonPath("$.token").isNotEmpty())
	            .andExpect(jsonPath("$.tokenType").value("Bearer"))
	            .andExpect(jsonPath("$.user.email").value(EMAIL))
	            .andExpect(jsonPath("$.user.role").value("USER"))
	            .andExpect(jsonPath("$.user.password").doesNotExist())
	            .andReturn();

	    User storedUser = userRepository.findByEmail(EMAIL).orElseThrow();
	    org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, storedUser.getPassword());
	    org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, storedUser.getPassword()));

	    mockMvc.perform(post("/api/auth/register")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(registerBody))
	            .andExpect(status().isConflict());

	    mockMvc.perform(post("/api/auth/register")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"fullName":"","email":"bad-email","password":"short"}
	                            """))
	            .andExpect(status().isBadRequest());

	    String userToken = objectMapper.readTree(registration.getResponse().getContentAsString())
	            .get("token").stringValue();
	    mockMvc.perform(get("/api/auth/me")
	                    .header("Authorization", "Bearer " + userToken))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.user.fullName").value("Phase Three Test"))
	            .andExpect(jsonPath("$.user.name").value("Phase Three Test"));
	    mockMvc.perform(post("/api/pets")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(validPetJson()))
	            .andExpect(status().isUnauthorized());
	    mockMvc.perform(post("/api/pets")
	                    .header("Authorization", "Bearer " + userToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(validPetJson()))
	            .andExpect(status().isForbidden());

	    mockMvc.perform(get("/api/pets"))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.pets").isArray());
	    mockMvc.perform(get("/images/max.jpeg"))
	            .andExpect(status().isOk());

	    String loginBody = """
	            {"email":"%s","password":"%s","role":"USER"}
	            """.formatted(EMAIL, PASSWORD);
	    MvcResult login = mockMvc.perform(post("/api/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(loginBody))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.token").isNotEmpty())
	            .andExpect(jsonPath("$.user.role").value("USER"))
	            .andReturn();
	    String loginToken = objectMapper.readTree(login.getResponse().getContentAsString())
	            .get("token").stringValue();
	    org.junit.jupiter.api.Assertions.assertEquals(
	            2, loginToken.chars().filter(character -> character == '.').count());

	    mockMvc.perform(get("/api/auth/me"))
	            .andExpect(status().isUnauthorized());

	    mockMvc.perform(post("/api/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"email":"%s","password":"Wrong-password-123"}
	                            """.formatted(EMAIL)))
	            .andExpect(status().isUnauthorized())
	            .andExpect(jsonPath("$.detail").value(containsString("Invalid email or password")));

	    String adminSelectionBody = """
	            {"email":"%s","password":"%s","role":"ADMIN"}
	            """.formatted(EMAIL, PASSWORD);
	    mockMvc.perform(post("/api/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(adminSelectionBody))
	            .andExpect(status().isForbidden())
	            .andExpect(jsonPath("$.detail").value(containsString("administrator access")));

	    storedUser.setRole(UserRole.ADMIN);
	    userRepository.saveAndFlush(storedUser);
	    String adminLoginBody = """
	            {"email":"%s","password":"%s","role":"ADMIN"}
	            """.formatted(EMAIL, PASSWORD);
	    MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(adminLoginBody))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.user.role").value("ADMIN"))
	            .andReturn();
	    String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
	            .get("token").stringValue();

	    MvcResult regularUserRegistration = mockMvc.perform(post("/api/auth/signup")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {
	                              "name":"Regular Test User",
	                              "email":"regular-test@example.com",
	                              "password":"Another-password-123"
	                            }
	                            """))
	            .andExpect(status().isCreated())
	            .andExpect(jsonPath("$.user.role").value("USER"))
	            .andReturn();
	    String regularUserToken = objectMapper.readTree(
	                    regularUserRegistration.getResponse().getContentAsString())
	                    .get("token").stringValue();

	    MvcResult createdPet = mockMvc.perform(post("/api/pets")
	                    .header("Authorization", "Bearer " + adminToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(validPetJson()))
	            .andExpect(status().isCreated())
	            .andExpect(jsonPath("$.status").value("AVAILABLE"))
	            .andExpect(jsonPath("$.adoptionFee").value(5000))
	            .andReturn();
	    JsonNode petJson = objectMapper.readTree(createdPet.getResponse().getContentAsString());
	    long petId = petJson.get("id").asLong();

	    mockMvc.perform(get("/api/pets/" + petId))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.pet.name").value("Test pet"));

	    mockMvc.perform(put("/api/pets/" + petId)
	                    .header("Authorization", "Bearer " + adminToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {
	                              "name":"Updated test pet",
	                              "breed":"Mixed breed",
	                              "age":4,
	                              "gender":"Female",
	                              "description":"Updated test description",
	                              "imageUrl":"test-updated.jpg",
	                              "adoptionFee":1,
	                              "status":"PENDING"
	                            }
	                            """))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.status").value("PENDING"))
	            .andExpect(jsonPath("$.adoptionFee").value(5000));

	    mockMvc.perform(get("/api/pets"))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.pets[?(@.id == %d)]".formatted(petId)).doesNotExist());
	    mockMvc.perform(get("/api/pets/" + petId))
	            .andExpect(status().isNotFound());

	    mockMvc.perform(get("/api/pets/" + petId)
	                    .header("Authorization", "Bearer " + adminToken))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.pet.status").value("PENDING"));

	    mockMvc.perform(delete("/api/pets/" + petId)
	                    .header("Authorization", "Bearer " + regularUserToken))
	            .andExpect(status().isForbidden());
	    mockMvc.perform(delete("/api/pets/" + petId)
	                    .header("Authorization", "Bearer " + adminToken))
	            .andExpect(status().isNoContent());

	    mockMvc.perform(get("/api/pets/" + petId)
	                    .header("Authorization", "Bearer " + adminToken))
	            .andExpect(status().isNotFound());
	}

	@Test
	void developmentEmailTestRequiresAdminAndValidRecipient() throws Exception {
	    mockMvc.perform(post("/api/test-email")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"recipient":"test@example.com"}
	                            """))
	            .andExpect(status().isUnauthorized());

	    MvcResult registration = mockMvc.perform(post("/api/auth/signup")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {
	                              "name":"Email Test User",
	                              "email":"email-test-user@example.com",
	                              "password":"Email-test-password-123"
	                            }
	                            """))
	            .andExpect(status().isCreated())
	            .andReturn();
	    String userToken = objectMapper.readTree(registration.getResponse().getContentAsString())
	            .get("token").stringValue();

	    mockMvc.perform(post("/api/test-email")
	                    .header("Authorization", "Bearer " + userToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"recipient":"test@example.com"}
	                            """))
	            .andExpect(status().isForbidden());

	    User admin = userRepository.findByEmail("email-test-user@example.com").orElseThrow();
	    admin.setRole(UserRole.ADMIN);
	    userRepository.saveAndFlush(admin);
	    when(emailService.sendWelcomeTestEmail("test@example.com")).thenReturn(true);
	    MvcResult login = mockMvc.perform(post("/api/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {
	                              "email":"email-test-user@example.com",
	                              "password":"Email-test-password-123",
	                              "role":"ADMIN"
	                            }
	                            """))
	            .andExpect(status().isOk())
	            .andReturn();
	    String adminToken = objectMapper.readTree(login.getResponse().getContentAsString())
	            .get("token").stringValue();

	    mockMvc.perform(post("/api/test-email")
	                    .header("Authorization", "Bearer " + adminToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"recipient":"test@example.com"}
	                            """))
	            .andExpect(status().isOk())
	            .andExpect(jsonPath("$.success").value(true));

	    when(emailService.sendWelcomeTestEmail("test@example.com")).thenReturn(false);
	    mockMvc.perform(post("/api/test-email")
	                    .header("Authorization", "Bearer " + adminToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"recipient":"test@example.com"}
	                            """))
	            .andExpect(status().isBadGateway())
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value(
	                    "Email could not be sent. Check the development backend mail configuration."));

	    mockMvc.perform(post("/api/test-email")
	                    .header("Authorization", "Bearer " + adminToken)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content("""
	                            {"recipient":"not-an-email"}
	                            """))
	            .andExpect(status().isBadRequest())
	            .andExpect(jsonPath("$.fieldErrors.recipient").exists());
	}

	private String validPetJson() {
	    return """
	            {
	              "name":"Test pet",
	              "breed":"Mixed breed",
	              "age":2,
	              "gender":"Female",
	              "description":"Test pet for the authentication integration test",
	              "imageUrl":"test-pet.jpg",
	              "adoptionFee":1,
	              "status":"AVAILABLE"
	            }
	            """;
	}
}
