SET search_path TO lab_drug_store;



-- 1 Получить сведения о покупателях, которые не пришли забрать свой заказ в назначенное им время и общее их число.
SELECT
    bc.Client_id AS ID_клиента,
    bc."ФИО" AS ФИО,
    bc."Телефон" AS Телефон,
    bc."Адрес" AS Адрес,
    o.Order_id AS Номер_заказа,
    o."Время_изготовления" AS Время_готовности,
    CURRENT_TIMESTAMP - o."Время_изготовления" AS Просрочка -- разница между текущем временем и временем изготовления деарства
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
WHERE o."Статус" = 'готов к выдаче'
  AND o."Время_изготовления" IS NOT NULL
  AND o."Время_изготовления" < CURRENT_TIMESTAMP - INTERVAL '1 hour' -- заказы готовы более часа назад
ORDER BY o."Время_изготовления";



-- 2.	Получить перечень и общее число покупателей, которые ждут прибытия на склад нужных им медикаментов в целом
-- и по указанной категории медикаментов.
SELECT
    bc.Client_id AS ID_клиента,
    bc."ФИО" AS ФИО,
    bc."Телефон" AS Телефон,
    bc."Адрес" AS Адрес,
    o.Order_id AS Номер_заказа,
    l."Название" AS Лекарство
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
         JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE o."Статус" = 'ожидание компонентов'
ORDER BY bc.Client_id;



-- 3.	Получить перечень десяти наиболее часто используемых медикаментов в целом и указанной категории медикаментов.
SELECT
    l.Medicine_id AS ID,
    l."Название" AS Лекарство,
    l."Тип" AS Тип,
    COUNT(o.Order_id) AS Количество_заказов -- агрегирующая функция
FROM "Лекарства" AS l
         JOIN "Заказы" AS o ON l.Medicine_id = o.Medicine_id
GROUP BY l.Medicine_id, l."Название", l."Тип"
ORDER BY COUNT(o.Order_id) DESC
LIMIT 10;



-- 4.	Получить какой объем указанных веществ использован за указанный период.
SELECT
    c.Component_id AS ID_компонента,
    c.Name AS Компонент,
    SUM(r.quantity_reserved) AS Использовано
FROM "Резерв_компонентов" AS r
         JOIN "Заказы" AS o ON r.order_id = o.Order_id
         JOIN "Компоненты" AS c ON r.component_id = c.Component_id
WHERE o."Статус" IN ('выполнен', 'в производстве')
  AND o."Дата_создания" BETWEEN '2025-03-01' AND '2025-03-31'
GROUP BY c.Component_id, c.Name
ORDER BY Использовано DESC;



-- 5.	Получить перечень и общее число покупателей, заказывавших определенное лекарство
-- или определенные типы лекарств за данный период.
SELECT DISTINCT
    bc.Client_id AS ID_клиента,
    bc."ФИО" AS ФИО,
    bc."Телефон" AS Телефон,
    bc."Адрес" AS Адрес
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
WHERE o.Medicine_id = 1
  AND o."Дата_создания" BETWEEN '2026-03-01' AND '2026-03-07';



-- 6.	Получить перечень и типы лекарств, достигших своей критической нормы или закончившихся.
WITH component_stock AS (
    SELECT
        c.Component_id,
        COALESCE(SUM(p.Quantity), 0) AS Current_quantity
    FROM "Компоненты" AS c
             LEFT JOIN "Партии_компонентов" AS p ON c.Component_id = p.Component_id
    GROUP BY c.Component_id
)
SELECT DISTINCT
    l.Medicine_id AS ID_лекарства,
    l."Название",
    l."Тип",
    l."Способ_применения"
FROM "Лекарства" AS l
         JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
         JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
         JOIN "Компоненты" AS c ON r."Компоненты" = c.Component_id
         JOIN component_stock AS cs ON c.Component_id = cs.Component_id
WHERE cs.Current_quantity <= c.Critical_level
ORDER BY "Название";



-- 7.	Получить перечень лекарств с минимальным запасом на складе в целом и по указанной категории медикаментов.
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
             MIN(FLOOR(cs.total_quantity / r.Количество)) AS maximum_of_units
         FROM "Лекарства" AS l
                  JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
                  JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
                  JOIN component_sum as cs ON r."Компоненты" = cs.Component_id
         WHERE l."Тип" = 'изготавливаемое'
         GROUP BY l.Medicine_id
     )
SELECT
    l.Medicine_id AS ID_лекарства,
    l."Название" AS Лекарство,
    l."Тип" AS Тип,
    CASE
        WHEN l."Тип" = 'готовое' THEN g."Остаток"
        ELSE COALESCE(cs.maximum_of_units, 0)
        END AS "Запас на складе, ед."
FROM "Лекарства" AS l
         LEFT JOIN "Готовые_лекарства" AS g ON l.Medicine_id = g.Medicine_id
         LEFT JOIN all_stock AS cs ON l.Medicine_id = cs.med_id
ORDER BY "Запас на складе, ед.";



-- 8.	Получить полный перечень и общее число заказов находящихся в производстве.
SELECT
    o.Order_id AS ID_заказа,
    o."Статус" AS Статус_заказа,
    o."Дата_создания" AS Дата_создания,
    l."Название" AS Лекарство,
    bc."ФИО" AS ФИО_клиента
FROM "Заказы" AS o
    JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
    JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE o."Статус" = 'в производстве'
ORDER BY "Дата_создания";



-- 9.	Получить полный перечень и общее число препаратов требующихся для заказов, находящихся в производстве.
WITH orders_in_production AS (
    SELECT
        o.Order_id,
        o.Medicine_id,
        r."Количество_лекарства" AS Требуемое_количество
    FROM "Заказы" AS o
        JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
    WHERE o."Статус" = 'в производстве'
)
SELECT
    oi.Order_id AS Номер_заказа,
    l."Название" AS Лекарство,
    oi.Требуемое_количество AS "Количество, ед."
FROM orders_in_production AS oi
    JOIN "Лекарства" AS l ON oi.Medicine_id = l.Medicine_id
ORDER BY Номер_заказа;



-- 10.	Получить все технологии приготовления лекарств указанных типов, конкретных лекарств, лекарств, находящихся в справочнике заказов в производстве.
WITH
-- лекарства из заказов в производстве
production_medicines AS (
    SELECT DISTINCT Medicine_id
    FROM "Заказы"
    WHERE "Статус" = 'в производстве'
),
-- технологии с привязкой к лекарствам
technologies AS (
    SELECT
    t.Technology_id,
    t."Название" AS Технология,
    t."Описание_процесса",
    t.Medicine_id,
    l."Название" AS Лекарство,
    l."Тип"
FROM "Технологические_карты" AS t
    JOIN "Лекарства" AS l ON t.Medicine_id = l.Medicine_id
)
SELECT
    Technology_id,
    Технология,
    "Описание_процесса",
    Medicine_id,
    Лекарство,
    CASE
        WHEN "Тип" = 'изготавливаемое' THEN 'по типу' -- 1 - тип
        WHEN Medicine_id IN (2,5) THEN 'конкретное лекарство' -- 2 - по конкретному id лекарства
        WHEN Medicine_id IN (SELECT Medicine_id FROM production_medicines) THEN 'в производстве' -- из заказов + 'в производстве'
        END AS Причина
FROM technologies
ORDER BY Medicine_id, Technology_id;



-- 11.	Получить сведения о ценах на указанное лекарство в готовом виде,
-- об объеме и ценах на все компоненты, требующиеся для этого лекарства.
SELECT
    l.Medicine_id,
    l."Название" AS "Лекарство",
    l."Цена" AS "Цена лекарства, руб.",
    c.Component_id,
    c.Name AS "Компонент",
    c.price AS "Цена компонента, руб./ед.",
    r."Количество" AS "Требуемый объем на единицу лекарства",
    (r."Количество" * c.price) AS "Стоимость на единицу лекраства, руб."
FROM "Лекарства" AS l
         JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
         JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
         JOIN "Компоненты" AS c ON r."Компоненты" = c.Component_id
WHERE l.Medicine_id = 4 -- указать ID изготавливаемого лекарства
ORDER BY l.medicine_id, c.Component_id;



-- 12.	Получить сведения о наиболее часто делающих заказы клиентах на медикаменты определенного типа,
-- на конкретные медикаменты.
SELECT
    bc.Client_id,
    bc."ФИО" AS "ФИО клиента",
    bc."Телефон",
    COUNT(o.Order_id) AS Количество_заказов
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
         JOIN "Лекарства" AS l ON o.Medicine_id = l.Medicine_id
WHERE l."Тип" = 'готовое' -- или 'изготавливаемое'
GROUP BY bc.Client_id, "ФИО клиента", bc."Телефон"
ORDER BY Количество_заказов DESC
LIMIT 10;



-- 13.	Получить сведения о конкретном лекарстве (его тип, способ приготовления, названия всех компонент,
-- цены, его количество на складе).
WITH component_stock AS (
    SELECT
        c.Component_id,
        c.Name,
        c.Price,
        COALESCE(SUM(p.Quantity), 0) AS stock_quantity -- суммарное количесвто компонента по партиям
    FROM "Компоненты" AS c
             LEFT JOIN "Партии_компонентов" AS p ON c.Component_id = p.Component_id
    GROUP BY c.Component_id, c.Name, c.Price
),
-- из query7.sql
     max_units AS (
         SELECT
             l.Medicine_id,
             MIN(FLOOR(cs.stock_quantity / r."Количество")) AS maximum_of_units -- кол-во изготовлений лекарства
         FROM "Лекарства" AS l
                  JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
                  JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
                  JOIN component_stock AS cs ON r."Компоненты" = cs.Component_id
         WHERE l.Medicine_id = 4 -- кастомный id
         GROUP BY l.Medicine_id
     )
SELECT
    l.Medicine_id AS "id лекарства",
    l."Название" AS Лекарство,
    l."Тип" AS Тип,
    t."Описание_процесса" AS "способ приготовления",
    cs.Component_id AS "id компонента",
    cs.Name AS "компонент",
    r."Количество" AS "требуется на 1 ед.",
    cs.Price AS "цена компонента",
    cs.stock_quantity AS "остаток компонента, ед.",
    mu.maximum_of_units AS "можно приготовить, ед."
FROM "Лекарства" AS l
         LEFT JOIN "Технологические_карты" AS t ON l.Medicine_id = t.Medicine_id
         LEFT JOIN "Рецептуры" AS r ON t.Technology_id = r."Технологическая_карта"
         LEFT JOIN component_stock AS cs ON r."Компоненты" = cs.Component_id
         LEFT JOIN max_units AS mu ON l.Medicine_id = mu.Medicine_id
WHERE l.Medicine_id = 4 -- тот же кастомный id
ORDER BY cs.Component_id;