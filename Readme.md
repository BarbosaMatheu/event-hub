# 🎟️ EventHub API

> A modern RESTful API built with **Java 21** and **Spring Boot 3** designed for managing events, categories, participant registrations, and ticket availability tracking.

---

## 📌 About the Project

**EventHub API** is a backend application developed to handle event management and user booking workflows. The project adheres to software engineering best practices, emphasizing **Clean Code**, **Layered Architecture (Controller-Service-Repository)**, **DTO patterns**, and automatic schema generation via JPA/Hibernate.

---

## 🚀 Tech Stack

* **Language:** Java 21 (LTS)
* **Framework:** Spring Boot 3
* **Web / REST:** Spring Web (MVC)
* **Data Access:** Spring Data JPA / Hibernate
* **Database:** PostgreSQL
* **Validation:** Jakarta Bean Validation
* **Build Tool:** Maven

---

## ✨ Key Features

* 📅 **Event Management:** Full CRUD operations for events (title, description, date/time, venue, maximum capacity, price).
* 🗂️ **Category Organization:** Group events into categories (e.g., *Tech*, *Workshops*, *Concerts*, *Meetups*).
* 👥 **Participant Management:** Register attendees with validated fields (Email, Name, ID).
* 🎟️ **Registration & Booking:** Handle user registrations with automated capacity checks and duplicate booking protection.
* ⚠️ **Global Exception Handling:** Custom exception handling (`@ControllerAdvice`) providing clear HTTP responses (`400 Bad Request`, `404 Not Found`).

---

## 🛠️ Getting Started

### Prerequisites

* **Java JDK 21** or higher
* **PostgreSQL** installed and running
* **Maven** (or an IDE like IntelliJ IDEA)

---

### ⚙️ Database Configuration

Configure your PostgreSQL credentials in `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/eventhub_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### Running the Application

1. Clone the repository:

'git clone [https://github.com/NopsyNight/event-hub.git](https://github.com/NopsyNight/event-hub.git)] '

2. Navigate to the project directory:

cd event-hub

3. Run The Application:

./mvnw spring-boot:run

4. Acess the API:
   The server will start at http://localhost:8080.