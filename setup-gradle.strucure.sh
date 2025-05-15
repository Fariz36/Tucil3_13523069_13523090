#!/bin/bash

# Setup Gradle folder structure

echo "Setting up Gradle folder structure..."

# Create gradle wrapper directory
mkdir -p gradle/wrapper

# Create gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-7.6-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

echo "Gradle folder structure created!"
echo ""
echo "Next steps:"
echo "1. Run ./init-gradle.sh to generate wrapper files"
echo "2. Run ./gradlew build to build the project"
echo "3. Run ./gradlew runCli to run CLI version"
echo "4. Run ./gradlew runGui to run GUI version"