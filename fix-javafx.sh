#!/bin/bash

# Fix JavaFX Issues Script

echo "Fixing JavaFX Issues..."
echo "====================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Step 1: Backup existing files
echo -e "${YELLOW}Step 1: Backing up existing files...${NC}"
mkdir -p backup
cp build.gradle backup/build.gradle.backup 2>/dev/null
cp src/gui/fxml/MainView.fxml backup/MainView.fxml.backup 2>/dev/null
cp src/gui/fxml/VisualizationView.fxml backup/VisualizationView.fxml.backup 2>/dev/null

# Step 2: Create fixed build.gradle
echo -e "${YELLOW}Step 2: Creating fixed build.gradle...${NC}"
cat > build.gradle << 'EOF'
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.0.14'
}

group = 'com.rushhour'
version = '1.0.0'

repositories {
    mavenCentral()
}

javafx {
    version = "17"
    modules = ["javafx.controls", "javafx.fxml"]
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    main {
        java {
            srcDirs = ['src']
        }
        resources {
            srcDirs = ['src']
            exclude '**/*.java'
        }
    }
}

application {
    mainClass = 'cli.Main'
}

// Task untuk menjalankan CLI
task runCli(type: JavaExec) {
    group = 'application'
    description = 'Run the CLI version of Rush Hour'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'cli.Main'
    standardInput = System.in
}

// Task untuk menjalankan GUI dengan JavaFX
task runGui(type: JavaExec) {
    group = 'application'
    description = 'Run the GUI version of Rush Hour with JavaFX'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'gui.GuiMain'
    standardInput = System.in
    
    jvmArgs = [
        '--module-path', classpath.asPath,
        '--add-modules', 'javafx.controls,javafx.fxml'
    ]
}

// Set duplicates strategy for all Copy tasks
tasks.withType(Copy) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Configure JAR with duplicates strategy
jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            'Main-Class': 'cli.Main'
        )
    }
}

// Process resources with duplicates strategy
processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
EOF

# Step 3: Clean and rebuild
echo -e "${YELLOW}Step 3: Cleaning and rebuilding...${NC}"
./gradlew clean
./gradlew build

# Step 4: Test CLI
echo -e "${YELLOW}Step 4: Testing CLI...${NC}"
./gradlew runCli --dry-run

# Step 5: Test GUI
echo -e "${YELLOW}Step 5: Testing GUI...${NC}"
./gradlew runGui --dry-run

echo -e "${GREEN}Setup completed!${NC}"
echo ""
echo "Now you can run:"
echo "  ./gradlew runCli   - To run CLI version"
echo "  ./gradlew runGui   - To run GUI version"
echo ""
echo "If FXML error persists, check the Insets values in your FXML files."
echo "Make sure they use proper format: <Insets bottom=\"20.0\" left=\"20.0\" right=\"20.0\" top=\"20.0\" />"