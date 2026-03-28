SET search_path TO lab_drug_store;


-- 1 Получить сведения о покупателях, которые не пришли забрать свой заказ в назначенное им время и общее их число.
SELECT
    bc.Client_id AS "ID клиента",
    bc.ФИО AS "ФИО",
    bc.Телефон AS "Телефон",
    bc.Адрес AS "Адрес",
    o.Order_id AS "Номер заказа",
    o.Время_изготовления AS "Время готовности",
    CURRENT_TIMESTAMP - o.Время_изготовления AS "Просрочка"
FROM "Заказы" o
         JOIN "Больные_клиенты" bc ON o.Client_id = bc.Client_id
WHERE o.Статус = 'готов к выдаче'
  AND o.Время_изготовления IS NOT NULL
  AND o.Время_изготовления < CURRENT_TIMESTAMP - INTERVAL '1 hour'
ORDER BY o.Время_изготовления;