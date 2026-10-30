const { pool } = require("../config/db");

async function findAvailablePets({ species, search, limit }) {
  const values = [];
  const filters = ["available = TRUE"];
  if (species && ["dog", "cat"].includes(String(species).toLowerCase())) {
    values.push(String(species).toLowerCase());
    filters.push(`LOWER(species) = $${values.length}`);
  }
  if (search && String(search).trim()) {
    values.push(`%${String(search).trim().slice(0, 100)}%`);
    filters.push(`(name ILIKE $${values.length} OR breed ILIKE $${values.length} OR location ILIKE $${values.length})`);
  }
  let query = `SELECT * FROM pets WHERE ${filters.join(" AND ")} ORDER BY created_at DESC`;
  if (limit && Number.isInteger(Number(limit)) && Number(limit) > 0) {
    values.push(Math.min(Number(limit), 50));
    query += ` LIMIT $${values.length}`;
  }
  const { rows } = await pool.query(query, values);
  return rows;
}

async function findPetById(id) {
  const { rows } = await pool.query("SELECT * FROM pets WHERE id = $1", [id]);
  return rows[0] || null;
}

module.exports = { findAvailablePets, findPetById };
