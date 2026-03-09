/**
 * Database connection pool for Trace HQ.
 * Loads credentials from .env (trace_hq/.env). Never log or expose DB_PASSWORD.
 * @see markdown/database/DATABASE.md
 */

require('dotenv').config({ path: require('path').resolve(__dirname, '..', '.env') });
const mysql = require('mysql2/promise');

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME || 'trace-hq',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  connectTimeout: 15000, // 15s for cold MySQL / slow startup
});

module.exports = { pool };
