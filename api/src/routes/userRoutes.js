const express = require('express');
const authenticateToken = require('../middlewares/authMiddleware');
const UserModel = require('../models/userModel');

const router = express.Router();

router.get('/me', authenticateToken, async (req, res) => {
  try {
    const user = await UserModel.findById(req.user.userId);
    if (!user) {
      return res.status(404).json({ error: 'User not found.' });
    }

    return res.status(200).json({
      user: {
        id: user.id,
        email: user.email,
        createdAt: user.created_at,
        updatedAt: user.updated_at
      }
    });
  } catch (error) {
    console.error('Error fetching user profile:', error);
    return res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
