SET search_path TO lab_drug_store;

-- 1 роли
CREATE ROLE registrator;
CREATE ROLE storekeeper;
CREATE ROLE pharmacist;

-- 2 пользователи
CREATE USER reg_user WITH PASSWORD 'reg_pass';
CREATE USER store_user WITH PASSWORD 'store_pass';
CREATE USER pharm_user WITH PASSWORD 'pharm_pass';

-- 3 назначаю роли пользователям
GRANT registrator TO reg_user;
GRANT storekeeper TO store_user;
GRANT pharmacist TO pharm_user;

-- 4 права доступа к схемам (по умолчанию их у юзеров нема)
GRANT USAGE ON SCHEMA lab_drug_store TO registrator, storekeeper, pharmacist;

-- права для роли registrator
GRANT SELECT, INSERT, UPDATE ("Статус", "Время_изготовления") ON "Заказы" TO registrator;
GRANT SELECT, INSERT, UPDATE ("ФИО", "Телефон", "Адрес") ON "Больные_клиенты" TO registrator;
GRANT SELECT, INSERT ON "Рецепты" TO registrator;
GRANT SELECT ON "Лекарства" TO registrator;
GRANT SELECT ON "Готовые_лекарства" TO registrator;
GRANT SELECT ON "Изготавливаемые_лекарства" TO registrator;
GRANT SELECT ON "Технологические_карты" TO registrator;
GRANT SELECT ON "Рецептуры" TO registrator;
GRANT SELECT ON "Компоненты" TO registrator;
GRANT SELECT ON "Поставщики" TO registrator;
-- может просматривать представления
GRANT SELECT ON v_unclaimed_orders, v_waiting_customers, v_orders_in_production, v_required_medicines_for_production TO registrator;
GRANT EXECUTE ON PROCEDURE add_client TO registrator;
-- TODO - пересмотри необходимость выдачи прав на add_medicine. в теории нужен некий администратор
GRANT EXECUTE ON PROCEDURE add_medicine TO registrator;

-- права для кладовщика для управления складскими таблицами
GRANT SELECT, INSERT, UPDATE ON "Партии_компонентов" TO storekeeper;
GRANT SELECT, INSERT, UPDATE ON "Заявки_на_пополнение_компонентов" TO storekeeper;
GRANT SELECT, INSERT, UPDATE ON "Заявки_на_пополнение_готовых_лека" TO storekeeper;
GRANT SELECT, UPDATE ("Остаток") ON "Готовые_лекарства" TO storekeeper;
GRANT SELECT ON "Компоненты" TO storekeeper;
GRANT SELECT ON "Поставщики" TO storekeeper;
GRANT SELECT ON "Лекарства" TO storekeeper;
GRANT SELECT ON "Изготавливаемые_лекарства" TO storekeeper;
GRANT SELECT ON "Технологические_карты" TO storekeeper;
GRANT SELECT ON "Рецептуры" TO storekeeper;
GRANT SELECT ON "Заказы" TO storekeeper;
GRANT SELECT ON "Рецепты" TO storekeeper;
GRANT SELECT ON "Больные_клиенты" TO storekeeper;
GRANT SELECT ON v_critical_medicines, v_medicine_stock, v_used_components TO storekeeper;

-- права для фармацевта
-- может обновлять статус заказа (переводить в производство и в готов к выдаче)
GRANT UPDATE ("Статус", "Время_изготовления") ON "Заказы" TO pharmacist;
GRANT SELECT ON "Технологические_карты" TO pharmacist;
GRANT SELECT ON "Рецептуры" TO pharmacist;
GRANT SELECT ON "Лекарства" TO pharmacist;
GRANT SELECT ON "Изготавливаемые_лекарства" TO pharmacist;
GRANT SELECT ON "Компоненты" TO pharmacist;
GRANT SELECT ON "Заказы" TO pharmacist;
GRANT SELECT ON "Рецепты" TO pharmacist;
GRANT SELECT ON "Больные_клиенты" TO pharmacist;
GRANT SELECT ON v_technologies, v_orders_in_production, v_required_medicines_for_production TO pharmacist;
-- TODO - пересмотри необходимость выдачи прав на add_medicine. в теории нужен некий администратор
GRANT EXECUTE ON PROCEDURE add_medicine TO pharmacist;