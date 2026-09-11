const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const UserModel = require('../models/userModel');

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const AuthController = {
  async register(req, res) {
    try {
      const { email, password } = req.body;

      if (!email || !password) {
        return res.status(400).json({ error: 'Email and password are required.' });
      }

      const trimmedEmail = email.trim().toLowerCase();

      if (!EMAIL_REGEX.test(trimmedEmail)) {
        return res.status(400).json({ error: 'Invalid email format.' });
      }

      if (password.length < 6) {
        return res.status(400).json({ error: 'Password must be at least 6 characters long.' });
      }

      const existingUser = await UserModel.findByEmail(trimmedEmail);
      if (existingUser) {
        return res.status(409).json({ error: 'Email is already registered.' });
      }

      const saltRounds = 10;
      const passwordHash = await bcrypt.hash(password, saltRounds);

      const newUser = await UserModel.create(trimmedEmail, passwordHash);

      const jwtSecret = process.env.JWT_SECRET || 'fallback_jwt_secret';
      const token = jwt.sign(
        { userId: newUser.id, email: newUser.email },
        jwtSecret,
        { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
      );

      return res.status(201).json({
        message: 'User registered successfully.',
        user: {
          id: newUser.id,
          email: newUser.email,
          createdAt: newUser.created_at
        },
        token
      });
    } catch (error) {
      console.error('Registration error:', error);
      return res.status(500).json({ error: 'Internal server error during registration.' });
    }
  },

  async login(req, res) {
    try {
      const { email, password } = req.body;

      if (!email || !password) {
        return res.status(400).json({ error: 'Email and password are required.' });
      }

      const trimmedEmail = email.trim().toLowerCase();

      const user = await UserModel.findByEmail(trimmedEmail);
      if (!user) {
        return res.status(401).json({ error: 'Invalid email or password.' });
      }

      const isPasswordValid = await bcrypt.compare(password, user.password_hash);
      if (!isPasswordValid) {
        return res.status(401).json({ error: 'Invalid email or password.' });
      }

      const jwtSecret = process.env.JWT_SECRET || 'fallback_jwt_secret';
      const token = jwt.sign(
        { userId: user.id, email: user.email },
        jwtSecret,
        { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
      );

      return res.status(200).json({
        message: 'Login successful.',
        user: {
          id: user.id,
          email: user.email,
          createdAt: user.created_at
        },
        token
      });
    } catch (error) {
      console.error('Login error:', error);
      return res.status(500).json({ error: 'Internal server error during login.' });
    }
  },

  async logout(req, res) {
    try {
      // In a stateless JWT architecture, the server acknowledges token termination
      // and instructs the client to purge stored tokens.
      return res.status(200).json({
        message: 'Logout successful. Token invalidated on client.'
      });
    } catch (error) {
      console.error('Logout error:', error);
      return res.status(500).json({ error: 'Internal server error during logout.' });
    }
  }
};

module.exports = AuthController;
