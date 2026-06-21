FROM nginx:1.27-alpine

COPY frontend/dist /usr/share/nginx/html
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
