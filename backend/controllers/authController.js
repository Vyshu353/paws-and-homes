const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { pool } = require("../config/db");
const { sendNotification } = require("../services/emailService");

function createToken(user) {
  return jwt.sign({ sub: user.id }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || "7d",
  });
}

async function signup(req, res, next) {
  const { name, email, password } = req.body;
  if (typeof name !== "string" || name.trim().length < 2 || name.trim().length > 100) {
    return res.status(400).json({ message: "Name must be between 2 and 100 characters." });
  }
  if (typeof email !== "string" || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ message: "Enter a valid email address." });
  }
  if (typeof password !== "string" || password.length < 8 || password.length > 72) {
    return res.status(400).json({ message: "Password must be between 8 and 72 characters." });
  }

  try {
    const passwordHash = await bcrypt.hash(password, 12);
    const { rows } = await pool.query(
      "INSERT INTO users (name, email, password_hash) VALUES ($1, $2, $3) RETURNING id, name, email, role",
      [name.trim(), email.trim().toLowerCase(), passwordHash],
    );
    const user = rows[0];
    try {
      await sendNotification({
        to: user.email,
        subject: "Welcome to Paws & Homes",
        text: `Hi ${user.name},\n\nWelcome to Paws & Homes! We're so glad you're here. Your new best friend might be just around the corner.\n\nThe Paws & Homes team`,
      });
    } catch (emailError) {
      console.error("Welcome email delivery failed:", emailError);
    }
    return res.status(201).json({ user, token: createToken(user) });
  } catch (error) {
    if (error.code === "23505") return res.status(409).json({ message: "An account with this email already exists." });
    return next(error);
  }
}

async function login(req, res, next) {
  const { email, password } = req.body;
  if (typeof email !== "string" || typeof password !== "string") {
    return res.status(400).json({ message: "Email and password are required." });
  }
  try {
    const { rows } = await pool.query(
      "SELECT id, name, email, role, password_hash FROM users WHERE email = $1",
      [email.trim().toLowerCase()],
    );
    const user = rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      return res.status(401).json({ message: "Email or password is incorrect." });
    }
    delete user.password_hash;
    return res.json({ user, token: createToken(user) });
  } catch (error) {
    return next(error);
  }
}

function currentUser(req, res) {
  return res.json({ user: req.user });
}

module.exports = { signup, login, currentUser };
