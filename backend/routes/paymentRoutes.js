const express = require("express");
const { createCheckout } = require("../controllers/paymentController");
const { authenticate } = require("../middleware/auth");
const router = express.Router();

router.post("/checkout", authenticate, createCheckout);

module.exports = router;
