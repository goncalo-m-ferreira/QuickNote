const express = require('express');
const multer = require('multer');
const NoteController = require('../controllers/noteController');
const authenticateToken = require('../middlewares/authMiddleware');

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }
});

const router = express.Router();

router.use(authenticateToken);

router.get('/', NoteController.getAll);
router.get('/:id', NoteController.getById);
router.post('/', NoteController.create);
router.put('/:id', NoteController.update);
router.delete('/:id', NoteController.delete);

// Photo endpoints
router.put('/:id/photo', upload.single('photo'), NoteController.uploadPhoto);
router.get('/:id/photo', NoteController.getPhoto);
router.delete('/:id/photo', NoteController.deletePhoto);

module.exports = router;
