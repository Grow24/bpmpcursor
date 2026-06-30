FROM node:20-alpine

WORKDIR /app

COPY package.json ./
COPY . .

RUN chmod +x deploy.sh

ENV NODE_ENV=production

CMD ["npm", "start"]
