const express = require("express");
const { signup, login, currentUser } = require("../controllers/authController");
const { authenticate } = require("../middleware/auth");
const router = express.Router();

router.post("/signup", signup);
router.post("/login", login);
router.get("/me", authenticate, currentUser);

module.exports = router;
