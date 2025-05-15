#!/bin/bash

# Rush Hour Runner Script

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed.${NC}"
    echo "Please install Java 11 or higher."
    exit 1
fi

# Check if compiled classes exist
if [ ! -d "bin" ] || [ -z "$(ls -A bin)" ]; then
    echo -e "${YELLOW}Compiled classes not found. Compiling...${NC}"
    ./compile.sh
    if [ $? -ne 0 ]; then
        echo -e "${RED}Compilation failed. Exiting.${NC}"
        exit 1
    fi
fi

# Display menu
echo -e "${GREEN}Rush Hour Puzzle Solver${NC}"
echo "======================="
echo "1. Run GUI Version"
echo "2. Run CLI Version"
echo "3. Recompile"
echo "4. Exit"
echo
read -p "Choose an option (1-4): " choice

case $choice in
    1)
        echo -e "${GREEN}Starting GUI...${NC}"
        if command -v gradle &> /dev/null; then
            gradle runGui
        else
            # Try to run with JavaFX if available
            java -cp bin gui.GuiMain 2>/dev/null || {
                echo -e "${YELLOW}JavaFX not found in classpath.${NC}"
                echo "Trying with JavaFX modules..."
                read -p "Enter path to JavaFX lib directory: " FX_PATH
                java --module-path "$FX_PATH" --add-modules javafx.controls,javafx.fxml -cp bin gui.GuiMain
            }
        fi
        ;;
    2)
        echo -e "${GREEN}Starting CLI...${NC}"
        java -cp bin cli.Main
        ;;
    3)
        echo -e "${YELLOW}Recompiling...${NC}"
        rm -rf bin/*
        ./compile.sh
        ;;
    4)
        echo -e "${GREEN}Goodbye!${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid option. Please choose 1-4.${NC}"
        exit 1
        ;;
esac