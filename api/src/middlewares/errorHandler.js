function errorHandler(err, req, res, next) {
  console.error('Unhandled Error:', err);

  // Handle invalid JSON body syntax errors from express.json()
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    return res.status(400).json({ error: 'Malformed JSON payload in request body.' });
  }

  // Handle custom application errors with defined status codes
  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal server error.';

  return res.status(statusCode).json({ error: message });
}

module.exports = errorHandler;
