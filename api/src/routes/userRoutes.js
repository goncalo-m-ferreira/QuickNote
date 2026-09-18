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
        displayName: user.display_name ?? null,
        createdAt: user.created_at,
        updatedAt: user.updated_at
      }
    });
  } catch (error) {
    console.error('Error fetching user profile:', error);
    return res.status(500).json({ error: 'Internal server error.' });
  }
});

router.patch('/me', authenticateToken, async (req, res) => {
  try {
    const { displayName } = req.body || {};

    if (displayName === undefined || displayName === null || typeof displayName !== 'string') {
      return res.status(400).json({ error: 'Display name is required and must be a string.' });
    }

    const trimmedName = displayName.trim();

    if (trimmedName.length < 2 || trimmedName.length > 20) {
      return res.status(400).json({ error: 'Display name must be between 2 and 20 characters.' });
    }

    const updatedUser = await UserModel.updateDisplayName(req.user.userId, trimmedName);
    if (!updatedUser) {
      return res.status(404).json({ error: 'User not found.' });
    }

    return res.status(200).json({
      message: 'Profile updated successfully.',
      user: {
        id: updatedUser.id,
        email: updatedUser.email,
        displayName: updatedUser.display_name,
        createdAt: updatedUser.created_at,
        updatedAt: updatedUser.updated_at
      }
    });
  } catch (error) {
    console.error('Error updating user profile:', error);
    return res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
