const NoteModel = require('../models/noteModel');

const NoteController = {
  async getAll(req, res) {
    try {
      const notes = await NoteModel.findAllByUser(req.user.userId);
      return res.status(200).json({ notes });
    } catch (error) {
      console.error('Error fetching notes:', error);
      return res.status(500).json({ error: 'Internal server error fetching notes.' });
    }
  },

  async getById(req, res) {
    try {
      const noteId = parseInt(req.params.id, 10);
      if (isNaN(noteId)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }

      const note = await NoteModel.findById(noteId);
      if (!note) {
        return res.status(404).json({ error: 'Note not found.' });
      }

      // Authorization check: Verify resource owner
      if (note.user_id !== req.user.userId) {
        return res.status(403).json({ error: 'Forbidden: You do not have permission to access this note.' });
      }

      return res.status(200).json({ note });
    } catch (error) {
      console.error('Error fetching note:', error);
      return res.status(500).json({ error: 'Internal server error fetching note.' });
    }
  },

  async create(req, res) {
    try {
      const { title, content } = req.body;

      if (!title || typeof title !== 'string' || title.trim() === '') {
        return res.status(400).json({ error: 'Note title is required.' });
      }

      const newNote = await NoteModel.create(
        req.user.userId,
        title,
        content !== undefined && content !== null ? String(content) : ''
      );

      return res.status(201).json({
        message: 'Note created successfully.',
        note: newNote
      });
    } catch (error) {
      console.error('Error creating note:', error);
      return res.status(500).json({ error: 'Internal server error creating note.' });
    }
  },

  async update(req, res) {
    try {
      const noteId = parseInt(req.params.id, 10);
      if (isNaN(noteId)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }

      const note = await NoteModel.findById(noteId);
      if (!note) {
        return res.status(404).json({ error: 'Note not found.' });
      }

      // Authorization check: Verify resource owner
      if (note.user_id !== req.user.userId) {
        return res.status(403).json({ error: 'Forbidden: You do not have permission to modify this note.' });
      }

      const { title, content } = req.body;
      if (!title || typeof title !== 'string' || title.trim() === '') {
        return res.status(400).json({ error: 'Note title is required.' });
      }

      const updatedNote = await NoteModel.update(
        noteId,
        req.user.userId,
        title,
        content !== undefined && content !== null ? String(content) : ''
      );

      return res.status(200).json({
        message: 'Note updated successfully.',
        note: updatedNote
      });
    } catch (error) {
      console.error('Error updating note:', error);
      return res.status(500).json({ error: 'Internal server error updating note.' });
    }
  },

  async delete(req, res) {
    try {
      const noteId = parseInt(req.params.id, 10);
      if (isNaN(noteId)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }

      const note = await NoteModel.findById(noteId);
      if (!note) {
        return res.status(404).json({ error: 'Note not found.' });
      }

      // Authorization check: Verify resource owner
      if (note.user_id !== req.user.userId) {
        return res.status(403).json({ error: 'Forbidden: You do not have permission to delete this note.' });
      }

      await NoteModel.delete(noteId, req.user.userId);
      return res.status(200).json({ message: 'Note deleted successfully.' });
    } catch (error) {
      console.error('Error deleting note:', error);
      return res.status(500).json({ error: 'Internal server error deleting note.' });
    }
  }
};

module.exports = NoteController;
