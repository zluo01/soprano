#!/bin/bash

# Exit script on any error
set -e

# Define paths
FRONTEND_DIR="frontend"
MAVEN_PROJECT_DIR=$(pwd)

echo "Cleanup..."
mvn -q clean

echo "Building web UI package..."

# Navigate to the frontend directory
if [ -d "$FRONTEND_DIR" ]; then
  cd "$FRONTEND_DIR"
  # Install npm dependencies
  echo "Installing npm dependencies..."
  npm install --silent
  # Build the web UI package
  echo "Building the web UI package..."
  npm run build --silent
  # Return to the Maven project directory
  cd "$MAVEN_PROJECT_DIR"
else
  echo "Error: Frontend directory '$FRONTEND_DIR' does not exist."
  exit 1
fi


echo "Building Maven project..."

mvn -q compile

echo "Build completed successfully!"
