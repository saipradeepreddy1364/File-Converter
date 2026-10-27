# File-Converter

A full-stack, enterprise-grade file conversion platform, PDF toolkit, and in-house neural-pattern OCR engine.

---

## 🚀 Features

- **Document & PDF Toolkit**:
  - PDF to Word (DOCX), Excel (XLSX), PowerPoint (PPTX), and Text
  - Office to PDF conversion (DOCX, PPTX, XLSX to PDF)
  - PDF Merge, Split, Reorder, and Rotate
  - PDF Encryption, Decryption, Watermarking, and Optimization/Compression
  - PDF Repair and Metadata Editing
- **In-House OCR Engine**:
  - Image and scanned document text recognition
  - Multi-font neural pattern matching with automatic deskewing and preprocessing
  - Searchable PDF generation and editable DOCX export
  - Evaluation and benchmarking suite
- **Modern Responsive Frontend**:
  - React 19 + TypeScript + Vite + Tailwind CSS
  - Drag-and-drop file upload with conversion status tracking and real-time preview
  - Dark mode and responsive UI

---

## 📁 Project Structure

```
File-Converter/
├── backend/                  # Spring Boot 3.3.4 (Java 21)
│   ├── pom.xml               # Maven configuration & dependencies
│   └── src/
│       ├── main/java/        # Controllers, Services, OCR Engine
│       └── test/java/        # Comprehensive unit & integration tests
├── Frontend/                 # React 19 + TypeScript + Vite
│   ├── package.json          # Dependencies & scripts
│   ├── src/                  # Components, Hooks, Services, Types
│   └── vite.config.ts        # Vite configuration
└── README.md
```

---

## 🛠️ Getting Started

### Prerequisites
- **Java 21+**
- **Maven 3.9+**
- **Node.js 20+** and **npm**

### 1. Backend Setup
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
*Backend runs on `http://localhost:8080`.*

### 2. Frontend Setup
```bash
cd Frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:5173`.*

---

## 🧪 Testing

Run backend test suite:
```bash
cd backend
mvn test
```

Build production bundles:
```bash
# Frontend
cd Frontend
npm run build

# Backend
cd backend
mvn package -DskipTests
```
