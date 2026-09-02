# Momentra Node runtime — API + all TypeScript workers (Dokploy / Compose).
# Build context MUST be backend/ (parent of typescript/ and workers/).
#
#   docker build -f Dockerfile -t momentra-node .
#   docker run ... momentra-node                          # API
#   docker run ... momentra-node npm run worker:outbox    # worker

FROM node:22-alpine AS build
WORKDIR /app

COPY typescript/package.json typescript/package-lock.json ./typescript/
WORKDIR /app/typescript
RUN npm ci

COPY typescript/ ./
COPY workers/ ../workers/
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl tini

COPY typescript/package.json typescript/package-lock.json ./typescript/
WORKDIR /app/typescript
# Production deps include tsx (runtime dependency in package.json)
RUN npm ci --omit=dev

COPY --from=build /app/typescript/dist ./dist
COPY typescript/src ./src
COPY typescript/scripts ./scripts
COPY typescript/tsconfig.json ./tsconfig.json
COPY workers/ ../workers/

ENV NODE_ENV=production
ENV PORT=3000
# Workers run via tsx from /app/workers/* and resolve deps from the API package.
ENV NODE_PATH=/app/typescript/node_modules
EXPOSE 3000

# Workers resolve ../../../.env → /app/.env; Dokploy injects real env vars either way.
WORKDIR /app/typescript

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["node", "dist/index.js"]
