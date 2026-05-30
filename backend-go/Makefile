.PHONY: build run test clean docker-build docker-run

# 变量
BINARY_NAME=crm-server
CMD_PATH=cmd/crm-server/main.go
BUILD_PATH=./build

# 默认目标
all: build

# 下载依赖
deps:
	go mod download

# 构建
build:
	@echo "Building $(BINARY_NAME)..."
	@mkdir -p $(BUILD_PATH)
	CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o $(BUILD_PATH)/$(BINARY_NAME) $(CMD_PATH)
	@echo "Build completed!"

# 本地运行
run:
	go run $(CMD_PATH)

# 热重载开发 (需要安装 air: go install github.com/air-verse/air@latest)
dev:
	air

# 测试
test:
	go test -v ./...

# 测试覆盖率
test-cover:
	go test -coverprofile=coverage.out ./...
	go tool cover -html=coverage.out -o coverage.html

# 代码格式化
fmt:
	go fmt ./...

# 代码检查
lint:
	golangci-lint run

# 清理
clean:
	@echo "Cleaning..."
	@rm -rf $(BUILD_PATH)
	@rm -f coverage.out coverage.html
	@echo "Clean completed!"

# Docker 构建
docker-build:
	docker build -t $(BINARY_NAME):latest .

# Docker 运行
docker-run:
	docker run -d -p 8080:8080 --name $(BINARY_NAME) $(BINARY_NAME):latest

# 生成 Swagger 文档
swagger:
	swag init -g $(CMD_PATH) -o ./docs

# 数据库迁移
migrate-up:
	migrate -path migrations -database "$(DATABASE_URL)" up

migrate-down:
	migrate -path migrations -database "$(DATABASE_URL)" down

# 帮助信息
help:
	@echo "Usage:"
	@echo "  make deps          - 下载依赖"
	@echo "  make build         - 构建项目"
	@echo "  make run           - 运行项目"
	@echo "  make dev           - 开发模式 (热重载)"
	@echo "  make test          - 运行测试"
	@echo "  make test-cover    - 测试覆盖率"
	@echo "  make fmt           - 格式化代码"
	@echo "  make lint          - 代码检查"
	@echo "  make clean         - 清理构建文件"
	@echo "  make docker-build  - Docker 构建"
	@echo "  make docker-run    - Docker 运行"
	@echo "  make swagger       - 生成 Swagger 文档"
	@echo "  make migrate-up    - 执行数据库迁移"
	@echo "  make migrate-down  - 回滚数据库迁移"
