FROM node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build && \
    if [ ! -f dist/angular-frontend/browser/index.html ]; then \
      cp dist/angular-frontend/browser/index.csr.html dist/angular-frontend/browser/index.html; \
    fi

FROM nginx:1.27-alpine

RUN apk add --no-cache jq
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/angular-frontend/browser/ /usr/share/nginx/html/
COPY docker/40-demo-config.sh /docker-entrypoint.d/40-demo-config.sh
RUN chmod +x /docker-entrypoint.d/40-demo-config.sh

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
