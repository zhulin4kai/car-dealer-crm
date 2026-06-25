ARG DOCKERHUB_LIBRARY_PREFIX=

FROM ${DOCKERHUB_LIBRARY_PREFIX}node:22-alpine AS build
WORKDIR /workspace/dealer-web

ARG NPM_REGISTRY=https://registry.npmmirror.com
ARG VITE_API_BASE_URL=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY dealer-web/package.json dealer-web/package-lock.json ./
RUN npm ci --registry="${NPM_REGISTRY}"

COPY dealer-web/ ./
RUN npm run build

FROM ${DOCKERHUB_LIBRARY_PREFIX}nginx:1.27-alpine

COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dealer-web/dist /usr/share/nginx/html

EXPOSE 80
