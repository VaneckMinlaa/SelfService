FROM nginx:alpine

RUN apk add --no-cache openssl curl

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSED 80 443

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
