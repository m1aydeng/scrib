#!/bin/bash

# Scrib 博客系统一键启动脚本
# 使用方法: ./start.sh

set -e

echo "=========================================="
echo "  Scrib 博客系统 Docker 启动脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 检查 docker-compose 是否可用
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: docker-compose 未安装${NC}"
    exit 1
fi

# 使用 docker compose 或 docker-compose
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

echo -e "${YELLOW}步骤 1/4: 停止并删除旧容器...${NC}"
$COMPOSE_CMD down 2>/dev/null || true
echo -e "${GREEN}✓ 旧容器已清理${NC}"
echo ""

echo -e "${YELLOW}步骤 2/4: 重新构建镜像（这可能需要几分钟）...${NC}"
$COMPOSE_CMD build
echo -e "${GREEN}✓ 镜像构建完成${NC}"
echo ""

echo -e "${YELLOW}步骤 3/4: 启动服务...${NC}"
$COMPOSE_CMD up -d
echo -e "${GREEN}✓ 服务已启动${NC}"
echo ""

echo -e "${YELLOW}步骤 4/4: 等待服务就绪...${NC}"
echo "正在等待数据库启动..."
sleep 5

# 等待数据库健康检查通过
MAX_WAIT=60
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker exec scrib-db mysqladmin ping -h localhost -u root -proot --silent 2>/dev/null; then
        echo -e "${GREEN}✓ 数据库已就绪${NC}"
        break
    fi
    echo -n "."
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
done

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    echo -e "\n${RED}警告: 数据库启动超时，但继续等待应用启动...${NC}"
fi

echo ""
echo "正在等待应用启动..."
sleep 10

# 等待应用健康检查
MAX_WAIT=120
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    HTTP_CODE=$(docker exec scrib-app wget --quiet --tries=1 --spider --timeout=5 -O /dev/null -S http://localhost:8080/scrib/home 2>&1 | grep -oP 'HTTP/\d\.\d \K\d+' || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ 应用已就绪${NC}"
        break
    fi
    echo -n "."
    sleep 3
    WAIT_COUNT=$((WAIT_COUNT + 3))
done

echo ""
echo ""
echo "=========================================="
echo -e "${GREEN}  启动完成！${NC}"
echo "=========================================="
echo ""
echo "访问地址:"
echo -e "  ${GREEN}http://localhost:8080/scrib${NC}"
echo ""
echo "服务状态:"
$COMPOSE_CMD ps
echo ""
echo "查看日志:"
echo "  $COMPOSE_CMD logs -f"
echo ""
echo "停止服务:"
echo "  $COMPOSE_CMD down"
echo ""

