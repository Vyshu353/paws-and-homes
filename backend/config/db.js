const { Pool } = require("pg");

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSL === "true" ? { rejectUnauthorized: true } : undefined,
});

pool.on("error", (error) => {
  console.error("Unexpected PostgreSQL pool error:", error);
});

async function initializeDatabase() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id SERIAL PRIMARY KEY,
      name VARCHAR(100) NOT NULL,
      email VARCHAR(255) NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      role VARCHAR(20) NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin')),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS pets (
      id SERIAL PRIMARY KEY,
      name VARCHAR(100) NOT NULL,
      species VARCHAR(40) NOT NULL,
      breed VARCHAR(100) NOT NULL,
      age INTEGER NOT NULL CHECK (age >= 0),
      gender VARCHAR(20) NOT NULL,
      location VARCHAR(160) NOT NULL,
      image_url TEXT NOT NULL,
      description TEXT NOT NULL DEFAULT '',
      adoption_fee NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (adoption_fee >= 0),
      available BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS adoptions (
      id SERIAL PRIMARY KEY,
      user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      pet_id INTEGER NOT NULL REFERENCES pets(id),
      notes TEXT NOT NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'paid', 'approved', 'rejected')),
      stripe_session_id TEXT UNIQUE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS idx_pets_available ON pets(available);
    CREATE INDEX IF NOT EXISTS idx_adoptions_user_id ON adoptions(user_id);
  `);

  const { rows } = await pool.query("SELECT COUNT(*)::int AS count FROM pets");
  if (rows[0].count === 0) {
    await pool.query(
      `INSERT INTO pets (name, species, breed, age, gender, location, image_url, description, adoption_fee)
       VALUES
       ($1, 'Dog', 'Golden retriever', 2, 'Male', 'Brooklyn, NY', $2, $3, 150),
       ($4, 'Cat', 'Tabby cat', 1, 'Female', 'Queens, NY', $5, $6, 95),
       ($7, 'Dog', 'Corgi mix', 3, 'Male', 'Jersey City, NJ', $8, $9, 125),
       ($10, 'Dog', 'Labrador mix', 1, 'Female', 'Brooklyn, NY', $11, $12, 125),
       ($13, 'Cat', 'Calico', 2, 'Female', 'Queens, NY', $14, $15, 95),
       ($16, 'Dog', 'Beagle mix', 4, 'Male', 'Hoboken, NJ', $17, $18, 110)`,
      [
        "Milo", "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=900&q=85", "Milo is a gentle, sunny-natured companion who loves long walks and a cozy spot by your side.",
        "Olive", "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=900&q=85", "Olive is curious, affectionate, and always ready to turn an ordinary afternoon into a purr-fect one.",
        "Biscuit", "https://images.unsplash.com/photo-1612536057832-2ff7ead58194?auto=format&fit=crop&w=900&q=85", "Biscuit is a cheerful little adventurer with a big personality and an even bigger heart.",
        "Poppy", "https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=900&q=85", "Poppy is a playful sweetheart who would love a family to call her own.",
        "Cleo", "https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=900&q=85", "Cleo is a calm, cuddly friend who makes every sunny windowsill better.",
        "Charlie", "https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?auto=format&fit=crop&w=900&q=85", "Charlie is a loyal buddy who loves sniffy walks, treats, and being part of the team.",
      ],
    );
  }
}

module.exports = { pool, initializeDatabase };
