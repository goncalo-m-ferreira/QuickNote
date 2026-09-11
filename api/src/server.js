const express = require('express');
const cors = require('cors');
require('dotenv').config();
const { initDb } = require('./db');
const authRoutes = require('./routes/authRoutes');
const userRoutes = require('./routes/userRoutes');
const noteRoutes = require('./routes/noteRoutes');

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

// Routes
app.use('/auth', authRoutes);
app.use('/users', userRoutes);
app.use('/notes', noteRoutes);

// Fallback route for unhandled endpoints
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Server bootstrap with database initialization
async function startServer() {
  try {
    if (process.env.DATABASE_URL) {
      await initDb();
    } else {
      console.warn('DATABASE_URL not found in environment. Skipping database initialization.');
    }

    app.listen(PORT, () => {
      console.log(`QuickNote API running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

startServer();
