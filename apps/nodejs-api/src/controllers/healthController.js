// Health check controller
const healthCheck = (req, res) => {
  res.json({
    success: true,
    message: 'API is running',
    timestamp: new Date().toISOString(),
  });
};

module.exports = {
  healthCheck,
};
