FROM node:22-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY package.json package-lock.json ./
COPY next.config.ts .
COPY postcss.config.mjs .
COPY tsconfig.json .
COPY src src
COPY public public

RUN npm run build

FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production

COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/.next .next
COPY --from=build /app/public public
COPY --from=build /app/next.config.ts .

EXPOSE 3000

CMD ["npm", "run", "start", "--", "-H", "0.0.0.0"]
