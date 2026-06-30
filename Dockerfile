FROM node:20-alpine

WORKDIR /app

# git: post-commit hook setup on "Open in Cursor"
RUN apk add --no-cache git

COPY package.json ./
COPY . .

RUN chmod +x deploy.sh

ENV NODE_ENV=production

CMD ["npm", "start"]
