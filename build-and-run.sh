#!/bin/bash

# Script to build and run the DS Project 04 application
# This script assumes all required files are in the same directory as this script

set -e

cd "$(dirname "$0")"

echo "Building Docker image..."
docker build -t ds-project-04 .

echo "Starting container (port 8080). Stop with: docker rm -f ds-project-04-container"
docker rm -f ds-project-04-container 2>/dev/null || true
docker run -d -p 8080:8080 --name ds-project-04-container ds-project-04

echo "Done. Container 'ds-project-04-container' is running on http://localhost:8080/"
echo "Set MONGODB_URI when running docker if you do not use the default in MongoHolder.java, e.g.:"
echo "  docker run -d -p 8080:8080 -e MONGODB_URI='...' --name ds-project-04-container ds-project-04"
