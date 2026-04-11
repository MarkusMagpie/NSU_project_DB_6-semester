SET search_path TO lab_drug_store;

DROP VIEW IF EXISTS v_unclaimed_orders;
DROP VIEW IF EXISTS v_waiting_customers;
DROP VIEW IF EXISTS v_top_medicines;
DROP VIEW IF EXISTS v_used_components;
DROP VIEW IF EXISTS v_critical_medicines;
DROP VIEW IF EXISTS v_medicine_stock;
DROP VIEW IF EXISTS v_orders_in_production;
DROP VIEW IF EXISTS v_technologies;
DROP VIEW IF EXISTS v_frequent_customers_ready;
DROP VIEW IF EXISTS v_frequent_customers_compounded;

-- 1 Получить сведения о покупателях, которые не пришли забрать свой заказ в назначенное им время и общее их число.
CREATE OR REPLACE VIEW v_unclaimed_orders AS
SELECT
    bc.Client_id AS client_id,
    bc."ФИО" AS full_name,
    bc."Телефон" AS phone,
    bc."Адрес" AS address,
    o.Order_id AS order_id,
    o."Время_изготовления" as completion_time,
    (CURRENT_TIMESTAMP - o."Время_изготовления") AS overdue_interval -- разница между текущем временем и временем изготовления деарства
FROM "Заказы" AS o
    JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
WHERE o."Статус" = 'готов к выдаче'
  AND o."Время_изготовления" IS NOT NULL
  AND o."Время_изготовления" < CURRENT_TIMESTAMP - INTERVAL '1 hour' -- заказы готовы более часа назад
ORDER BY o."Время_изготовления";

-- 2.	Получить перечень и общее число покупателей, которые ждут прибытия на склад нужных им медикаментов в целом
-- и по указанной категории медикаментов.
CREATE OR REPLACE VIEW v_waiting_customers AS
SELECT
    bc.Client_id AS client_id,
    bc."ФИО" AS full_name,
    bc."Телефон" AS phone,
    bc."Адрес" AS address,
    o.Order_id AS order_id,
    l."Название" AS medicine_name,
    l."Тип" AS medicine_type
FROM "Заказы" AS o
    JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
    JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE o."Статус" = 'ожидание компонентов'
ORDER BY bc.Client_id;

-- 3.	Получить перечень десяти наиболее часто используемых медикаментов в целом и указанной категории медикаментов.
CREATE OR REPLACE VIEW v_top_medicines AS
SELECT
    l.Medicine_id AS medicine_id,
    l."Название" AS medicine_name,
    l."Тип" AS medicine_type,
    COUNT(o.Order_id) AS order_count
FROM "Лекарства" AS l
    JOIN "Заказы" AS o ON l.Medicine_id = o.Medicine_id
GROUP BY l.Medicine_id, l."Название", l."Тип"
ORDER BY COUNT(o.Order_id) DESC
LIMIT 10;

-- 4.	Получить какой объем указанных веществ использован за указанный период.
CREATE OR REPLACE VIEW v_used_components AS
SELECT
    c.Component_id AS component_id,
    o.order_id AS order_id,
    c.Name AS component_name,
    SUM(r.quantity_reserved) AS used_quantity
FROM "Резерв_компонентов" AS r
    JOIN "Заказы" AS o ON r.order_id = o.Order_id
    JOIN "Компоненты" AS c ON r.component_id = c.Component_id
WHERE o."Статус" IN ('выполнен', 'в производстве')
GROUP BY c.Component_id, o.order_id, c.Name;

-- 6.	Получить перечень и типы лекарств, достигших своей критической нормы или закончившихся.
CREATE OR REPLACE VIEW v_critical_medicines AS
WITH component_stock AS (
    SELECT
        c.Component_id,
        COALESCE(SUM(p.Quantity), 0) AS Current_quantity
    FROM "Компоненты" AS c
             LEFT JOIN "Партии_компонентов" AS p ON c.Component_id = p.Component_id
    GROUP BY c.Component_id
)
SELECT DISTINCT
    l.Medicine_id AS medicine_id,
    l."Название" AS medicine_name,
    c.Name AS component_name,
    cs.Current_quantity AS component_stock,
    c.Critical_level AS critical_level,
    l."Способ_применения" AS application_method,
    l."Тип" AS medicine_type
FROM "Лекарства" AS l
    JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
    JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
    JOIN "Компоненты" AS c ON r."Компоненты" = c.Component_id
    JOIN component_stock AS cs ON c.Component_id = cs.Component_id
WHERE cs.Current_quantity <= c.Critical_level
ORDER BY medicine_name, component_name;

-- 7.	Получить перечень лекарств с минимальным запасом на складе в целом и по указанной категории медикаментов.
CREATE OR REPLACE VIEW v_medicine_stock AS
WITH component_sum AS (
    SELECT
        Component_id,
        COALESCE(SUM(Quantity), 0) AS total_quantity
    FROM "Партии_компонентов"
    GROUP BY Component_id
),
all_stock AS (
    SELECT
        l.Medicine_id as med_id,
        MIN(FLOOR(cs.total_quantity / r."Количество")) AS maximum_of_units
    FROM "Лекарства" AS l
        JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
        JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
        JOIN component_sum as cs ON r."Компоненты" = cs.Component_id
    WHERE l."Тип" = 'изготавливаемое'
    GROUP BY l.Medicine_id
)
SELECT
    l.Medicine_id AS medicine_id,
    l."Название" AS medicine_name,
    l."Тип" AS medicine_type,
    CASE
        WHEN l."Тип" = 'готовое' THEN g."Остаток"
        ELSE COALESCE(cs.maximum_of_units, 0)
        END AS stock_quantity
FROM "Лекарства" AS l
    LEFT JOIN "Готовые_лекарства" AS g ON l.Medicine_id = g.Medicine_id
    LEFT JOIN all_stock AS cs ON l.Medicine_id = cs.med_id
ORDER BY stock_quantity;

-- 8.	Получить полный перечень и общее число заказов находящихся в производстве.
CREATE OR REPLACE VIEW v_orders_in_production AS
SELECT
    o.Order_id AS order_id,
    o."Статус" AS status,
    o."Дата_создания" AS creation_date,
    l."Название" AS medicine_name,
    bc."ФИО" AS customer_name
FROM "Заказы" AS o
    JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
    JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE o."Статус" = 'в производстве'
ORDER BY "Дата_создания";

-- 9.	Получить полный перечень и общее число препаратов требующихся для заказов, находящихся в производстве.
CREATE OR REPLACE VIEW v_required_medicines_for_production AS
WITH orders_in_production AS (
    SELECT
        o.Order_id AS order_id,
        o.Medicine_id as medicine_id,
        r."Количество_лекарства" AS required_quantity
    FROM "Заказы" AS o
        JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    WHERE o."Статус" = 'в производстве'
)
SELECT
    oi.Order_id,
    l."Название" AS medicine_name,
    oi.required_quantity
FROM orders_in_production AS oi
         JOIN "Лекарства" AS l ON oi.Medicine_id = l.Medicine_id
ORDER BY oi.order_id;

-- 10.	Получить все технологии приготовления лекарств указанных типов, конкретных лекарств, лекарств, находящихся в справочнике заказов в производстве.
CREATE OR REPLACE VIEW v_technologies AS
WITH
-- лекарства из заказов в производстве
production_medicines AS (
    SELECT DISTINCT Medicine_id
    FROM "Заказы"
    WHERE "Статус" = 'в производстве'
)
SELECT
    t.Technology_id AS technology_id,
    t."Название" AS technology_name,
    t."Описание_процесса" AS preparation_description,
    t.Medicine_id AS medicine_id,
    l."Название" AS medicine_name,
    l."Тип" AS medicine_type,
    CASE
        WHEN l."Тип" = 'изготавливаемое' THEN 'by_type' -- 1 - тип
        WHEN l.medicine_id IN (2,5) THEN 'specific_medicine' -- 2 - по конкретному id лекарства
        WHEN l.medicine_id IN (SELECT Medicine_id FROM production_medicines) THEN 'in_production' -- из заказов + 'в производстве'
        END AS reason
FROM "Технологические_карты" AS t
    JOIN "Лекарства" AS l ON t.Medicine_id = l.Medicine_id
ORDER BY l.Medicine_id, t.Technology_id;

-- 12.	Получить сведения о наиболее часто делающих заказы клиентах на готовые лекарства
CREATE OR REPLACE VIEW v_frequent_customers_ready AS
SELECT
    bc.Client_id AS client_id,
    bc."ФИО" AS full_name,
    bc."Телефон" AS phone,
    COUNT(o.Order_id) AS order_count
FROM "Заказы" AS o
    JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
    JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE l."Тип" = 'готовое'
GROUP BY bc.Client_id, bc."ФИО", bc."Телефон"
ORDER BY order_count DESC
LIMIT 10;

-- 12.1	Получить сведения о наиболее часто делающих заказы клиентах на изготавливаемые лекарства
CREATE OR REPLACE VIEW v_frequent_customers_compounded AS
SELECT
    bc.Client_id AS client_id,
    bc."ФИО" AS full_name,
    bc."Телефон" AS phone,
    COUNT(o.Order_id) AS order_count
FROM "Заказы" AS o
    JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
    JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE l."Тип" = 'изготавливаемое'
GROUP BY bc.Client_id, bc."ФИО", bc."Телефон"
ORDER BY order_count DESC
LIMIT 10;
