#!/bin/bash

# Rush Hour Compilation Script

echo "Compiling Rush Hour Project..."

# Create bin directory if it doesn't exist
mkdir -p bin

# Check if JavaFX is available
JAVAFX_PATH=""
if [ -n "$JAVAFX_HOME" ]; then
    JAVAFX_PATH="$JAVAFX_HOME/lib"
elif [ -d "/usr/share/openjfx/lib" ]; then
    JAVAFX_PATH="/usr/share/openjfx/lib"
fi

# Try to compile with JavaFX first (for GUI)
if [ -n "$JAVAFX_PATH" ]; then
    echo "Found JavaFX at: $JAVAFX_PATH"
    echo "Compiling with JavaFX support..."
    
    javac --module-path "$JAVAFX_PATH" \
          --add-modules javafx.controls,javafx.fxml \
          -d bin \
          -cp src \
          src/cli/*.java \
          src/gui/*.java \
          src/gui/controllers/*.java
else
    echo "JavaFX not found. Compiling CLI version only..."
    
    # Compile only CLI files
    javac -d bin \
          -cp src \
          src/cli/*.java
fi

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    
    # Copy resources
    echo "Copying resources..."
    mkdir -p bin/resources
    cp -r src/resources/* bin/resources/ 2>/dev/null || echo "No resources to copy"
    
    # Copy FXML files
    if [ -d "src/gui/fxml" ]; then
        mkdir -p bin/gui/fxml
        cp src/gui/fxml/*.fxml bin/gui/fxml/ 2>/dev/null || echo "No FXML files to copy"
    fi
    
    echo ""
    echo "Build complete! You can now run:"
    echo "  - CLI version: java -cp bin cli.Main"
    
    if [ -n "$JAVAFX_PATH" ]; then
        echo "  - GUI version: java --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.fxml -cp bin gui.GuiMain"
    fi
else
    echo "Compilation failed. Please check the error messages above."
    exit 1
fi