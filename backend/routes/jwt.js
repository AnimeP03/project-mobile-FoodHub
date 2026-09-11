const jwt = require('jsonwebtoken')
const dotenv = require('dotenv')

dotenv.config()

const jwt_secret = process.env.JWT_SECRET

const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];


    if (!token) {
        return res.status(401).json({ error: 'Accesso negato. Token mancante.' });
    }

    jwt.verify(token, jwt_secret, (err, user) => {
        if (err) {
            return res.status(403).json({ error: 'Token non valido o scaduto.' });
        }
        req.user = user; 
        next(); 
    });
};

module.exports = authenticateToken;