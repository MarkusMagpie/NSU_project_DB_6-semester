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



-- 10.	Получить все технологии приготовления лекарств указанных типов,
-- конкретных лекарств,
-- лекарств, находящихся в справочнике заказов в производстве.
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



-- технологии для лекарств указанного типа лекарств
SELECT
    t.*,
    l."Название" AS Лекарство
FROM "Технологические_карты" AS t
    JOIN "Лекарства" AS l ON t.Medicine_id = l.Medicine_id
WHERE l."Тип" = 'изготавливаемое';

-- технологии для конкретных лекарств по id
SELECT
    t.*,
    l."Название" AS Лекарство
FROM "Технологические_карты" AS t
    JOIN "Лекарства" AS l ON t.Medicine_id = l.Medicine_id
WHERE l.Medicine_id IN (2,5);

-- технологии для лекарств находящихся в статусе 'в производстве' в таблице Заказы
SELECT
    t.*,
    l."Название" AS Лекарство
FROM "Технологические_карты" AS t
    JOIN "Лекарства" AS l ON t.Medicine_id = l.Medicine_id
    JOIN "Заказы" AS o ON l.Medicine_id = o.Medicine_id
WHERE o."Статус" = 'в производстве';