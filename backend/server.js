const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
dotenv.config();

const app = express();


const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/user');
const planRoutes = require('./routes/plan');

app.use(cors())
app.use(express.json({ limit: '50mb' }));

app.use('/auth', authRoutes);
app.use('/user', userRoutes);
app.use('/plan', planRoutes)

const port = process.env.PORT || 3000;
app.listen(port, () => {
    console.log(`Server in esecuzione sulla porta ${port}`);
});