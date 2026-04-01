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