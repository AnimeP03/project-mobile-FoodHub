const axios = require('axios')
const path = require('path')
const fs = require('fs').promises
const express = require('express')
const pool = require('../db')
const dotenv = require('dotenv')
const authenticateToken = require('./jwt')

dotenv.config()
const api = process.env.SPOONACULAR_API
const {AllergyEnum,DietEnum,DietTypeEnum,TimeEnum,CousineEnum, getSpoonacularFilters} = require('../data/enum')

const router = express.Router()

async function getUserPreferences(userId){
    const id = userId

    const [rows] = await pool.query( 
            `SELECT 
                dietary_regime, 
                diet_type, 
                time_preparation, 
                allergies, 
                cousine
            FROM Preferences 
            WHERE user_id = ?`, [userId]);

    const prefs = rows[0];
    return prefs;
}

function mapToLightWeightMeal(recipe) {
    return {
        spoonacularId: recipe.id, 
        image: recipe.image || "",
        title: recipe.title || "Senza Titolo",
        readyInMinutes: recipe.readyInMinutes || 0,
        servings: recipe.servings || 0,
        vegetarian: recipe.vegetarian || false,
        vegan: recipe.vegan || false,
        glutenFree: recipe.glutenFree || false,
        dairyFree: recipe.dairyFree || false,
        summary: recipe.summary || "",
        spoonacularScore: recipe.spoonacularScore || recipe.healthScore || 0.0,

        extendedIngredients: (recipe.extendedIngredients || []).map(ing => ({
            id: ing.id || 0,
            image: ing.image || null,
            originalName: ing.originalName || "",
            nameClean: ing.nameClean || ing.name || "",
            measures: {
                amount: ing.measures?.metric?.amount || 0.0,
                unitShort: ing.measures?.metric?.unitShort || "",
                unitLong: ing.measures?.metric?.unitLong || ""
            }
        })),

        nutrition: {
            nutrients: (recipe.nutrition?.nutrients || []).map(n => ({
                name: n.name || "",
                amount: n.amount || 0.0,
                unit: n.unit || ""
            })),
            caloricBreakdown: {
                percentProtein: recipe.nutrition?.caloricBreakdown?.percentProtein || 0.0,
                percentFat: recipe.nutrition?.caloricBreakdown?.percentFat || 0.0,
                percentCarbs: recipe.nutrition?.caloricBreakdown?.percentCarbs || 0.0
            }
        },

        cuisines: recipe.cuisines || [],
        dishTypes: recipe.dishTypes || [],

        analyzedInstructions: (recipe.analyzedInstructions || []).map(instruction => ({
            steps: (instruction.steps || []).map(s => ({
                number: s.number || 0,
                step: s.step || ""
            }))
        }))
    };
}

router.post('/testGenerate', authenticateToken, async (req, res) => {
    try {
        console.log("testGenerate")
        const filePath = path.join(__dirname, '../../mock_data.json'); 
        const fileContent = await fs.readFile(filePath, 'utf-8');
        const bigJsonData = JSON.parse(fileContent);

        const recipesArray = bigJsonData.results
        const meals = recipesArray.map(mapToLightWeightMeal);

        res.status(200).json(meals);

    } catch (err) {
        console.error("Errore durante la generazione del piano:", err);
        res.status(500).json({ err: "Errore interno del server" });
    }
});

router.post('/generate', authenticateToken, async (req, res) => {
    console.log("generate")
    try {
        const userId = req.user.userid;
        const userPref = await getUserPreferences(userId)


        const type = req.query.type;
        const count = parseInt(req.query.count);

        const prefs = userPref || { 
            dietary_regime: "ONNIVORO", 
            diet_type: [], 
            allergies: [], 
            time_preparation: "PIU_DI_60", 
            cousine: [] 
        };

        const params = {
            apiKey: api,
            type: type,
            number: count,
            addRecipeInformation: true,
            addRecipeNutrition: true,
            addRecipeInstructions: true,
            instructionsRequired: true,
            fillIngredients: true,
            sort: "random"
        };

        const calorieTarget = (type === "breakfast") ? 300: 500;

        if (Array.isArray(prefs.diet_type) && prefs.diet_type.length > 0) {
            let mergedMacroFilters = {};

            prefs.diet_type.forEach(selectedType => {
                const cleanType = selectedType.toUpperCase().trim();
                const macroFilters = getSpoonacularFilters(cleanType, calorieTarget);
    
                Object.assign(mergedMacroFilters, macroFilters);
            });

            Object.assign(params, mergedMacroFilters);
        }

        if (Array.isArray(prefs.allergies) && prefs.allergies.length > 0) {
            const intolerancesList = [];
            const excludeIngredientsList = [];

            prefs.allergies.forEach(allergy => {
                const cleanKey = allergy.toUpperCase().trim();
                const mappedAllergy = AllergyEnum[cleanKey];

                if (mappedAllergy) {
                    if (mappedAllergy.intolerance) {
                        intolerancesList.push(mappedAllergy.intolerance);
                    }
                    if (mappedAllergy.exclude) {
                        excludeIngredientsList.push(mappedAllergy.exclude);
                    }
                }
            });

            if (intolerancesList.length > 0) params.intolerances = intolerancesList.join(',');
            if (excludeIngredientsList.length > 0) params.excludeIngredients = excludeIngredientsList.join(',');
        }


        if (prefs.dietary_regime && DietEnum[prefs.dietary_regime.toUpperCase()]) {
            const diet = DietEnum[prefs.dietary_regime.toUpperCase()];
            params.diet = diet
        }

        if (prefs.time_preparation && TimeEnum[prefs.time_preparation]) {
            const maxMinutes = TimeEnum[prefs.time_preparation];
            if (maxMinutes) params.maxReadyTime = maxMinutes;
        }

        if (Array.isArray(prefs.cousine) && prefs.cousine.length > 0) {
            params.cuisine = prefs.cousine.map(c => c.toLowerCase().trim()).join(',');
        }

        console.log("Parametri inviati a Spoonacular:", params);
        const url = 'https://api.spoonacular.com/recipes/complexSearch';
        const urlCompletto = axios.getUri({
            url: url,
            params: params
        })
        console.log(urlCompletto)

        const response = await axios.get(url, { params });

        const quotaRequest = response.headers['x-api-quota-request'];
        const quotaUsed = response.headers['x-api-quota-used'];
        const quotaLeft = response.headers['x-api-quota-left'];

        const recipesArray = response.data.results || [];
        const meals = recipesArray.map(mapToLightWeightMeal);

        res.set('X-API-Quota-Request', quotaRequest);
        res.set('X-API-Quota-Used', quotaUsed);
        res.set('X-API-Quota-Left', quotaLeft);

        res.status(200).json(meals);

    } catch (err) {
        console.error("Errore durante la generazione del piano:", err);
        res.status(500).json({ err: "Errore interno del server" });
    }
});

router.get("/current", authenticateToken, async(req,res) => {
    console.log("Get current")
    try {
        const userId = req.user.userid;

        const [rows] = await pool.query(
            'SELECT * FROM meal_entity WHERE user_id = ?', 
            [userId]
        );

        const mealEntities = rows.map(row => ({
            id: row.id,
            userid: userId,

            spoonacularId: row.spoonacularId,
            image: row.image,
            title: row.title,
            readyInMinutes: row.readyInMinutes,
            servings: row.servings,

            vegetarian: Boolean(row.vegetarian),
            vegan: Boolean(row.vegan),
            glutenFree: Boolean(row.glutenFree),
            dairyFree: Boolean(row.dairyFree),

            summary: row.summary,
            spoonacularScore: row.spoonacularScore,

            extendedIngredients: row.extendedIngredients,
            nutrition: row.nutrition,
            cuisines: row.cuisines,
            dishTypes: row.dishTypes,
            analyzedInstructions: row.analyzedInstructions,

            dayOfWeek: row.dayOfWeek,
            mealType: row.mealType,

            fetchedTimestamp: row.fetchedTimestamp
        }));


        res.status(200).json(mealEntities);

    } catch (error) {
        console.error("Errore durante il recupero del piano:", error);
        res.status(500).json({ message: "Errore interno del server" });
    }
});

router.post("/current", authenticateToken, async(req,res) => {
    console.log("Post current")
    try {
        const userId = req.user.userid; 

        const meals = req.body;

        if (!Array.isArray(meals) || meals.length === 0) {
            return res.status(400).json({ 
                success: false, 
                message: "Nessun pasto ricevuto o formato non valido." 
            });
        }

        await pool.query(`DELETE FROM meal_entity WHERE user_id = ?`, [userId]);

        const valuesToInsert = meals.map(meal => [
            userId,
            meal.spoonacularId,
            meal.image,
            meal.title,
            meal.readyInMinutes,
            meal.servings,
            meal.vegetarian ? 1 : 0,
            meal.vegan ? 1 : 0,
            meal.glutenFree ? 1 : 0,
            meal.dairyFree ? 1 : 0,
            meal.summary,
            meal.spoonacularScore,
            
            JSON.stringify(meal.extendedIngredients || []),
            JSON.stringify(meal.nutrition || {}),
            JSON.stringify(meal.cuisines || []),
            JSON.stringify(meal.dishTypes || []),
            JSON.stringify(meal.analyzedInstructions || []),
            
            meal.dayOfWeek,
            meal.mealType,

            meal.fetchedTimestamp || Date.now()
        ]);

        const sql = `
            INSERT INTO meal_entity (
                user_id, spoonacularId, image, title, readyInMinutes, servings, 
                vegetarian, vegan, glutenFree, dairyFree, summary, spoonacularScore, 
                extendedIngredients, nutrition, cuisines, dishTypes, analyzedInstructions, 
                dayOfWeek, mealType, fetchedTimestamp
            ) VALUES ?
        `;

        const [result] = await pool.query(sql, [valuesToInsert]);


        res.status(201).json({success: true, message: 'WeekPlan saved succesfully' + userId });
    } catch (err) {
        console.error("Errore durante l'inserimento del piano:", err);
        res.status(500).json({ err: "Errore interno del server" });
    }
});


module.exports = router;
