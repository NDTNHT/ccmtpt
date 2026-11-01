# Task Management System

> Báo cáo hoàn thiện Vue 3, Pinia, Kanban, localStorage, AI và CI/CD: [docs/README.md](docs/README.md).

Dịch vụ quản lý công việc viết bằng Java 17, cung cấp REST API và ứng dụng console trên cùng
một lõi nghiệp vụ. Dữ liệu được lưu trong SQLite; có thể thay bằng JSON file hoặc bộ nhớ tạm
khi cần.

## 1. Tính năng

- Tạo, xem, cập nhật và theo dõi vòng đời công việc: `TODO → IN_PROGRESS → DONE`, `CANCELLED`.
- Tự động tính độ ưu tiên theo hạn chót và loại dự án khi client không gửi `priority`.
- Quản lý dự án; công việc không thuộc dự án nào được đưa vào `INBOX`.
- Quản lý công việc con (subtask) trong cùng một công việc.
- Gia hạn hạn chót và tính lại độ ưu tiên.
- Lưu trữ bền vững bằng SQLite, JSON file hoặc bộ nhớ.
- Ghi nhật ký hoạt động từ các sự kiện nghiệp vụ.

## 2. Yêu cầu

- Java 17 trở lên
- Maven 3.8 trở lên

## 3. Build và kiểm thử

```bash
mvn clean test
mvn clean package
```

Bộ kiểm thử chạy offline, sử dụng database và file tạm cho từng test.

## 4. Chạy ứng dụng console

```bash
mvn clean compile
mvn exec:java
```

Mặc định dữ liệu được lưu tại `data/tasks.db`. Có thể chỉ định database khác:

```bash
mvn exec:java -Dtaskmanagement.db=data/tasks-prod.db
```

Menu console hỗ trợ xem danh sách, tạo công việc, chọn hoặc tạo dự án, bắt đầu, hoàn thành,
hủy và tính lại độ ưu tiên.

## 5. Chạy REST API

```bash
mvn spring-boot:run
```

Mặc định chạy tại `http://localhost:8080`. Đổi cổng khi cần:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

Nếu cổng 8080 đã bị chiếm (ví dụ bởi Steam trên Windows), dùng cổng khác:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18081

# hoặc chạy jar đã đóng gói
java -Dserver.port=18081 -jar target/task-management-system-1.0.0.jar
```

### Endpoint

| Method | Path | Mô tả |
|---|---|---|
| `POST` | `/api/tasks` | Tạo công việc |
| `GET` | `/api/tasks` | Danh sách công việc |
| `GET` | `/api/tasks/{id}` | Chi tiết công việc |
| `GET` | `/api/tasks/by-project/{projectId}` | Công việc theo dự án |
| `POST` | `/api/tasks/{id}/start` | Chuyển sang `IN_PROGRESS` |
| `POST` | `/api/tasks/{id}/complete` | Chuyển sang `DONE` |
| `POST` | `/api/tasks/{id}/cancel` | Chuyển sang `CANCELLED` |
| `POST` | `/api/tasks/{id}/refresh-priority` | Tính lại độ ưu tiên |
| `PATCH` | `/api/tasks/{id}/deadline` | Gia hạn hạn chót |
| `GET` | `/api/projects` | Danh sách dự án |
| `GET` | `/api/projects/inbox` | Dự án `INBOX` |
| `POST` | `/api/projects` | Tạo dự án |

### Ví dụ

Tạo dự án:

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Ung dung quan ly","description":"Ke hoach quy 4"}'
```

Tạo công việc:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Thiet ke giao dien","description":"Man hinh danh sach","dueDate":"2026-12-31T17:00:00Z","priority":"HIGH"}'
```

- Bỏ trống `priority` để hệ thống tự tính theo hạn chót và dự án.
- Bỏ trống `projectId` để đưa công việc vào `INBOX`.
- `projectId` không tồn tại trả về `404`.

### Mã lỗi

| HTTP | Trường hợp |
|---|---|
| `400` | Dữ liệu request không hợp lệ, sai định dạng UUID |
| `404` | Không tìm thấy công việc hoặc dự án |
| `409` | Chuyển trạng thái không hợp lệ |
| `422` | Vi phạm quy tắc nghiệp vụ |
| `500` | Lỗi không mong đợi phía server |

## 6. Triển khai

### Chạy bằng jar

```bash
java -jar target/task-management-system-1.0.0.jar
```

Cấu hình qua biến môi trường:

```bash
SERVER_PORT=9090 \
TASKMANAGEMENT_DB=/var/lib/task-management/tasks.db \
API_KEY=change-me \
java -jar target/task-management-system-1.0.0.jar
```

Trên PowerShell:

```powershell
$env:SERVER_PORT="9090"
$env:TASKMANAGEMENT_DB="C:\task-management\tasks.db"
$env:API_KEY="change-me"
java -jar target\task-management-system-1.0.0.jar
```

### Docker

```bash
docker build -t task-management-system:1.0.0 .
docker run -d --name task-management \
  -p 8080:8080 \
  -v task-data:/app/data \
  -e API_KEY=change-me \
  task-management-system:1.0.0
```

Hoặc dùng Docker Compose:

```bash
API_KEY=change-me docker compose up -d --build
```

Dữ liệu được giữ trong volume `task-data` tại `/app/data`.

### Health check

```bash
curl http://localhost:8080/actuator/health
```

Endpoint này luôn công khai để container/orchestrator kiểm tra trạng thái. Dockerfile đã cấu hình
`HEALTHCHECK` gọi endpoint này.

### Xác thực API

Khi đặt `API_KEY`, mọi endpoint `/api/**` yêu cầu header:

```text
X-API-Key: <giá trị API_KEY>
```

Nếu `API_KEY` để trống, xác thực bị tắt (phù hợp môi trường nội bộ hoặc phát triển).

### Trước khi đưa lên production

- Đặt `API_KEY` và chạy sau reverse proxy có TLS.
- Mount volume cho database và có kế hoạch backup định kỳ.
- Chạy một instance ghi SQLite; không scale ngang nhiều instance cùng file.
- Gửi log ra hệ thống tập trung (stdout đã ở định dạng SLF4J).
- Đặt giới hạn CPU/RAM và timeout của reverse proxy.
- Domain event hiện xử lý trong tiến trình; nếu cần đảm bảo không mất sự kiện khi crash, bổ sung
  outbox và message broker.

## 7. Cấu hình

| Thuộc tính | Biến môi trường | Mặc định | Mô tả |
|---|---|---|---|
| `server.port` | `PORT` / `SERVER_PORT` | `8080` | Cổng REST API |
| `taskmanagement.db` | `TASKMANAGEMENT_DB` | `data/tasks.db` | Đường dẫn database SQLite |
| `api.key` | `API_KEY` | trống | Bật xác thực bằng header `X-API-Key` |
| `taskmanagement.file` | — | `data/tasks.json` | Đường dẫn file khi dùng JSON repository |

Thứ tự ưu tiên: tham số dòng lệnh (`--server.port=...`) → biến môi trường → `application.properties`.
Có thể truyền `-Dtaskmanagement.db=...` khi chạy trực tiếp.

Ứng dụng mặc định dùng SQLite. Để dùng JSON file hoặc bộ nhớ, thay repository tương ứng trong
`Main` hoặc `ApiApplication`.

## 8. Dữ liệu

SQLite lưu ba bảng: `tasks`, `subtasks`, `projects`. Công việc và công việc con được ghi trong
cùng một transaction. Database bật chế độ WAL và có index theo `project_id` để đọc/ghi ổn định
hơn khi có truy cập đồng thời.

Khi nâng cấp từ phiên bản cũ, kiểm tra các bản ghi có `project_id` không còn tồn tại. Ứng dụng
trả về lỗi rõ ràng thay vì tự gán lại dự án. Có thể xóa `data/tasks.db` để khởi tạo dữ liệu mới.

## 9. Cấu trúc thư mục

```text
.
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── data/
├── dependencies/
│   └── src/
│       ├── main/java/com/taskmanagement/
│       │   ├── domain/          # Mô hình nghiệp vụ, value object, sự kiện
│       │   ├── application/     # Use case, DTO, boundary, port
│       │   ├── adapter/         # REST, persistence, presenter
│       │   └── infrastructure/  # Cấu hình, console, event, listener
│       └── test/java/com/taskmanagement/
└── target/
```

## 10. Kiểm thử

```bash
mvn clean test
```

Bộ test bao phủ: nghiệp vụ công việc và dự án, tính độ ưu tiên, chuyển trạng thái, quy tắc
trùng tiêu đề, persistence SQLite/JSON, presenter, xác thực API, xử lý input console và các
trường hợp lỗi của API.

## 11. Phiên bản

`1.0.0`
