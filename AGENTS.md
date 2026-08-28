# AI Agent Guidelines for `study` Project

## Architecture & Overview
- **Project Structure**: Standard Maven project layout.
- **Languages**: Java 8 (`maven.compiler.source` and `target` in `pom.xml`).
- **Core Package**: `org.example`.
- **Main Entry Point**: `org.example.Main`.

## Build & Run
- **Build Tool**: Maven.
- **Commands**:
  - Compile: `mvn clean compile`
  - Package: `mvn clean package`
  - Run: Execute `org.example.Main` directly in IDE or via `java -cp target/study-1.0-SNAPSHOT.jar org.example.Main`.

## Coding Patterns & Guidelines
- Follow basic Java 8 syntax and practices.
- Ensure all source files use `UTF-8` encoding.
- Keep dependencies minimal, managed explicitly within `pom.xml`.

