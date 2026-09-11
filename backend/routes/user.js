const express = require('express')
const pool = require('../db')
const dotenv = require('dotenv')
const authenticateToken = require('./jwt')
const axios = require('axios')

dotenv.config()

const router = express.Router()

router.get('/preferences', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.userid;

        const [rows] = await pool.query( 
            `SELECT 
                dietary_regime, 
                diet_type, 
                time_preparation, 
                allergies, 
                cousine
            FROM Preferences 
            WHERE user_id = ?`, [userId]);

        if (rows.length === 0) {
        return res.status(404).json({ 
            success: false, 
            message: "Preferenze non ancora impostate per questo utente." 
        });
        }
        const prefs = rows[0];
        const responseData = {
            dietaryRegime: prefs.dietary_regime || "",
            allergies: prefs.allergies || [],
            dietType: prefs.diet_type || [],
            timePreparation: prefs.time_preparation || "",
            cousine: prefs.cousine || []
        };

        console.log(responseData)
        res.status(201).json(responseData);
        
    } catch (err){
        console.error(err)
        res.status(500).json({ success: false, message: 'Database Error' })
    }
});


router.post('/preferences', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.userid; 
        
        const { 
            dietaryRegime, 
            dietType,       
            timePreparation, 
            allergies, 
            cousine 
        } = req.body;

        const query = `
            INSERT INTO Preferences (
                user_id, 
                dietary_regime, 
                diet_type, 
                time_preparation, 
                allergies, 
                cousine
            ) 
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                dietary_regime = VALUES(dietary_regime), 
                diet_type = VALUES(diet_type), 
                time_preparation = VALUES(time_preparation), 
                allergies = VALUES(allergies),
                cousine = VALUES(cousine)
        `;

        await pool.query(query, [
            userId, 
            dietaryRegime || 'ONNIVORO', 
            JSON.stringify(dietType || []),
            timePreparation || 'PIU_DI_60',
            JSON.stringify(allergies || []), 
            JSON.stringify(cousine || [])
        ]);

        res.status(201).json({success: true, message: 'Preferences saved succesfully' + userId 
        });

    } catch (err) {
        console.error(err)
        res.status(500).json({ success: false, message: 'Database Error' })
    }
});

router.post('/verify', authenticateToken, async (req, res) => {

    const { barcode } = req.body;
    const userId = req.user.userid; 


    try {
        const offUrl = `https://world.openfoodfacts.org/api/v2/product/${barcode}?fields=code,product_name,generic_name,quantity,categories,categories_tags`;
        const offResponse = await axios.get(offUrl, {
            headers: { 'User-Agent': 'KitchenOS/1.0 (Node.js Backend)' }
        });

        console.log(barcode)

        if (offResponse.data.status !== 1 || !offResponse.data.product) {
            return res.status(404).json({ 
                success: false, 
                message: "Product doesnt exist in db (openfoodfacts)" 
            });
        }

        const product = offResponse.data.product;
        let searchTerm = "";

        if (product.categories) {
            searchTerm = product.categories;
        } else if (product.product_name) {
            searchTerm = product.product_name;
        }

        if (!searchTerm) {
            return res.status(404).json({ 
                success: false, 
                message: "Product doesnt exist in db(openfoodfacts)" 
            });
        }

        const SPOONACULAR_API_KEY = process.env.SPOONACULAR_API; 
        const spoonUrl = `https://api.spoonacular.com/recipes/parseIngredients?apiKey=${SPOONACULAR_API_KEY}`;

        const formData = new URLSearchParams();
        formData.append('ingredientList', searchTerm);

        const spoonResponse = await axios.post(spoonUrl, formData, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });

        console.log("found spoonacular")

        const quotaRequest = spoonResponse.headers['x-api-quota-request'];
        const quotaUsed = spoonResponse.headers['x-api-quota-used'];
        const quotaLeft = spoonResponse.headers['x-api-quota-left'];

        if (quotaRequest) res.set('X-API-Quota-Request', quotaRequest);
        if (quotaUsed) res.set('X-API-Quota-Used', quotaUsed);
        if (quotaLeft) res.set('X-API-Quota-Left', quotaLeft);

        const parsedResults = spoonResponse.data;
        if (!parsedResults || parsedResults.length === 0 || !parsedResults[0].id) {
            console.log("differenti")
            return res.status(404).json({ 
                success: false, 
                message: "Product db != api" 
            });
        }

        const ingredientData = parsedResults[0];


        let rawQuantity = product.quantity ? String(product.quantity) : "";
        let cleanQuantity = rawQuantity.replace(',', '.').replace(/[^0-9.]/g, '');
        let parsedAmount = parseFloat(cleanQuantity);
        if (isNaN(parsedAmount)) {
            parsedAmount = 0.0;
        }

        const possibleUnits = ingredientData.possibleUnits || [];
        const preferredUnits = ['g', 'ml', 'pz'];
        const foundUnit = possibleUnits.find(u => preferredUnits.includes(u.toLowerCase()));

        let finalUnit = '';

        if (foundUnit) {
            const lowerUnit = foundUnit.toLowerCase();  
            finalUnit = foundUnit;
        } else {
            finalUnit = ingredientData.unit;
        }


        const name = ingredientData.image
        const formattedName = name.split('.')[0].replaceAll("-", " ");

        return res.status(200).json({
            id: ingredientData.id,
            userid: userId,
            originalName: formattedName,
            image: ingredientData.image,
            amountInStock: parsedAmount ,
            unit: finalUnit,
        });

    } catch (err) {
        console.error(err);
        return res.status(500).json({ success: false, message: "Database Error" });
    }


});


router.post('/getCustom', authenticateToken, async(req,res)=> {
    try {
        console.log("gETRecipe")
        const userId = req.user.userid;

        const [rows] = await pool.query('SELECT * FROM custom_meals WHERE userid = ?', [userId]);
        
        const recipes = rows.map(row => ({
            id: row.id,
            userid: row.userid,
            spoonacularId: row.spoonacularId,
            image: row.image,
            title: row.title,
            readyInMinutes: row.readyInMinutes,
            servings: row.servings,
            summary: row.summary,
            spoonacularScore: row.spoonacularScore,
            extendedIngredients: row.extendedIngredients || [],
            nutrition: row.nutrition || null,
            cuisines: row.cuisines || [],
            dishTypes: row.dishTypes || [],
            analyzedInstructions: row.analyzedInstructions || []
        }));

        res.status(200).json(recipes);
    } catch (error) {
        console.error("Errore MySQL Get:", error);
        res.status(500).json([]);
    }
});

router.post('/addCustom', authenticateToken, async(req,res)=> {
    try {
        console.log("ADDRecipe")
        const recipe = req.body;
        const userId = req.user.userid;

        const query = `
            INSERT INTO custom_meals 
            (id, userid, spoonacularId, image, title, readyInMinutes, servings, summary, spoonacularScore, extendedIngredients, nutrition, cuisines, dishTypes, analyzedInstructions) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `;

        const values = [
            recipe.id, 
            userId,
            recipe.spoonacularId || null,
            recipe.image || null,
            recipe.title,
            recipe.readyInMinutes,
            recipe.servings,
            recipe.summary || '',
            recipe.spoonacularScore || null,
            JSON.stringify(recipe.extendedIngredients || []),
            JSON.stringify(recipe.nutrition || null),
            JSON.stringify(recipe.cuisines || []),
            JSON.stringify(recipe.dishTypes || []),
            JSON.stringify(recipe.analyzedInstructions || [])
        ];

        const [result] = await pool.query(query, values);

        res.status(200).json({ success: true, message: "Succesful " });
    } catch (error) {
        console.error("Errore MySQL Add:", error);
        res.status(500).json({ success: false, message: "Errore interno." });
    }
});

router.post('/deleteCustom', authenticateToken, async(req,res)=> {
    try {
        console.log("DeleteRecipe")
        const { id } = req.body;
        const userId = req.user.userid;

        const [result] = await pool.query(
            'DELETE FROM custom_meals WHERE id = ? AND userid = ?', 
            [id, userId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: "NOT FOUND" });
        }

        res.status(200).json({ success: true, message: "Succesful eliminated" });
    } catch (error) {
        console.error("Errore MySQL Delete:", error);
        res.status(500).json({ success: false, message: "Errore durante l'eliminazione." });
    }
});

module.exports = router;