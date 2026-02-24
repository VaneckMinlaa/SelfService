# Stage 1: Construction
# Compiler et builder l'application React avec Vite
FROM node:20-alpine AS build
WORKDIR /app

# Copier manifeste des dépendances
COPY package.json package-lock.json* ./
# Installer les dépendances
RUN npm ci

# Copier tout le code source
COPY . .
# Builder l'application React (sortie dans /app/dist)
RUN npm run build

# Stage 2: Exécution
# Serveur web pour les fichiers statiques
FROM nginx:alpine
# Copier les fichiers statiques buildés depuis le stage précédent
COPY --from=build /app/dist /usr/share/nginx/html
# Supprimer la configuration par défaut Nginx
RUN rm -f /etc/nginx/conf.d/default.conf
# Copier la configuration Nginx personnalisée
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
# Démarrer Nginx en mode foreground
CMD ["nginx", "-g", "daemon off;"]
