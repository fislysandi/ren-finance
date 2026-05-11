FROM clojure:temurin-21-bookworm AS build
RUN apt-get update && apt-get install -y git curl unzip xz-utils bash && rm -rf /var/lib/apt/lists/*
RUN curl -sSfL https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_3.13.9-stable.tar.xz | tar xJ -C /opt
ENV PATH="/opt/flutter/bin:${PATH}"
RUN flutter config --no-analytics
WORKDIR /build
COPY frontend/ /build/frontend/
RUN cd frontend && flutter pub get 2>&1 && clojure -M:cljd compile 2>&1 && flutter build web --release 2>&1 && mkdir -p /output && cp -r build/web /output/
