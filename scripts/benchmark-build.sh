#!/bin/bash

# Build Performance Benchmark Script
# Measures build times with different configurations

echo "=== Hilt Build Performance Benchmark ==="

# Clean build
echo "Running clean build..."
./gradlew clean
CLEAN_START=$(date +%s%N)
./gradlew assembleDebug
CLEAN_END=$(date +%s%N)
CLEAN_TIME=$(( (CLEAN_END - CLEAN_START) / 1000000 ))

echo "Clean build time: ${CLEAN_TIME}ms"

# Incremental build (no changes)
echo "Running incremental build (no changes)..."
INCREMENTAL_START=$(date +%s%N)
./gradlew assembleDebug
INCREMENTAL_END=$(date +%s%N)
INCREMENTAL_TIME=$(( (INCREMENTAL_END - INCREMENTAL_START) / 1000000 ))

echo "Incremental build time: ${INCREMENTAL_TIME}ms"

# Generate report
echo "=== Build Performance Report ===" > build-performance-report.txt
echo "Clean build: ${CLEAN_TIME}ms" >> build-performance-report.txt
echo "Incremental build: ${INCREMENTAL_TIME}ms" >> build-performance-report.txt
echo "Improvement: $(( CLEAN_TIME - INCREMENTAL_TIME ))ms" >> build-performance-report.txt

echo "Benchmark complete. Report saved to build-performance-report.txt"