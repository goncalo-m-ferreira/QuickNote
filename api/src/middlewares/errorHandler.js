function errorHandler(err, req, res, next) {
  // Handle invalid JSON body syntax errors from express.json()
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    return res.status(400).json({ error: 'Malformed JSON payload in request body.' });
  }

  // Handle multer file upload errors (e.g. file size limit exceeded)
  if (err && (err.name === 'MulterError' || err.code === 'LIMIT_FILE_SIZE')) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(413).json({ error: 'Photo exceeds the maximum allowed size of 5MB.' });
    }
    return res.status(400).json({ error: 'Invalid photo upload.' });
  }

  console.error('Unhandled Error:', err);

  // Handle custom application errors with defined status codes
  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal server error.';

  return res.status(statusCode).json({ error: message });
}

module.exports = errorHandler;
