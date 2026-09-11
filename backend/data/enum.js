const AllergyEnum = Object.freeze({
    LATTOSIO: { intolerance: "Dairy" },
    GLUTINE: { intolerance: "Gluten" },
    CROSTACEI: { intolerance: "Shellfish" },
    UOVA: { intolerance: "Egg" },
    PESCE: { intolerance: "Seafood" },
    ARACHIDI: { intolerance: "Peanut" },
    SOIA: { intolerance: "Soy" },
    FRUTTA_GUSCIO: { intolerance: "Tree Nut" },
    SESAMO: { intolerance: "Sesame" },
    SOLFITI: { intolerance: "Sulfite" },
    MOLLUSCHI: { intolerance: "Shellfish" },
    SEDANO: { exclude: "celery" },
    SENAPE: { exclude: "mustard" },
    LUPINI: { exclude: "lupin" }
});

const DietEnum = Object.freeze({
    ONNIVORO: "", // Nessun filtro
    VEGETARIANO: "vegetarian",
    VEGANO: "vegan",
    PESCATARIANO: "pescetarian",
    CHETO: "ketogenic",
    PALEO: "paleo"
});

const DietTypeEnum = Object.freeze({
    NESSUNA: "NESSUNA",
    BILANCIATA: "BILANCIATA",
    HIGH_FIBER: "HIGH_FIBER",
    HIGH_PROTEIN: "HIGH_PROTEIN",
    LOW_CARB: "LOW_CARB",
    LOW_FAT: "LOW_FAT"
});

const Diet = Object.freeze({
    BILANCIATA : "25 ,25 , 60",
    HIGH_FIBER: "3g",
    HIGH_PROTEIN: "30",
    LOW_CARB: "10",
    LOW_FAT: "5"
});
const MealCalories = Object.freeze({
    BREAKFAST: { min: 300, max: 500 },
    MAIN_COURSE: { min: 500, max: 800 },
});

function calculateMacroGrams(Cals, percentage, kcalPerGram) {
    return {
        minGrams: Math.round((Cals * percentage) / kcalPerGram)
    };
}

function getSpoonacularFilters(dietType, cal) {
    let filters = {};

    if (!dietType || dietType === DietTypeEnum.NESSUNA) {
        return filters;
    }

    switch (dietType) {
        case DietTypeEnum.BILANCIATA:
            filters.minProtein = calculateMacroGrams(cal, 0.25, 4)
            filters.maxCarbs = calculateMacroGrams(cal, 0.6, 4)
            filters.maxFat = calculateMacroGrams(cal, 0.25, 9)
            break;

        case DietTypeEnum.HIGH_FIBER:
            filters.minFiber = 3;
            break;

        case DietTypeEnum.HIGH_PROTEIN:
            filters.minProtein =  calculateMacroGrams(cal, 0.3, 4)
            break;

        case DietTypeEnum.LOW_CARB:
            filters.maxCarbs =  calculateMacroGrams(cal, 0.3, 4)
            break;

        case DietTypeEnum.LOW_FAT:
            filters.maxFat =  calculateMacroGrams(cal, 0.25, 9)
            break;
    }

    return filters;
}

const TimeEnum = Object.freeze({
    MENO_DI_15: 15,
    TRA_15_E_30: 30,
    TRA_30_E_60: 60,
    MENO_60: 60,
    PIU_DI_60: null
});

const CousineEnum = Object.freeze({
    AFRICANA: "African",
    AMERICANA: "American",
    ASIATICA: "Asian",
    BRITANNICA: "British",
    CAJUN: "Cajun",
    CARAIBICA: "Caribbean",
    CINESE: "Chinese",
    COREANA: "Korean",
    EBRAICA: "Jewish",
    EST_EUROPA: "Eastern European",
    EUROPEA: "European",
    FRANCESE: "French",
    GERMANICA: "German",
    GIAPPONESE: "Japanese",
    GRECA: "Greek",
    INDIANA: "Indian",
    IRLANDESE: "Irish",
    ITALIANA: "Italian",
    LATINO_AMERICANA: "Latin American",
    MEDITERRANEA: "Mediterranean",
    MEDIORIENTALE: "Middle Eastern",
    MESSICANA: "Mexican",
    NORDICA: "Nordic",
    SPAGNOLA: "Spanish",
    STATI_UNITI_SUD: "Southern",
    THAILANDESE: "Thai",
    VIETNAMITA: "Vietnamese"
});

module.exports = {
    AllergyEnum,
    DietEnum,
    DietTypeEnum,
    TimeEnum,
    CousineEnum,
    getSpoonacularFilters
};