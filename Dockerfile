FROM clojure:temurin-21-bookworm AS build
RUN apt-get update && apt-get install -y git curl unzip xz-utils bash && rm -rf /var/lib/apt/lists/*
RUN git clone --depth 1 --branch 3.24.5 https://github.com/flutter/flutter.git /opt/flutter
ENV PATH="/opt/flutter/bin:${PATH}"
RUN flutter config --no-analytics
WORKDIR /build
COPY frontend/ /build/frontend/
RUN cd frontend && flutter pub get 2>&1 && clojure -M:cljd compile 2>&1; echo "CLJD_EXIT=$?"
