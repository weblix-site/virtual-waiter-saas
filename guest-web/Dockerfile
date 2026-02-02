FROM node:20-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE
ENV NEXT_TELEMETRY_DISABLED=1
COPY guest-web/package*.json ./
RUN npm ci
COPY guest-web/ ./
RUN npm run build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=build /app/package*.json ./
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
RUN npm ci --omit=dev
EXPOSE 3000
CMD ["npm", "run", "start"]
