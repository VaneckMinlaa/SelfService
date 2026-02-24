# Image Nginx Alpine pour la passerelle API
FROM nginx:alpine

# Installer les outils nécessaires: openssl (certificats) et curl (health check)
RUN apk add --no-cache openssl curl

# Copier la configuration Nginx personnalisée
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exposer les ports HTTP et HTTPS
EXPOSE 80 443

# Health check pour vérifier la disponibilité d'Nginx
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# Démarrer Nginx en mode foreground
CMD ["nginx", "-g", "daemon off;"]
