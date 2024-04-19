# README

Пока что программа работает в "консольном режиме". Единого докер-компоуза для развёртки проекта пока что нет. Но есть другой докер-компоуз - с постгресом и кафками! Для его запуска ставим Docker Desktop и WSL

1. [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
2. [https://learn.microsoft.com/ru-ru/windows/wsl/install](https://learn.microsoft.com/ru-ru/windows/wsl/install)

Далее открываем терминал в папке **docker** (там где лежит **docker-compse.yml**), прописываем следующую команду и поднимаем все нужные сервисы (PostgreSQL, Kafka)

```powershell
docker-compose up
```
