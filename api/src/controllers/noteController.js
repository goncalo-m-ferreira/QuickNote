const NoteModel = require('../models/noteModel');

const NOTE_ID_REGEX = /^[1-9]\d*$/;

function isValidNoteId(idParam) {
  return typeof idParam === 'string' && NOTE_ID_REGEX.test(idParam);
}

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
      if (!isValidNoteId(req.params.id)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }
      const noteId = parseInt(req.params.id, 10);

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

      if (title.trim().length > 255) {
        return res.status(400).json({ error: 'Note title must not exceed 255 characters.' });
      }

      if (!content || typeof content !== 'string' || content.trim() === '') {
        return res.status(400).json({ error: 'Note content is required.' });
      }

      const newNote = await NoteModel.create(
        req.user.userId,
        title.trim(),
        content.trim()
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
      if (!isValidNoteId(req.params.id)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }
      const noteId = parseInt(req.params.id, 10);

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

      if (title.trim().length > 255) {
        return res.status(400).json({ error: 'Note title must not exceed 255 characters.' });
      }

      if (!content || typeof content !== 'string' || content.trim() === '') {
        return res.status(400).json({ error: 'Note content is required.' });
      }

      const updatedNote = await NoteModel.update(
        noteId,
        req.user.userId,
        title.trim(),
        content.trim()
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
      if (!isValidNoteId(req.params.id)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }
      const noteId = parseInt(req.params.id, 10);

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
  },

  async uploadPhoto(req, res) {
    try {
      if (!isValidNoteId(req.params.id)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }
      const noteId = parseInt(req.params.id, 10);

      if (!req.file) {
        return res.status(400).json({ error: 'No photo file provided.' });
      }

      const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/webp'];
      if (!allowedMimeTypes.includes(req.file.mimetype)) {
        return res.status(400).json({ error: 'Invalid file type. Only JPEG, PNG and WebP are allowed.' });
      }

      const note = await NoteModel.findById(noteId);
      if (!note) {
        return res.status(404).json({ error: 'Note not found.' });
      }

      if (note.user_id !== req.user.userId) {
        return res.status(403).json({ error: 'Forbidden: You do not have permission to modify this note.' });
      }

      await NoteModel.updatePhoto(noteId, req.file.buffer, req.file.mimetype);

      return res.status(200).json({ message: 'Photo uploaded successfully.' });
    } catch (error) {
      console.error('Error uploading photo:', error);
      return res.status(500).json({ error: 'Internal server error uploading photo.' });
    }
  },

  async getPhoto(req, res) {
    try {
      if (!isValidNoteId(req.params.id)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }
      const noteId = parseInt(req.params.id, 10);

      const note = await NoteModel.getPhoto(noteId);
      if (!note) {
        return res.status(404).json({ error: 'Note not found.' });
      }

      if (note.user_id !== req.user.userId) {
        return res.status(403).json({ error: 'Forbidden: You do not have permission to access this note.' });
      }

      if (!note.photo) {
        return res.status(404).json({ error: 'No photo found for this note.' });
      }

      res.type(note.photo_mime_type).send(note.photo);
    } catch (error) {
      console.error('Error fetching photo:', error);
      return res.status(500).json({ error: 'Internal server error fetching photo.' });
    }
  },

  async deletePhoto(req, res) {
    try {
      if (!isValidNoteId(req.params.id)) {
        return res.status(400).json({ error: 'Invalid note ID format.' });
      }
      const noteId = parseInt(req.params.id, 10);

      const note = await NoteModel.findById(noteId);
      if (!note) {
        return res.status(404).json({ error: 'Note not found.' });
      }

      if (note.user_id !== req.user.userId) {
        return res.status(403).json({ error: 'Forbidden: You do not have permission to delete this note.' });
      }

      await NoteModel.deletePhoto(noteId);

      return res.status(200).json({ message: 'Photo deleted successfully.' });
    } catch (error) {
      console.error('Error deleting photo:', error);
      return res.status(500).json({ error: 'Internal server error deleting photo.' });
    }
  }
};

module.exports = NoteController;
