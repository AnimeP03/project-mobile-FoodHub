CREATE DATABASE kitchenos;
USE kitchenos;

-- Tabella Utenti
CREATE TABLE Users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella Preferenze 

CREATE TABLE Preferences (
    user_id INT PRIMARY KEY,
    dietary_regime VARCHAR(50),
    diet_type JSON, 
    time_preparation VARCHAR(50),
    allergies JSON,
    cousine JSON,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);


-- Tabella WeekPlan
CREATE TABLE meal_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    spoonacularId BIGINT,
    image VARCHAR(512),
    title VARCHAR(255),
    readyInMinutes INT,
    servings INT,
    vegetarian BOOLEAN,
    vegan BOOLEAN,
    glutenFree BOOLEAN,
    dairyFree BOOLEAN,
    summary TEXT,
    spoonacularScore DOUBLE,
    
    extendedIngredients JSON,
    nutrition JSON,
    cuisines JSON,
    dishTypes JSON,
    analyzedInstructions JSON,
    
    dayOfWeek VARCHAR(50),
    mealType VARCHAR(50),

    fetchedTimestamp BIGINT,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Tabella Ricette Custom
CREATE TABLE custom_meals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userid VARCHAR(255) NOT NULL,
    spoonacularId BIGINT DEFAULT NULL,
    image VARCHAR(1000) DEFAULT NULL,
    title VARCHAR(255) NOT NULL,
    readyInMinutes INT NOT NULL,
    servings INT NOT NULL,
    summary TEXT,
    spoonacularScore DOUBLE DEFAULT NULL,
    
    extendedIngredients JSON,
    nutrition JSON,
    cuisines JSON,
    dishTypes JSON,
    analyzedInstructions JSON
);