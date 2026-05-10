# Multi-stage Clojure backend build
FROM clojure:temurin-21-alpine AS build
WORKDIR /app
COPY backend/deps.edn backend/
RUN clojure -Srepro -Spath -A:dependency > /dev/null 2>&1 || true
COPY backend/ .
RUN clojure -M -e "(compile (quote ren-finance.core))" 2>/dev/null; exit 0
RUN mkdir -p target/classes

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash curl
WORKDIR /app
COPY --from=build /app /app
EXPOSE 3000
VOLUME ["/data"]
ENV REN_STORAGE_PATH=/data/ren-finance
ENV REN_HTTP_PORT=3000
ENV REN_LOG_LEVEL=info
CMD ["sh", "-c", "cd /app/backend && clojure -M -m ren-finance.server.core"]
