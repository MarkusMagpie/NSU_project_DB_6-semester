SET search_path TO lab_drug_store;

-- DELETE FROM "Партии_компонентов";
TRUNCATE "Заявки_на_пополнение_компонентов" RESTART IDENTITY CASCADE;
TRUNCATE "Партии_компонентов" RESTART IDENTITY CASCADE;

INSERT INTO "Партии_компонентов" (Batch_id, Receipt_date, Quantity, Component_request_id, Component_id)
VALUES
    (1, '2025-01-10', 5000.0, 1, 1),
    (2, '2025-01-15', 2000.0, 2, 2),
    (3, '2025-02-01', 1000.0, 3, 3),
    (4, '2025-02-10', 500.0,  4, 4),
    (5, '2025-02-11', 500.0,  5, 5),
    (6, '2025-02-12', 2000.0, 6, 6),
    (7, '2025-02-13', 500.0,  7, 7);