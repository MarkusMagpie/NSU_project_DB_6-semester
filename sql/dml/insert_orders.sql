SET search_path TO lab_drug_store;

-- заказ на готовое лекарство - Парацетамол
INSERT INTO "Заказы" (Order_id, "Дата_создания", "Статус", "Время_изготовления", "Цена", Medicine_id)
VALUES (1, CURRENT_TIMESTAMP, NULL, NULL, 150.00, 1);
-- 'готов к выдаче'

-- заказ на изготавливаемое лекарство - микстура от кашля – все компоненты есть
INSERT INTO "Заказы" (Order_id, "Дата_создания", "Статус", "Время_изготовления", "Цена", Medicine_id)
VALUES (2, CURRENT_TIMESTAMP, NULL, NULL, 200.00, 4);
-- trg_set_order_initial_status должен установить статус 'готов к производству' и создать резервы компонентов