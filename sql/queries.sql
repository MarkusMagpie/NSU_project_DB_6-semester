SET search_path TO lab_drug_store;

TRUNCATE TABLE
    "Поставщики",
    "Компоненты",
    "Больные_клиенты",
    "Лекарства",
    "Готовые_лекарства",
    "Изготавливаемые_лекарства",
    "Технологические_карты",
    "Рецептуры",
    "Заявки_на_пополнение_компонентов",
    "Заявки_на_пополнение_готовых_лека",
    "Партии_компонентов",
    "Рецепты",
    "Заказы",
    "Резерв_компонентов"
    RESTART IDENTITY CASCADE;



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

-- общее количество таких заказов
SELECT COUNT(*) AS Не_забрали_заказы
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
WHERE o."Статус" = 'готов к выдаче'
  AND o."Время_изготовления" IS NOT NULL
  AND o."Время_изготовления" < CURRENT_TIMESTAMP - INTERVAL '1 hour';



-- 2.	Получить перечень и общее число покупателей, которые ждут прибытия на склад нужных им медикаментов в целом
-- и по указанной категории медикаментов.

-- перечень покупателей ждущих медикаменты
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

-- общее число покупателей с ожидающими заказами
SELECT COUNT(DISTINCT bc.Client_id) AS Количество_ожидающих_клиентов
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
WHERE o."Статус" = 'ожидание компонентов';

-- перечень покупателей ждущих медикаменты конкретного типа
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
WHERE o."Статус" = 'ожидание компонентов' AND l."Тип" = 'изготавливаемое'
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

-- общий объем
SELECT COALESCE(SUM(r.quantity_reserved), 0) AS Общий_объем
FROM "Резерв_компонентов" AS r
         JOIN "Заказы" AS o ON r.order_id = o.Order_id
WHERE o."Статус" IN ('выполнен', 'в производстве')
  AND o."Дата_создания" BETWEEN '2025-03-01' AND '2025-03-31';



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

-- общее число покупателей
SELECT COUNT(DISTINCT bc.Client_id) AS Количество_покупателей
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
WHERE o.Medicine_id = 1
  AND o."Дата_создания" BETWEEN '2026-03-01' AND '2026-03-07';

SELECT DISTINCT
    bc.Client_id AS ID_клиента,
    bc."ФИО" AS ФИО,
    bc."Телефон" AS Телефон,
    bc."Адрес" AS Адрес
FROM "Заказы" AS o
         JOIN "Рецепты" AS r ON o.Prescription_id = r.Prescription_id
         JOIN "Больные_клиенты" AS bc ON r.Client_id = bc.Client_id
         JOIN "Лекарства" AS l ON o.medicine_id = l.medicine_id
WHERE l."Тип" = 'изготавливаемое' AND o."Дата_создания" BETWEEN '2026-03-01' AND '2026-03-07';



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