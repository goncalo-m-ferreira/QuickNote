const db = require('../db');

const NoteModel = {
  async findAllByUser(userId) {
    const result = await db.query(
      `SELECT id, user_id, title, content, created_at, updated_at
       FROM notes
       WHERE user_id = $1
       ORDER BY updated_at DESC`,
      [userId]
    );
    return result.rows;
  },

  async findById(id) {
    const result = await db.query(
      `SELECT id, user_id, title, content, created_at, updated_at
       FROM notes
       WHERE id = $1`,
      [id]
    );
    return result.rows[0] || null;
  },

  async create(userId, title, content = '') {
    const result = await db.query(
      `INSERT INTO notes (user_id, title, content)
       VALUES ($1, $2, $3)
       RETURNING id, user_id, title, content, created_at, updated_at`,
      [userId, title.trim(), content]
    );
    return result.rows[0];
  },

  async update(id, userId, title, content) {
    const result = await db.query(
      `UPDATE notes
       SET title = $1, content = $2, updated_at = CURRENT_TIMESTAMP
       WHERE id = $3 AND user_id = $4
       RETURNING id, user_id, title, content, created_at, updated_at`,
      [title.trim(), content, id, userId]
    );
    return result.rows[0] || null;
  },

  async delete(id, userId) {
    const result = await db.query(
      `DELETE FROM notes
       WHERE id = $1 AND user_id = $2
       RETURNING id`,
      [id, userId]
    );
    return result.rows[0] || null;
  },

  async updatePhoto(id, photoBuffer, mimeType) {
    const result = await db.query(
      `UPDATE notes
       SET photo = $1, photo_mime_type = $2, updated_at = CURRENT_TIMESTAMP
       WHERE id = $3
       RETURNING id, user_id, title, content, created_at, updated_at`,
      [photoBuffer, mimeType, id]
    );
    return result.rows[0] || null;
  },

  async getPhoto(id) {
    const result = await db.query(
      `SELECT photo, photo_mime_type, user_id
       FROM notes
       WHERE id = $1`,
      [id]
    );
    return result.rows[0] || null;
  },

  async deletePhoto(id) {
    const result = await db.query(
      `UPDATE notes
       SET photo = NULL, photo_mime_type = NULL, updated_at = CURRENT_TIMESTAMP
       WHERE id = $1
       RETURNING id, user_id, title, content, created_at, updated_at`,
      [id]
    );
    return result.rows[0] || null;
  }
};

module.exports = NoteModel;
