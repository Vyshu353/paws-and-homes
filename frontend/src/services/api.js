const API_URL = import.meta.env.VITE_API_URL || "/api";
const API_ORIGIN = import.meta.env.VITE_API_URL
  ? new URL(import.meta.env.VITE_API_URL, window.location.origin).origin
  : "http://localhost:5000";

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem("pet-adoption-token");
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const fieldErrors = payload.fieldErrors && typeof payload.fieldErrors === "object"
      ? Object.entries(payload.fieldErrors)
        .map(([field, message]) => `${field}: ${message}`)
        .join("; ")
      : "";
    throw new Error(fieldErrors || payload.message || payload.detail || "Something went wrong. Please try again.");
  }
  return payload;
}

export function normalizePet(pet) {
  const breed = pet.breed || "";
  const catBreed = /\b(cat|tabby|calico|siamese|persian|maine coon|bengal|ragdoll|sphynx|russian blue|british shorthair)\b/i.test(breed);
  const rawImageUrl = pet.imageUrl || pet.image_url || "";
  const imageUrl = rawImageUrl && !/^(?:[a-z]+:)?\/\//i.test(rawImageUrl)
    ? new URL(rawImageUrl, `${API_ORIGIN}/`).toString()
    : rawImageUrl;
  const status = (pet.status || "AVAILABLE").toUpperCase();

  return {
    ...pet,
    species: pet.species || (catBreed ? "Cat" : "Dog"),
    location: pet.location || "Location not listed",
    imageUrl,
    image_url: imageUrl,
    adoption_fee: pet.adoptionFee == null && pet.adoption_fee == null
      ? null
      : Number(pet.adoptionFee ?? pet.adoption_fee),
    available: pet.available ?? status === "AVAILABLE",
    status,
  };
}

export function formatINR(amount) {
  if (amount == null || !Number.isFinite(Number(amount))) {
    return "Contact for contribution";
  }
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

export { API_URL };
