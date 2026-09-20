# ── Stage 1: Build the Vite bundle ──────────────────────────────────────────
# Node 20, not 18: @clerk/react requires >=20.9.0 and react-router@7 requires
# >=20.0.0. Node 18 is also past end-of-life.
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

# Vite inlines import.meta.env at BUILD time, so these must be build args.
# Passing them as runtime `environment:` in compose has no effect whatsoever.
ARG VITE_CLERK_PUBLISHABLE_KEY
ARG VITE_RAZORPAY_KEY_ID
ENV VITE_CLERK_PUBLISHABLE_KEY=$VITE_CLERK_PUBLISHABLE_KEY
ENV VITE_RAZORPAY_KEY_ID=$VITE_RAZORPAY_KEY_ID

# Fail loudly at build time rather than shipping a bundle that renders the
# "Missing Clerk Publishable Key" error screen.
RUN test -n "$VITE_CLERK_PUBLISHABLE_KEY" || \
    (echo "ERROR: build arg VITE_CLERK_PUBLISHABLE_KEY is required" && exit 1)

COPY . .
RUN npm run build

# ── Stage 2: Serve via Nginx (static files + /api reverse proxy) ────────────
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost/ >/dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
