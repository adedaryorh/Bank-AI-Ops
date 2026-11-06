FROM node:22.22.3-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

ARG NG_OIDC_AUTHORITY=http://localhost:8180/realms/bankops
ARG NG_OIDC_CLIENT_ID=bankops-frontend
ARG NG_API_BASE_URL=http://localhost:8080/api/v1
RUN npm run build && node -e 'const fs = require("node:fs"); const path = "dist/bankops-frontend/browser/runtime-config.js"; const config = { oidcAuthority: process.env.NG_OIDC_AUTHORITY, oidcClientId: process.env.NG_OIDC_CLIENT_ID, apiBaseUrl: process.env.NG_API_BASE_URL }; fs.appendFileSync(path, "\nObject.assign(window.__BANKOPS_CONFIG__, " + JSON.stringify(config) + ");\n");'

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/bankops-frontend/browser /usr/share/nginx/html
# Source assets may be owner-only; nginx workers must be able to read built files.
RUN chmod -R a+rX /usr/share/nginx/html

EXPOSE 80
