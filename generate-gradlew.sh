#!/bin/bash

# Generate Gradle Wrapper Script

echo "Generating Gradle Wrapper for Rush Hour Project..."

# Check if gradle is installed
if ! command -v gradle &> /dev/null; then
    echo "Error: Gradle is not installed."
    echo "Please install Gradle first: https://gradle.org/install/"
    exit 1
fi

# Create gradle wrapper properties directory
mkdir -p gradle/wrapper

# Create settings.gradle
cat > settings.gradle << 'EOF'
rootProject.name = 'rush-hour-solver'
EOF

# Create gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-7.6-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# Generate gradle wrapper
gradle wrapper --gradle-version 7.6

if [ $? -eq 0 ]; then
    echo "Gradle wrapper generated successfully!"
    echo ""
    echo "Now you can use:"
    echo "  ./gradlew build    - to build the project"
    echo "  ./gradlew runCli   - to run CLI version"
    echo "  ./gradlew runGui   - to run GUI version"
    echo ""
    echo "On Windows use gradlew.bat instead of ./gradlew"
else
    echo "Failed to generate gradle wrapper"
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

echo "Setup complete!"