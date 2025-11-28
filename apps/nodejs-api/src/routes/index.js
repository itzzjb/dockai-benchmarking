const express = require('express');
const router = express.Router();

const healthRoutes = require('./healthRoutes');
const userRoutes = require('./userRoutes');

router.use('/health', healthRoutes);
router.use('/users', userRoutes);

module.exports = router;
