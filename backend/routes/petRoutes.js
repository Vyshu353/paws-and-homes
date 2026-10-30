const express = require("express");
const { listPets, getPet } = require("../controllers/petController");
const router = express.Router();

router.get("/", listPets);
router.get("/:id", getPet);

module.exports = router;
