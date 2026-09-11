const express = require('express');
const NoteController = require('../controllers/noteController');
const authenticateToken = require('../middlewares/authMiddleware');

const router = express.Router();

router.use(authenticateToken);

router.get('/', NoteController.getAll);
router.get('/:id', NoteController.getById);
router.post('/', NoteController.create);
router.put('/:id', NoteController.update);
router.delete('/:id', NoteController.delete);

module.exports = router;
