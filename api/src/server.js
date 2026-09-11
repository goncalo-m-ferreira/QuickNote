const express = require('express');
const cors = require('cors');
require('dotenv').config();
const { initDb } = require('./db');
const authRoutes = require('./routes/authRoutes');
const userRoutes = require('./routes/userRoutes');
const noteRoutes = require('./routes/noteRoutes');
const errorHandler = require('./middlewares/errorHandler');

const app = express();
const PORT = process.env.PORT || 3000;

// Base middleware
app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: 'QuickNote API',
    timestamp: new Date().toISOString()
  });
});

// Application routes
app.use('/auth', authRoutes);
app.use('/users', userRoutes);
app.use('/notes', noteRoutes);

// 404 Fallback for unmatched routes
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found.' });
});

// Centralized error handling middleware
app.use(errorHandler);

// Server bootstrap with database initialization
async function startServer() {
  try {
    const requiredEnvVars = ['DATABASE_URL', 'JWT_SECRET'];
    const missingEnvVars = requiredEnvVars.filter(
      (name) => !process.env[name] || process.env[name].trim() === ''
    );

    if (missingEnvVars.length > 0) {
      throw new Error(
        `Missing required environment variables: ${missingEnvVars.join(', ')}`
      );
    }

    await initDb();

    app.listen(PORT, () => {
      console.log(`QuickNote API running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

startServer();
