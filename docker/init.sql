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

ALTER TABLE transactions ADD CONSTRAINT transactions_call_names_fk FOREIGN KEY (call_id) REFERENCES call_names(call_id) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE TABLE cdr_abonents (
	msisdn int8 NOT NULL,
	CONSTRAINT all_abonents_pk PRIMARY KEY (msisdn)
);

CREATE TABLE tariffs (
	tariff_id varchar NOT NULL,
	tariff_name varchar NULL,
	num_of_minutes int4 NULL,
	price_incoming_calls float4 NULL,
	price_outcoming_calls float4 NULL,
	price_outcoming_calls_camo float4 NULL,
	price_of_period float4 NULL,
	internet_traffic int4 NULL,
	internet_max_speed int4 NULL,
	num_of_incoming_sms int4 NULL,
	num_of_outcoming_sms int4 NULL,
	price_incoming_sms float4 NULL,
	price_outcoming_sms float4 NULL,
	other_info jsonb NULL,
	CONSTRAINT tariffs_pk PRIMARY KEY (tariff_id),
	CONSTRAINT tariffs_unique UNIQUE (tariff_name)
);

CREATE TABLE users (
	msisdn int8 NOT NULL,
	tariff_id varchar NULL,
	money_balance float4 NULL,
	CONSTRAINT users_pk PRIMARY KEY (msisdn),
	CONSTRAINT users_tariffs_fk FOREIGN KEY (tariff_id) REFERENCES tariffs(tariff_id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE users_minutes (
	msisdn int8 NOT NULL,
	tariff_id varchar NULL,
	used_minutes_in int4 NULL,
	used_minutes_out int4 NULL,
	CONSTRAINT users_minutes_pk PRIMARY KEY (msisdn),
	CONSTRAINT users_minutes_tariffs_fk FOREIGN KEY (tariff_id) REFERENCES tariffs(tariff_id) ON DELETE SET NULL ON UPDATE CASCADE,
	CONSTRAINT users_minutes_users_fk FOREIGN KEY (msisdn) REFERENCES users(msisdn) ON DELETE SET NULL ON UPDATE CASCADE
);

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

INSERT INTO tariffs (tariff_id, tariff_name, num_of_minutes, price_incoming_calls, price_outcoming_calls, price_outcoming_calls_camo, price_of_period, internet_traffic, internet_max_speed, num_of_incoming_sms, num_of_outcoming_sms, price_incoming_sms, price_outcoming_sms, other_info) VALUES('11', 'Classic', NULL, 0, 1.5, 2.5, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO tariffs (tariff_id, tariff_name, num_of_minutes, price_incoming_calls, price_outcoming_calls, price_outcoming_calls_camo, price_of_period, internet_traffic, internet_max_speed, num_of_incoming_sms, num_of_outcoming_sms, price_incoming_sms, price_outcoming_sms, other_info) VALUES('12', 'Monthly', 50, NULL, NULL, NULL, 100, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(7968969935, '11', 200);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(74571938267, '11', 100);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(71364416478, '12', 10);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(7747873230, '12', 100);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(74982406633, '12', 200);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(787845253771, '11', 1300);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(74374224158, '12', 300);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(75326984737, '12', 300);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(76168793161, '11', 50);
INSERT INTO users (msisdn, tariff_id, money_balance) VALUES(79298674094, '11', 500);

INSERT INTO call_names (call_id) VALUES('01');
INSERT INTO call_names (call_id) VALUES('02');