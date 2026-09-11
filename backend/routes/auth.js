const express = require('express')
const bcrypt = require('bcrypt')
const pool = require('../db')
const dotenv = require('dotenv')
const jwt = require('jsonwebtoken')

dotenv.config()
const jwt_secret = process.env.JWT_SECRET



const router = express.Router()

router.get('/ping', (req, res) => {
    res.status(200).json({ 
        message: 'pong',
        timestamp: new Date().toISOString() 
    });
});

router.post('/register', async (req, res) => {
    const { email, username, password } = req.body
    if (!email || !password || !username) return res.status(400).json({ error: 'Data Required' })

    if (!email.includes('@')) return res.status(400).json({ error: 'Email error' });

    if (password.length < 7) return res.status(400).json({ error: 'Password error' });

    if (username.length < 3) return res.status(400).json({ error: 'Username error' });

    try {
        const [rows] = await pool.query('SELECT id FROM users WHERE email = ?', [email])
        if (rows.length) return res.status(409).json({ error: 'Email already in use' })

        const hash = await bcrypt.hash(password, 10)
        const [result] = await pool.query('INSERT INTO users (email, password, username) VALUES (?, ?, ?)', [email, hash, username])
        const [userRows] = await pool.query('SELECT id, email, username FROM users WHERE id = ?', [result.insertId])

        const token = jwt.sign({ userid: userRows[0].id }, jwt_secret, { expiresIn: '7d' })

        res.status(201).json({token: token , message: 'Registration Successful', user: userRows[0] })
    } catch (err) {
        console.error(err)
        res.status(500).json({ error: 'Database Error' })
    }
});

router.post('/login', async (req, res) => {
    const { email, password } = req.body
    /*const credentials = { email: "test@test", password: "test" };
    const { email, password } = credentials;*/
    if (!email || !password) return res.status(400).json({ error: 'Data required' })

    try {
        const [rows] = await pool.query('SELECT id, email, username, password, created_at FROM users WHERE email = ?', [email])
        if (!rows.length) return res.status(401).json({ error: 'Invalid Credentials' })

        const ok = await bcrypt.compare(password, rows[0].password)
        if (!ok) return res.status(401).json({ error: 'Invalid Credentials' })

        const token = jwt.sign({ userid: rows[0].id }, jwt_secret, { expiresIn: '7d' })
        
        const { password: _, ...userWithoutPassword } = rows[0]
        res.status(201).json({token: token ,message: 'Login Successful', user: userWithoutPassword })
    } catch (err) {
        console.error(err)
        res.status(500).json({ error: 'Database Error' })
    }
});

module.exports = router

