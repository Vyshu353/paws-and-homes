const express = require("express");
const { createAdoption, listMyAdoptions } = require("../controllers/adoptionController");
const { authenticate } = require("../middleware/auth");
const router = express.Router();

router.use(authenticate);
router.get("/mine", listMyAdoptions);
router.post("/", createAdoption);

module.exports = router;
