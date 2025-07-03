# 3D Secure System

A high-performance Java-based system for storing and retrieving 3D Secure card range data with fast PAN lookups.

## 📋 Requirements

- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0+
- Redis 8.0

## 🛠️ Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/Mino521/3D-Secure.git
cd 3D-Secure
```

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run Tests

```bash
mvn test
```

### 4. Start the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 5. Access API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/api-docs`

## 📖 API Usage

### 1. Lookup Card Range by PAN

```bash
curl "http://localhost:8080/api/v1/3d-secure/lookup?pan=4000020000000010"
```

**Response:**
```json
{
  "startRange": 4000020000000000,
  "endRange": 4000020009999999,
  "actionInd": "A",
  "acsEndProtocolVersion": "2.1.0",
  "threeDSMethodURL": "https://secure4.arcot.com/content-server/api/tds2/txn/browser/v1/tds-method",
  "acsStartProtocolVersion": "2.1.0",
  "acsInfoInd": [
    "01",
    "02"
  ]
}
```

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.