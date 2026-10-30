require("dotenv").config();
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const { initializeDatabase, pool } = require("./config/db");
const { handleWebhook } = require("./controllers/paymentController");
const authRoutes = require("./routes/authRoutes");
const petRoutes = require("./routes/petRoutes");
const adoptionRoutes = require("./routes/adoptionRoutes");
const paymentRoutes = require("./routes/paymentRoutes");

for (const variable of ["DATABASE_URL", "JWT_SECRET"]) {
  if (!process.env[variable]) throw new Error(`${variable} must be configured.`);
}
if (process.env.JWT_SECRET.length < 32) throw new Error("JWT_SECRET must be at least 32 characters long.");

const app = express();
app.disable("x-powered-by");
app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || "http://localhost:5173" }));
app.use("/api/payments/webhook", express.raw({ type: "application/json" }), handleWebhook);
app.use(express.json({ limit: "20kb" }));

app.get("/api/health", (_req, res) => res.json({ status: "ok" }));
app.use("/api/auth", rateLimit({ windowMs: 15 * 60 * 1000, limit: 30 }), authRoutes);
app.use("/api/pets", petRoutes);
app.use("/api/adoptions", adoptionRoutes);
app.use("/api/payments", paymentRoutes);

app.use((req, res) => res.status(404).json({ message: "Route not found." }));
app.use((error, _req, res, _next) => {
  console.error("API request failed:", error);
  res.status(error.status || 500).json({
    message: error.status ? error.message : "An unexpected server error occurred.",
  });
});

const port = Number(process.env.PORT || 5000);
initializeDatabase()
  .then(() => app.listen(port, "0.0.0.0", () => console.info(`Paws & Homes API listening on port ${port}`)))
  .catch(async (error) => {
    console.error("Could not initialize PostgreSQL:", error);
    await pool.end();
    process.exitCode = 1;
  });
