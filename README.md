# UNO

A web-based implementation of the classic UNO card game built with Java, Spring Boot, and Vaadin.

## Overview

UNO is a browser-based card game application that recreates the gameplay of the popular UNO card game. The application leverages:

- Java 21
- Spring Boot
- Vaadin
- Maven
- Docker

The project is designed as a modern web application with a responsive user interface and a scalable backend architecture.

---

## Features

- Interactive web-based UNO gameplay
- Modern UI built with Vaadin
- Spring Boot backend services
- Maven-based build and dependency management
- Docker support for containerized deployment
- Automated testing support

---

## Technology Stack

| Technology | Purpose |
|------------|----------|
| Java 21 | Core application development |
| Spring Boot | Backend framework |
| Vaadin | Frontend UI framework |
| Maven | Build and dependency management |
| Docker | Containerization |

---

## Project Structure

```text
UNO/
├── src/
│   ├── main/
│   └── test/
├── resources/
├── pom.xml
├── Dockerfile
├── mvnw
└── .gitignore
```

---

## Prerequisites

Before running the application, ensure you have:

- Java 21 or higher
- Maven 3.9+
- Docker (optional)

---

## Getting Started

### Option 1
#### Click [here](https://uno-djye.onrender.com/) to play! (You may have to wait a few minutes for the app to wind up)

### Option 2
#### Clone the Repository

```bash
git clone https://github.com/bgupta13/UNO.git
cd UNO
```

#### Run with Maven Wrapper

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```cmd
mvnw.cmd spring-boot:run
```

#### Run with Maven

```bash
mvn spring-boot:run
```

---

### Building the Application

Create a production build:

```bash
mvn clean package
```

The generated JAR file will be located in:

```text
target/
```

Run the packaged application:

```bash
java -jar target/*.jar
```

---

### Running Tests

Execute the test suite:

```bash
mvn test
```

---

### Docker Deployment

Build the Docker image:

```bash
docker build -t uno .
```

Run the container:

```bash
docker run -p 8080:8080 uno
```

Access the application at:

```text
http://localhost:8080
```

---

## Development

### Hot Reload

Spring Boot DevTools is included for faster local development.

Run:

```bash
mvn spring-boot:run
```

and changes will automatically reload during development.

---

## Future Enhancements

- Multiplayer support
- Online matchmaking
- User authentication
- Score tracking and leaderboards
- AI opponents
- Mobile-friendly experience
- Game statistics dashboard

---

## Contributing

1. Fork the repository
2. Create a feature branch

```bash
git checkout -b feature/my-feature
```

3. Commit your changes

```bash
git commit -m "Add new feature"
```

4. Push to your branch

```bash
git push origin feature/my-feature
```

5. Open a Pull Request

---

## License

This project is licensed under the MIT License unless otherwise specified.

---

## Author

**Bhav Gupta**

GitHub: https://github.com/bgupta13
