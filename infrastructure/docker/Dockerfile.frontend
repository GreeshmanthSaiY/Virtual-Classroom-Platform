# ==============================================================================
# Virtual Classroom Platform - Frontend Multi-Stage Production Dockerfile
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build static assets using Node.js
# ------------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Build arguments for Vite compilation
ARG VITE_API_BASE_URL="/api/v1"
ARG VITE_APP_NAME="Virtual Classroom"
ARG VITE_APP_DESCRIPTION="Enterprise Modern LMS & Virtual Classroom Platform"
ARG VITE_ENABLE_DEMO_CREDENTIALS="false"

# Pass to Vite build environment
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_APP_NAME=${VITE_APP_NAME}
ENV VITE_APP_DESCRIPTION=${VITE_APP_DESCRIPTION}
ENV VITE_ENABLE_DEMO_CREDENTIALS=${VITE_ENABLE_DEMO_CREDENTIALS}

# Copy dependency manifests and install cleanly
COPY package*.json ./
RUN npm ci --prefer-offline --no-audit

# Copy source code and configuration
COPY . .

# Compile TypeScript and generate optimized Vite production bundles
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Serve static artifacts with hardened, lightweight Nginx
# ------------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runner

# Remove default static pages and configurations
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy production build artifacts from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy optimized Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Container healthcheck: verify root and healthz response
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/healthz || exit 1

# Run Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
