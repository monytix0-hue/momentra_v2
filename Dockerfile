# Momentra API (+ workers sources) — Dokploy Application default build.
# Context: repository root (.)
# File: Dockerfile
#
# Compose full stack still uses: backend/deploy/dokploy/docker-compose.yml
#   which builds backend/Dockerfile with context backend/

FROM node:22-alpine AS build
WORKDIR /app

COPY backend/typescript/package.json backend/typescript/package-lock.json ./typescript/
WORKDIR /app/typescript
RUN npm ci

COPY backend/typescript/ ./
COPY backend/workers/ ../workers/
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl tini

COPY backend/typescript/package.json backend/typescript/package-lock.json ./typescript/
WORKDIR /app/typescript
RUN npm ci --omit=dev

COPY --from=build /app/typescript/dist ./dist
COPY backend/typescript/src ./src
COPY backend/typescript/scripts ./scripts
COPY backend/typescript/tsconfig.json ./tsconfig.json
COPY backend/workers/ ../workers/

ENV NODE_ENV=production
ENV PORT=3000
EXPOSE 3000

WORKDIR /app/typescript

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["node", "dist/index.js"]
