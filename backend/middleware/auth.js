const jwt = require("jsonwebtoken");
const { pool } = require("../config/db");

async function authenticate(req, res, next) {
  const authorization = req.headers.authorization;
  if (!authorization || !authorization.startsWith("Bearer ")) {
    return res.status(401).json({ message: "Please sign in to continue." });
  }
  try {
    const payload = jwt.verify(authorization.slice(7), process.env.JWT_SECRET);
    const { rows } = await pool.query(
      "SELECT id, name, email, role FROM users WHERE id = $1",
      [payload.sub],
    );
    if (!rows[0]) return res.status(401).json({ message: "Your session is no longer valid." });
    req.user = rows[0];
    next();
  } catch (error) {
    if (error.name === "JsonWebTokenError" || error.name === "TokenExpiredError") {
      return res.status(401).json({ message: "Your session is invalid or has expired." });
    }
    next(error);
  }
}

module.exports = { authenticate };
