# FYP-v1.0.1
Untuk Present
# Smart Chess Education & Trainer System

## Project Overview
A web-based AI-assisted chess education system using Stockfish engine to help players analyze games, learn best moves, and improve performance.

## Technologies That I Used
- Java Spring Boot
- MySQL
- Stockfish Chess Engine
- HTML / CSS / JavaScript

## Key Features
- Chessboard analysis (Stockfish suggested moves)
- PGN upload & analysis
- Player & coach management
- Puzzle training module

## System Architecture
- MVC Architecture
- RESTful API (PostMan)

## Project Structure

The backend follows a Spring MVC layout:

- `controller/` - REST endpoints only. Controllers validate access, map requests, and delegate work.
- `service/` - Business logic such as puzzle import, upload processing, Stockfish, and analysis workflows.
- `service/vision/` - Integration boundary for external computer vision tooling such as YOLO/OpenCV.
- `repository/` - Spring Data JPA database access.
- `model/` - JPA entities mapped to database tables.
- `dto/` - Request/response objects used by the API. Feature-specific DTOs live in subpackages such as `dto/puzzle` and `dto/vision`.
- `security/` - JWT and Spring Security configuration.
- `config/` - Application startup/configuration classes.

Frontend assets are kept under `src/main/resources/static/`.

Computer vision tooling is intentionally kept outside the Java source tree:

- `tools/vision/` - Python scripts and Python dependency file for YOLO/OpenCV processing.
- `docs/` - Workflow documentation, including the CVAT + YOLO11 labeling/training process.

Large generated files such as trained model weights, YOLO runs, datasets, and local logs are ignored by Git.

## How to Run
1. Clone repository
2. Configure database
3. Run Spring Boot application

## 👤 Author
- Muhammad Syazrin Amzar bin Mohd Zaimi (S65748)
- SMSK (KP)
