const { findAvailablePets, findPetById } = require("../models/petModel");

async function listPets(req, res, next) {
  try {
    const pets = await findAvailablePets(req.query);
    return res.json({ pets });
  } catch (error) {
    return next(error);
  }
}

async function getPet(req, res, next) {
  const petId = Number(req.params.id);
  if (!Number.isInteger(petId) || petId < 1) {
    return res.status(400).json({ message: "A valid pet ID is required." });
  }
  try {
    const pet = await findPetById(petId);
    if (!pet) return res.status(404).json({ message: "Pet not found." });
    return res.json({ pet });
  } catch (error) {
    return next(error);
  }
}

module.exports = { listPets, getPet };
