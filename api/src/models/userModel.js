const db = require('../db');

const UserModel = {
  async findByEmail(email) {
    const result = await db.query(
      'SELECT id, email, password_hash, created_at, updated_at FROM users WHERE email = $1',
      [email.toLowerCase().trim()]
    );
    return result.rows[0] || null;
  },

  async findById(id) {
    const result = await db.query(
      'SELECT id, email, created_at, updated_at FROM users WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  },

  async create(email, passwordHash) {
    const result = await db.query(
      `INSERT INTO users (email, password_hash)
       VALUES ($1, $2)
       RETURNING id, email, created_at, updated_at`,
      [email.toLowerCase().trim(), passwordHash]
    );
    return result.rows[0];
  }
};

module.exports = UserModel;
