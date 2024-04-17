# README

Пока что программа работает в "консольном режиме". Единого докер-компоуза для развёртки проекта пока что нет. Но есть другой докер-компоуз с постгресом и кафками!. Для его запуска ставим Docker Desktop и WSL

1. [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
2. [https://learn.microsoft.com/ru-ru/windows/wsl/install](https://learn.microsoft.com/ru-ru/windows/wsl/install)

---

---

После подготовки докера создаём новую папку с проектом и “пуллим” образ для Постгреса

```powershell
mkdir BootcampProj
docker pull postgres
```

Дальше создаём `docker-compose.yml` 

```powershell
cat << EOF > docker-compose.yml
version: '3'
services:
  db:
    image: postgres
    container_name: postgres-server
    restart: always
    shm_size: 128mb
    environment:
      POSTGRES_DB: bootcamp_db
    expose:
      - "5434"
    ports:
      - "5434:5434"
    command: -p 5434
  adminer:
    image: adminer
    restart: always
    ports:
      - 5435:5435
  zookeeper:
    image: confluentinc/cp-zookeeper:7.2.1
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    restart: always
  kafka1:
    image: confluentinc/cp-kafka:7.2.1
    container_name: kafka1
    ports:
      - "8097:8097"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: EXTERNAL://localhost:8097,INTERNAL://kafka1:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CREATE_TOPICS: "data-topic:2:1, trigger-topic:1:1"
    restart: always
  kafka2:
    image: confluentinc/cp-kafka:7.2.1
    container_name: kafka2
    ports:
      - "8098:8098"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 2
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: EXTERNAL://localhost:8098,INTERNAL://kafka2:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CREATE_TOPICS: "data-topic:2:1, trigger-topic:1:1"
    restart: always
  kafka3:
    image: confluentinc/cp-kafka:7.2.1
    container_name: kafka3
    ports:
      - "8099:8099"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 3
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: EXTERNAL://localhost:8099,INTERNAL://kafka3:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CREATE_TOPICS: "data-topic:2:1, trigger-topic:1:1"
    restart: always
EOF
```

И поднимаем компоузер

```powershell
docker-compose up
```

Кафка немного очень сильно выкаблучивается и не поднимает топики записанные в `**docker-compose**`, поэтому нужно прописать ещё пару строк кода

```powershell
docker-compose exec kafka1 kafka-topics --create --topic data-topic --partitions 2 --replication-factor 1 --if-not-exists --bootstrap-server localhost:9092
docker-compose exec kafka1 kafka-topics --create --topic trigge-topic --partitions 1 --replication-factor 1 --if-not-exists --bootstrap-server localhost:9092
```

Докер наконец-то готов! Java проект вроде должен подниматься без проблем, Maven и Spring всё сами поднимут

---

Скрипт для заполнения бдшки

```sql
CREATE TABLE cdr_abonents (
	msisdn int8 NOT NULL,
	CONSTRAINT all_abonents_pk PRIMARY KEY (msisdn)
);

CREATE TABLE call_names (
	call_id varchar NOT NULL,
	CONSTRAINT call_names_pk PRIMARY KEY (call_id)
);

CREATE TABLE transactions (
	transaction_id serial4 NOT NULL,
	msisdn int8 NULL,
	msisdn_to int8 NULL,
	call_id varchar NULL,
	unix_start int4 NULL,
	unix_end int4 NULL,
	CONSTRAINT transactions_pk PRIMARY KEY (transaction_id)
);

ALTER TABLE transactions ADD CONSTRAINT transactions_all_abonents_fk FOREIGN KEY (msisdn) REFERENCES cdr_abonents(msisdn) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE transactions ADD CONSTRAINT transactions_call_names_fk FOREIGN KEY (call_id) REFERENCES call_names(call_id) ON DELETE SET NULL ON UPDATE CASCADE;

INSERT INTO call_names (call_id) VALUES('01');
INSERT INTO call_names (call_id) VALUES('02');

INSERT INTO cdr_abonents (msisdn) VALUES(7968969935);
INSERT INTO cdr_abonents (msisdn) VALUES(74571938267);
INSERT INTO cdr_abonents (msisdn) VALUES(71364416478);
INSERT INTO cdr_abonents (msisdn) VALUES(7747873230);
INSERT INTO cdr_abonents (msisdn) VALUES(74982406633);
INSERT INTO cdr_abonents (msisdn) VALUES(787845253770);
INSERT INTO cdr_abonents (msisdn) VALUES(74374224157);
INSERT INTO cdr_abonents (msisdn) VALUES(75326984736);
INSERT INTO cdr_abonents (msisdn) VALUES(76168793160);
INSERT INTO cdr_abonents (msisdn) VALUES(79298674093);
```
