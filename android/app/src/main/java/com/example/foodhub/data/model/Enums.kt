package com.example.foodhub.data.model

enum class IngredientStatus {
    IN_USE,
    UNUSED,
    TO_BUY
}

data class PantryUiItem(
    val id: Long,
    val name: String,
    val image: String?,
    val amount: Float,
    val unit: String,
    val status: IngredientStatus
)


enum class Allergies(
    val uiLabel: String,
) {
    LATTOSIO("Lattosio (Intolleranza)"),
    GLUTINE("Glutine / Celiachia"),
    CROSTACEI("Crostacei"),
    UOVA("Uova"),
    PESCE("Pesce"),
    ARACHIDI("Arachidi"),
    SOIA("Soia"),
    FRUTTA_GUSCIO("Frutta a guscio"),
    SESAMO("Semi di sesamo"),
    SOLFITI("Solfiti"),
    MOLLUSCHI("Molluschi"),
    SEDANO("Sedano"),
    SENAPE("Senape"),
    LUPINI("Lupini");
}

enum class DietaryRegime(
    val uiLabel: String,
) {
    ONNIVORO("Onnivoro (Nessuna restrizione)"),
    VEGETARIANO("Vegetariano"),
    VEGANO("Vegano"),
    PESCATARIANO("Pescatariano"),
    CHETOGENICO("Chetogenico (Keto)"),
    PALEO("Paleo");
}

enum class DietType(val uiLabel: String, val desc: String) {
    NESSUNA("Nessuna", ""),
    BILANCIATA("Bilanciata", "15% Grassi, 35% Proteine, 50% Carbo" ),
    HIGH_FIBER("High-Fiber", "Oltre 5g a porzione per il benessere intestinale"),
    HIGH_PROTEIN("High-Protein", "Più del 50% delle calorie da fonti proteiche"),
    LOW_CARB("Low-Carb", "Sotto il 20% di carboidrati per pasto"),
    LOW_FAT("Low-Fat", "Meno del 15% di grassi");
}

enum class TimePreparation(
    val uiLabel: String,
) {
    MENO_DI_15("Meno di 15 minuti"),
    TRA_15_E_30("15 - 30 minuti"),
    TRA_30_E_60("30 - 60 minuti"),
    PIU_DI_60("Più di 1 ora");
}

enum class Cousine(
    val uiLabel: String,
) {
    AFRICANA("Africana"),
    AMERICANA("Americana"),
    ASIATICA("Asiatica"),
    BRITANNICA("Britannica"),
    CAJUN("Cajun"),
    CARAIBICA("Caraibica"),
    CINESE("Cinese"),
    COREANA("Coreana"),
    EBRAICA("Ebraica"),
    EST_EUROPA("Est Europa"),
    EUROPEA("Europea"),
    FRANCESE("Francese"),
    GERMANICA("Tedesca"),
    GIAPPONESE("Giapponese"),
    GRECA("Greca"),
    INDIANA("Indiana"),
    IRLANDESE("Irlandese"),
    ITALIANA("Italiana"),
    LATINO_AMERICANA("Latino-Americana"),
    MEDITERRANEA("Mediterranea"),
    MEDIORIENTALE("Mediorientale"),
    MESSICANA("Messicana"),
    NORDICA("Nordica"),
    SPAGNOLA("Spagnola"),
    STATI_UNITI_SUD("Stati Uniti del Sud"),
    THAILANDESE("Thailandese"),
    VIETNAMITA("Vietnamita");
}