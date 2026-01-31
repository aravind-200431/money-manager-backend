# Money Manager – Backend

Spring Boot REST API for the Money Manager web application. It handles personal and business financial transactions, dashboard stats, category summaries, and filtering with persistence in MongoDB Atlas.

---

## What We Built

- **Transactions API**
  - Create income and expense transactions (with optional source/target account). (Transfer type exists in the API but is not used by the current UI.)
  - List transactions with pagination (`/transactions?page=0&size=10`).
  - Update a transaction (allowed only within **12 hours** of creation).
  - Filter by date range, category, and division (`/transactions/filter`).

- **Dashboard & Summary**
  - Dashboard stats by period: **weekly**, **monthly**, **yearly** (`/dashboard/{period}`).
  - Category summary (expense/income by category) for the selected period (`/summary/categories?period=monthly`).

- **Business Rules**
  - Edit allowed only within 12 hours; after that the API returns an error.
  - Validation on request body (type, amount, category, division, description, date).

- **Data Model**
  - Transaction: type (INCOME, EXPENSE; TRANSFER supported in API but not in UI), amount, category, division, description, date, optional `sourceAccount` and `targetAccount` for account tracking.

---

## Tech Stack

| Purpose        | Technology                    |
|----------------|-------------------------------|
| Language       | Java 17                       |
| Framework      | Spring Boot 3.2.5             |
| API            | Spring Web (REST)             |
| Database       | MongoDB Atlas                 |
| ODM            | Spring Data MongoDB           |
| Validation     | Bean Validation (Jakarta)      |
| Build          | Maven                         |
| Utilities      | Lombok                        |

---

## Prerequisites

- **Java 17** (or compatible JDK)
- **Maven** (or use the included wrapper `mvnw` / `mvnw.cmd`)
- **MongoDB Atlas** cluster (or any MongoDB 4.x+)

---

## How to Run

1. **Clone / open the project**
   ```bash
   cd money-manager-backend
   ```

2. **Configure MongoDB**
   - Set your MongoDB connection string in `src/main/resources/application.properties`:
     ```properties
     spring.data.mongodb.uri=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/<dbname>?retryWrites=true&w=majority
     ```
   - Or use an environment variable and reference it in `application.properties` if you prefer not to commit credentials.

3. **Build and run**
   ```bash
   # Windows
   mvnw.cmd spring-boot:run

   # Linux / macOS
   ./mvnw spring-boot:run
   ```
   Or with a local Maven install:
   ```bash
   mvn spring-boot:run
   ```

4. **Verify**
   - Server runs at **http://localhost:8081**
   - Example: `GET http://localhost:8081/transactions?page=0&size=10`
   - Dashboard: `GET http://localhost:8081/dashboard/monthly`
   - Category summary: `GET http://localhost:8081/summary/categories?period=monthly`

---

## Main API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/transactions` | Create a transaction (income/expense; transfer supported by API, not used in UI) |
| `GET` | `/transactions` | List transactions (paginated) |
| `PUT` | `/transactions/{id}` | Update a transaction (within 12 hours) |
| `GET` | `/transactions/filter` | Filter by startDate, endDate, category, division |
| `GET` | `/dashboard/weekly` | Dashboard stats for the week |
| `GET` | `/dashboard/monthly` | Dashboard stats for the month |
| `GET` | `/dashboard/yearly` | Dashboard stats for the year |
| `GET` | `/summary/categories` | Category summary (query param: `period`) |

---

## Project Structure (Overview)

```
src/main/java/com/money/manager/
├── controller/     # REST controllers
├── dto/            # Request/Response DTOs
├── enums/          # Division, TransactionType
├── exception/      # Global exception handling
├── model/          # MongoDB document (Transaction)
├── repository/     # Mongo repository
└── service/        # Business logic (stats, filter, 12-hour rule)
```

---

## Notes

- CORS is enabled for the frontend (e.g. `http://localhost:3000`). Adjust in the controller or via configuration if you deploy to another origin.
- The 12-hour edit rule is enforced in the service layer; the frontend hides the edit button after 12 hours for a better UX.
