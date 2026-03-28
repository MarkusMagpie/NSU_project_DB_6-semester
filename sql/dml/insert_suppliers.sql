SET search_path TO lab_drug_store;

INSERT INTO lab_drug_store."Поставщики" (Supplier_id, Full_name, Phone, Email)
VALUES
    (1, 'ООО "ФармСнаб"', '+74951234567', 'info@farmsnab.ru'),
    (2, 'ЗАО "МедТорг"', '+74957654321', 'sales@medtorg.ru');