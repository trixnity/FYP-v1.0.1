# FYP-v1.0.1
# Smart Chess Education & Trainer System

## Project Overview
Smart Chess Education & Trainer System is a web-based chess learning platform that helps students practice puzzles, analyze positions, and receive training support from coaches. The system uses Spring Boot, MySQL, Stockfish, and computer vision tools to support chess education workflows.

## Problem Statement
Chess students often rely on separate tools for puzzle practice, game analysis, and coach feedback. This project combines those workflows into one system, including puzzle management, analysis, student tracking, and image/PDF puzzle conversion.

## Objectives
- Provide an interactive chess puzzle training module.
- Allow coaches to create, upload, and manage puzzles.
- Analyze chess positions using Stockfish.
- Support image/PDF puzzle processing using YOLO/OpenCV.
- Organize students, coaches, assignments, sessions, and progress data in one platform.

## Technologies Used
- Java Spring Boot
- Spring MVC
- Spring Security with JWT
- Spring Data JPA
- MySQL
- Stockfish Chess Engine
- HTML / CSS / JavaScript
- Python
- YOLO11 / Ultralytics
- OpenCV
- CVAT for dataset annotation

## Key Features
- User authentication and role-based access
- Student, coach, and admin management
- Interactive chessboard analysis
- Stockfish best move and evaluation support
- Puzzle catalog and puzzle solving
- Coach puzzle builder
- Image/PDF puzzle upload workflow
- YOLO/OpenCV-based chess piece detection pipeline
- CVAT dataset labeling workflow documentation

## System Architecture
This project follows MVC architecture:

- **Model**: JPA entities in `model/`
- **View**: Static frontend files in `src/main/resources/static/`
- **Controller**: REST API endpoints in `controller/`
- **Service**: Business logic in `service/`
- **Repository**: Database access in `repository/`
- **DTO**: API request/response objects in `dto/`

## Project Structure

```text
src/main/java/com/fyp/fypsystem/
  config/          Application startup/configuration
  controller/      REST API controllers
  dto/             API request and response records
  model/           JPA entities
  repository/      Spring Data JPA repositories
  security/        JWT and Spring Security configuration
  service/         Business logic
  service/vision/  YOLO/OpenCV integration boundary

src/main/resources/
  application.properties
  static/          Frontend HTML, CSS, JS, images, and chess assets

tools/vision/
  chess_vision_to_fen.py
  requirements.txt

docs/
  cvat-yolo11-chess-workflow.md