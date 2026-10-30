const { pool } = require("../config/db");
const { sendNotification } = require("../services/emailService");

async function createAdoption(req, res, next) {
  const petId = Number(req.body.petId);
  const notes = req.body.notes;
  if (!Number.isInteger(petId) || petId < 1 || typeof notes !== "string" || notes.trim().length < 10 || notes.length > 1000) {
    return res.status(400).json({ message: "Choose a pet and add a note between 10 and 1000 characters." });
  }

  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const { rows: pets } = await client.query(
      "SELECT id, name, available FROM pets WHERE id = $1 FOR UPDATE",
      [petId],
    );
    if (!pets[0]) {
      await client.query("ROLLBACK");
      return res.status(404).json({ message: "Pet not found." });
    }
    if (!pets[0].available) {
      await client.query("ROLLBACK");
      return res.status(409).json({ message: "This pet has already found a home." });
    }
    const { rows } = await client.query(
      "INSERT INTO adoptions (user_id, pet_id, notes) VALUES ($1, $2, $3) RETURNING id, pet_id, status, created_at",
      [req.user.id, petId, notes.trim()],
    );
    await client.query("COMMIT");
    try {
      await sendNotification({
        to: req.user.email,
        subject: `Adoption application received for ${pets[0].name}`,
        text: `Hi ${req.user.name},\n\nWe received your adoption application for ${pets[0].name}. You can track your journey from your dashboard.\n\nThe Paws & Homes team`,
      });
    } catch (emailError) {
      console.error("Adoption application email delivery failed:", emailError);
    }
    return res.status(201).json({ adoption: rows[0] });
  } catch (error) {
    await client.query("ROLLBACK");
    return next(error);
  } finally {
    client.release();
  }
}

async function listMyAdoptions(req, res, next) {
  try {
    const { rows } = await pool.query(
      `SELECT a.id, a.status, a.created_at, p.name AS pet_name, p.image_url
       FROM adoptions a JOIN pets p ON p.id = a.pet_id
       WHERE a.user_id = $1 ORDER BY a.created_at DESC`,
      [req.user.id],
    );
    return res.json({ adoptions: rows });
  } catch (error) {
    return next(error);
  }
}

module.exports = { createAdoption, listMyAdoptions };
