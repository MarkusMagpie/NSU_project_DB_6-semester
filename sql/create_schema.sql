CREATE SCHEMA IF NOT EXISTS lab_drug_store;

SET search_path TO lab_drug_store;

-- DROP TABLE IF EXISTS Рецептуры CASCADE;
-- DROP TABLE IF EXISTS Технологические_карты CASCADE;
-- DROP TABLE IF EXISTS Изготавливаемые_лекарства CASCADE;
-- DROP TABLE IF EXISTS Готовые_лекарства CASCADE;
-- DROP TABLE IF EXISTS Лекарства CASCADE;
-- DROP TABLE IF EXISTS Заказы CASCADE;
-- DROP TABLE IF EXISTS Рецепты CASCADE;
-- DROP TABLE IF EXISTS Больные_клиенты CASCADE;
-- DROP TABLE IF EXISTS Партии_компонентов CASCADE;
-- DROP TABLE IF EXISTS Заявки_на_пополнение_компонентов CASCADE;
-- DROP TABLE IF EXISTS Заявки_на_пополнение_готовых_лекарств CASCADE;
-- DROP TABLE IF EXISTS Компоненты CASCADE;
-- DROP TABLE IF EXISTS Поставщики CASCADE;
-- DROP TABLE IF EXISTS Резерв_компонентов CASCADE;






CREATE TABLE IF NOT EXISTS Больные_клиенты (
    Client_id INT PRIMARY KEY,
    ФИО VARCHAR(100) NOT NULL,
    Телефон VARCHAR(20) UNIQUE NOT NULL,
    Адрес VARCHAR(100)
);

CREATE OR REPLACE FUNCTION check_phone_format() RETURNS TRIGGER AS $$
    BEGIN
        IF NEW.Телефон NOT LIKE '+7%' AND NEW.Телефон NOT LIKE '8%' THEN
            RAISE EXCEPTION 'Exception! Дан неверный формат телефона: "%". Номер должен начинаться с +7 или 8', NEW.Телефон;
            END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_check_phone_format
    BEFORE INSERT OR UPDATE ON Больные_клиенты
    FOR EACH ROW
    EXECUTE FUNCTION check_phone_format();

CREATE OR REPLACE PROCEDURE add_client(fio VARCHAR, phone VARCHAR, address VARCHAR) AS $$
    BEGIN
        INSERT INTO Больные_клиенты (ФИО, Телефон, Адрес)
        VALUES (fio, phone, address);
        END;
$$ LANGUAGE plpgsql;



CREATE TABLE IF NOT EXISTS Рецепты (
    Prescription_id integer PRIMARY KEY,
    Диагноз VARCHAR(200) NOT NULL,
    Наименование_лекарства VARCHAR(100) NOT NULL,
    Способ_применения VARCHAR(200) NOT NULL,
    Дата_выписки DATE NOT NULL,
    Количество_лекарства DECIMAL(10,2) NOT NULL,
    ФИО_врача VARCHAR(100) NOT NULL,
    Подпись_врача VARCHAR(100) NOT NULL,
    Печать_врача VARCHAR(100) NOT NULL,

    Client_id INT NOT NULL,

    CONSTRAINT FK_client_id FOREIGN KEY (Client_id)
        REFERENCES Больные_клиенты(Client_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT check_quantity CHECK (Количество_лекарства > 0),
    CONSTRAINT check_date CHECK (Дата_выписки <= CURRENT_DATE)
);



CREATE TABLE IF NOT EXISTS Лекарства (
    Medicine_id INT PRIMARY KEY,
    Название VARCHAR(100) NOT NULL UNIQUE,
    Тип VARCHAR(100) NOT NULL,
    Способ_применения VARCHAR(200) NOT NULL,
    Цена DECIMAL(10,2) NOT NULL,

    CONSTRAINT check_type CHECK (Тип IN ('готовое', 'изготавливаемое')),
    CONSTRAINT check_application CHECK (Способ_применения IN ('внутреннее', 'наружное', 'для смешивания')),
    CONSTRAINT check_price CHECK (Цена >= 0)
);

CREATE OR REPLACE FUNCTION add_medicine(
    name VARCHAR,
    type VARCHAR,
    application_method VARCHAR,
    price DECIMAL,

    manufacturer VARCHAR DEFAULT NULL, -- для готового
    dosage_form VARCHAR DEFAULT NULL, -- -//-

    compounded_type VARCHAR DEFAULT NULL, -- для изготавливаемого
    preparation_time INT DEFAULT NULL, -- -//-
    composition TEXT DEFAULT NULL -- -//-
)
    RETURNS INT AS $$
    DECLARE
        new_medicine_id INT;
    BEGIN
        IF type NOT IN ('готовое', 'изготавливаемое') THEN
            RAISE EXCEPTION 'Exception! Недопустимый тип лекарства: %', type;
        END IF;

        IF type = 'готовое' THEN
            IF manufacturer IS NULL OR dosage_form IS NULL THEN
                RAISE EXCEPTION 'Exception! Для готового лекарства необходимо указать производителя и форму выпуска';
            END IF;
        ELSE
            IF compounded_type IS NULL OR preparation_time IS NULL OR composition IS NULL THEN
                RAISE EXCEPTION 'Exception! Для изготавливаемого лекарства необходимо указать тип препарата, время приготовления и состав';
            END IF;
        END IF;

        -- вставка в супертип
        INSERT INTO Лекарства (Название, Тип, Способ_применения, Цена)
        VALUES (name, type, application_method, price)
        RETURNING Medicine_id INTO new_medicine_id;

        -- вставка в подтип
        IF type = 'готовое' THEN
            INSERT INTO Готовые_лекарства (Medicine_id, Производитель, Форма_выпуска)
            VALUES (new_medicine_id, manufacturer, dosage_form);
        ELSE
            INSERT INTO Изготавливаемые_лекарства (Medicine_id, Тип_препарата, Время_приготовления, Состав)
            VALUES (new_medicine_id, compounded_type, preparation_time, composition);
        END IF;

        RETURN new_medicine_id;
    END;
$$ LANGUAGE plpgsql;



CREATE TABLE IF NOT EXISTS Заказы (
    Order_id INT PRIMARY KEY,
    Дата_создания TIMESTAMP NOT NULL,
    Статус VARCHAR(50) NOT NULL,
    Время_изготовления TIMESTAMP,
    Цена DECIMAL(10,2) NOT NULL,

    Medicine_id INT NOT NULL,
    Prescription_id INT NOT NULL,

    CONSTRAINT FK_medicine_id FOREIGN KEY (Medicine_id)
        REFERENCES Лекарства(Medicine_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT FK_prescription_id FOREIGN KEY (Prescription_id)
        REFERENCES Рецепты(Prescription_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT check_price CHECK (Цена > 0),
    CONSTRAINT check_date CHECK (Дата_создания <= CURRENT_TIMESTAMP),
    CONSTRAINT check_status CHECK (Статус IN ('ожидание компонентов', 'готов к производству',
                                              'в производстве', 'готов к выдаче', 'выполнен'))
);

CREATE OR REPLACE FUNCTION set_order_initial_status() RETURNS TRIGGER AS $$
    DECLARE
        med_type VARCHAR;
        v_technology_id INT;
    BEGIN
        SELECT Тип INTO med_type
                   FROM Лекарства
                   WHERE Medicine_id = NEW.Medicine_id;

        IF med_type = 'изготавливаемое' THEN
            SELECT Technology_id INTO v_technology_id
                                 FROM Технологические_карты
                                 WHERE Medicine_id = NEW.Medicine_id;

            -- проверка каждого компонента
            PERFORM 1
            FROM Рецептуры r
            WHERE r.Технологическая_карта = v_technology_id
              -- для отсутствующего компонента нет записи в Партии_компонентов с Quantity > 0
              AND NOT EXISTS (
                SELECT 1
                FROM Партии_компонентов p
                WHERE p.Component_id = r.Компоненты AND p.Quantity > 0
              );

            IF FOUND THEN
                NEW.Статус := 'ожидание компонентов';
            ELSE
                NEW.Статус := 'готов к производству';
            END IF;
        ELSE
            -- готовое лекарство можно отдавать больному сразу
            NEW.Статус := 'готов к выдаче';
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_set_order_initial_status
    BEFORE INSERT ON Заказы
    FOR EACH ROW
    EXECUTE FUNCTION set_order_initial_status();



CREATE OR REPLACE FUNCTION auto_set_completion_time() RETURNS TRIGGER AS $$
    BEGIN
        IF NEW.Статус = 'готов к выдаче' AND OLD.Статус != 'готов к выдаче' THEN
            NEW.Время_изготовления := CURRENT_TIMESTAMP;
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_auto_set_completion_time
    BEFORE UPDATE ON Заказы
    FOR EACH ROW
    EXECUTE FUNCTION auto_set_completion_time();

CREATE OR REPLACE FUNCTION check_reserved_components() RETURNS TRIGGER AS $$
    DECLARE
        med_type VARCHAR;
        v_technology_id INT;
        component RECORD;
        reserved_amount DECIMAL;
    BEGIN
        IF NEW.Статус = 'в производстве' AND OLD.Статус != 'в производстве' THEN
            SELECT Тип INTO med_type
                       FROM Лекарства
                       WHERE Medicine_id = NEW.Medicine_id;

            IF med_type = 'изготавливаемое' THEN
                -- получаю тех карту
                SELECT Technology_id INTO v_technology_id
                FROM Технологические_карты
                WHERE Medicine_id = NEW.Medicine_id;

                IF v_technology_id IS NULL THEN
                    RAISE EXCEPTION 'Exception! Для изготавливаемого лекарства % не найдена технологическая карта', NEW.Medicine_id;
                END IF;

                -- перебор компонентов из рецептуры
                FOR component IN
                    SELECT r.Компоненты AS component_id, r.Количество AS required_amount
                    FROM Рецептуры r
                    WHERE r.Технологическая_карта = v_technology_id

                    LOOP
                    -- сумма зарезервированных компонентов для данного заказа
                    SELECT COALESCE(SUM(quantity_reserved), 0) INTO reserved_amount
                    FROM Резерв_компонентов
                    WHERE order_id = NEW.Order_id AND component_id = component.component_id;

                    IF reserved_amount < component.required_amount THEN
                        RAISE EXCEPTION 'Exception! Недостаточно зарезервировано компонента %: требуется %, зарезервировано %',
                            component.component_id, component.required_amount, reserved_amount;
                    END IF;
                END LOOP;
            END IF;
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_check_reserved_components
    BEFORE UPDATE ON Заказы
    FOR EACH ROW
    EXECUTE FUNCTION check_reserved_components();

-- списывает зарезервированные компоненты со склада в момент когда заказ переходит в производство
CREATE OR REPLACE FUNCTION consume_reserved_components() RETURNS TRIGGER AS $$
    DECLARE
        reservation RECORD; -- запись из Резерв_компонентов
        batch RECORD; -- из Партии_компонентов
        remaining DECIMAL; -- сколько еще нужно списать
    BEGIN
        IF NEW.Статус = 'в производстве' AND OLD.Статус != 'в производстве' THEN
            FOR reservation IN
                SELECT component_id, quantity_reserved
                FROM Резерв_компонентов
                WHERE order_id = NEW.Order_id

                LOOP
                    remaining := reservation.quantity_reserved;

                    -- поиск партии этого компонента
                    FOR batch IN
                        SELECT batch_id, Quantity
                        FROM Партии_компонентов
                        WHERE component_id = reservation.component_id AND Quantity > 0
                        ORDER BY receipt_date

                        LOOP
                            IF remaining <= 0 THEN
                                EXIT; -- все списано -> выход из цикла партий
                            END IF;

                            IF batch.Quantity >= remaining THEN
                                -- партия покрывает остаток
                                UPDATE Партии_компонентов
                                SET Quantity = Quantity - remaining
                                WHERE batch_id = batch.batch_id;
                                remaining := 0;
                            ELSE
                                -- списать всю партию
                                UPDATE Партии_компонентов
                                SET Quantity = 0
                                WHERE batch_id = batch.batch_id;
                                remaining := remaining - batch.Quantity;
                            END IF;
                        END LOOP;

                    -- после перебора всех партий осталось несписанное количество
                    IF remaining > 0 THEN
                        RAISE EXCEPTION 'Exception! Не хватает компонента % на складе', reservation.component_id;
                    END IF;

                END LOOP;

            -- после успешного списания удаляю резервы по заказу
            DELETE FROM Резерв_компонентов WHERE order_id = NEW.Order_id;
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_consume_reserved_components
    BEFORE UPDATE ON Заказы
    FOR EACH ROW
    EXECUTE FUNCTION consume_reserved_components();

CREATE OR REPLACE FUNCTION auto_reserve_components() RETURNS TRIGGER AS $$
    DECLARE
        med_type VARCHAR;
        v_technology_id INT;
        component RECORD;
    BEGIN
        SELECT Тип INTO med_type FROM Лекарства WHERE Medicine_id = NEW.Medicine_id;

        IF med_type = 'изготавливаемое' THEN
            SELECT Technology_id INTO v_technology_id
            FROM Технологические_карты
            WHERE Medicine_id = NEW.Medicine_id;

--             FOR component IN
--                 SELECT r.Компоненты AS component_id, r.Количество AS required
--                 FROM Рецептуры r
--                 WHERE r.Технологическая_карта = v_technology_id
--
--                 LOOP
--                     SELECT COALESCE(SUM(Quantity), 0) INTO available_amount
--                                                       FROM Партии_компонентов
--                                                       WHERE Component_id = component.component_id;
--
--                     IF available_amount < component.required THEN
--                         RAISE EXCEPTION 'Exception! Недостаточно компонента %: требуется %, доступно %',
--                             component.component_id, component.required, available_amount;
--                     END IF;
--                 END LOOP;

            -- создание резервов компоенентов
            FOR component IN
                SELECT r.Компоненты AS component_id, r.Количество AS required
                FROM Рецептуры r
                WHERE r.Технологическая_карта = v_technology_id

                LOOP
                    INSERT INTO Резерв_компонентов (order_id, component_id, quantity_reserved)
                    VALUES (NEW.Order_id, component.component_id, component.required);
                END LOOP;
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_auto_reserve_components
    AFTER INSERT ON Заказы
    FOR EACH ROW
    EXECUTE FUNCTION auto_reserve_components();



CREATE TABLE IF NOT EXISTS Поставщики (
    Supplier_id INT PRIMARY KEY,
    Full_name VARCHAR(100) NOT NULL,
    Phone VARCHAR(20) NOT NULL,
    Email VARCHAR(100) NOT NULL,

    CONSTRAINT unique_supplier_phone UNIQUE (Phone),
    CONSTRAINT unique_supplier_email UNIQUE (Email)
);



CREATE TABLE IF NOT EXISTS Компоненты (
    Component_id INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Shelf_life INT NOT NULL CHECK (Shelf_life > 0), -- в днях/месяцах
    Critical_level DECIMAL(10,2) NOT NULL CHECK (Critical_level >= 0),

    CONSTRAINT unique_component_name UNIQUE (Name)
);



CREATE TABLE IF NOT EXISTS Заявки_на_пополнение_компонентов (
    Component_request_id SERIAL PRIMARY KEY,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    Status VARCHAR(100) NOT NULL,
    Supplier_id INT NOT NULL,
    Component_id INT NOT NULL,

    CONSTRAINT FK_component_request_supplier FOREIGN KEY (Supplier_id)
        REFERENCES Поставщики(Supplier_id)
        ON DELETE RESTRICT,
    CONSTRAINT FK_request_component FOREIGN KEY (Component_id)
        REFERENCES Компоненты(Component_id)
        ON DELETE RESTRICT,
    CONSTRAINT check_status CHECK (Status IN ('новая', 'отправлена', 'получена'))
);



CREATE TABLE IF NOT EXISTS Готовые_лекарства (
    Medicine_id INT PRIMARY KEY,
    Производитель VARCHAR(200) NOT NULL,
    Форма_выпуска VARCHAR(100) NOT NULL,

    CONSTRAINT FK_ready_medicine FOREIGN KEY (Medicine_id)
        REFERENCES Лекарства(Medicine_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT check_dosage_form CHECK (Форма_выпуска IN ('таблетки', 'мази', 'настойки'))
);



CREATE TABLE IF NOT EXISTS Заявки_на_пополнение_готовых_лекарств (
    Medicine_request_id SERIAL PRIMARY KEY,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    Status VARCHAR(100) NOT NULL,
    Supplier_id INT NOT NULL,
    Medicine_id INT NOT NULL,

    CONSTRAINT FK_medicine_request_supplier FOREIGN KEY (Supplier_id)
        REFERENCES Поставщики(Supplier_id)
        ON DELETE RESTRICT,
    CONSTRAINT FK_request_medicine FOREIGN KEY (Medicine_id)
        REFERENCES Готовые_лекарства(Medicine_id)
        ON DELETE RESTRICT,
    CONSTRAINT check_medicine_status CHECK
        (Status IN ('новая','отправлена', 'получена'))
);



CREATE TABLE IF NOT EXISTS Изготавливаемые_лекарства (
    Medicine_id INT PRIMARY KEY,
    Состав TEXT NOT NULL,
    Время_приготовления INT NOT NULL,
    Тип_препарата VARCHAR(100) NOT NULL,

    CONSTRAINT FK_compounded_medicine FOREIGN KEY (Medicine_id)
        REFERENCES Лекарства(Medicine_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT check_prep_time CHECK (Время_приготовления > 0),
    CONSTRAINT check_compounded_type CHECK (Тип_препарата IN ('микстура', 'мазь', 'раствор', 'настойка', 'порошок'))
);

CREATE OR REPLACE FUNCTION check_application_method_consistency()
    RETURNS TRIGGER AS $$
    DECLARE
        app_method VARCHAR;
    BEGIN
        SELECT Способ_применения INTO app_method
                                 FROM Лекарства
                                 WHERE Medicine_id = NEW.Medicine_id;

        IF app_method IS NULL THEN
            RAISE EXCEPTION 'Exception! Лекарство с идентификатором % не существует', NEW.Medicine_id;
        END IF;

        IF NEW.Тип_препарата IN ('микстура', 'порошок') AND app_method != 'внутреннее' THEN
            RAISE EXCEPTION 'Exception! Для препарата типа "%" способ применения должен быть "внутреннее", а указан "%"',
                NEW.Тип_препарата, app_method;
        END IF;

        IF NEW.Тип_препарата = 'мазь' AND app_method != 'наружное' THEN
            RAISE EXCEPTION 'Exception! Для мази способ применения должен быть "наружное", а указан "%"', app_method;
        END IF;

        IF NEW.Тип_препарата = 'раствор' AND app_method NOT IN ('внутреннее', 'наружное', 'для смешивания') THEN
            RAISE EXCEPTION 'Exception! Для раствора способ применения должен быть одним из: внутреннее, наружное, для смешивания, а указан "%"',
                app_method;
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_check_application_method_consistency
    BEFORE INSERT OR UPDATE ON Изготавливаемые_лекарства
    FOR EACH ROW
    EXECUTE FUNCTION check_application_method_consistency();



CREATE TABLE IF NOT EXISTS Технологические_карты (
    Technology_id INT PRIMARY KEY,
    Название VARCHAR(100) NOT NULL,
    Описание_процесса VARCHAR(200) NOT NULL,
    Medicine_id INT NOT NULL UNIQUE,

    CONSTRAINT FK_technology_medicine FOREIGN KEY (Medicine_id)
        REFERENCES Изготавливаемые_лекарства(Medicine_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT check_tech_name CHECK (LENGTH(Название) > 0),
    CONSTRAINT check_tech_desc CHECK (LENGTH(Описание_процесса) > 0)
);



CREATE TABLE IF NOT EXISTS Партии_компонентов (
    Batch_id INT PRIMARY KEY,
    Receipt_date DATE NOT NULL,
    Quantity DECIMAL(10,2) NOT NULL CHECK (Quantity >= 0),

    Component_request_id INT NOT NULL,
    Component_id INT NOT NULL,

    CONSTRAINT FK_batch_request FOREIGN KEY (Component_request_id)
        REFERENCES Заявки_на_пополнение_компонентов(Component_request_id)
        ON DELETE RESTRICT,
    CONSTRAINT FK_batch_component FOREIGN KEY (Component_id)
        REFERENCES Компоненты(Component_id)
        ON DELETE RESTRICT,
    CONSTRAINT check_receipt_date CHECK (Receipt_date <= CURRENT_DATE)
);

CREATE OR REPLACE FUNCTION check_critical_level() RETURNS TRIGGER AS $$
    DECLARE
        total_quantity DECIMAL;
        critical DECIMAL;
        existing_request INT;
    BEGIN
        -- общее количество компонента
        SELECT COALESCE(SUM(Quantity), 0) INTO total_quantity
        FROM Партии_компонентов
        WHERE Component_id = NEW.Component_id;

        SELECT Critical_level INTO critical
        FROM Компоненты
        WHERE Component_id = NEW.Component_id;

        IF total_quantity <= critical THEN
            -- триггер может создавать несколько заявок если остаток долго находится ниже critical_level
            SELECT Component_request_id INTO existing_request
            FROM Заявки_на_пополнение_компонентов
            WHERE Component_id = NEW.Component_id AND Status IN ('новая', 'отправлена')
            LIMIT 1;

            IF existing_request IS NULL THEN
                INSERT INTO Заявки_на_пополнение_компонентов (Component_id, Quantity, Status, Supplier_id)
                VALUES (NEW.Component_id, critical * 2, 'новая', 1);
            END IF;
        END IF;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_check_critical_level
    AFTER INSERT OR UPDATE OF Quantity ON Партии_компонентов
    FOR EACH ROW
    EXECUTE FUNCTION check_critical_level();

-- при добавлении новой партии:
-- есть ли заказы в статусе 'ожидание компонентов', которые теперь могут быть выполнены,
-- перевожу их в 'готов к производству'
CREATE OR REPLACE FUNCTION update_waiting_orders() RETURNS TRIGGER AS $$
    DECLARE
        order_rec RECORD;
    BEGIN
        -- для каждого заказа в 'ожидании компонентов' который включает этот компонент
        FOR order_rec IN
            SELECT DISTINCT o.Order_id, t.Technology_id
            FROM Заказы AS o
                JOIN Лекарства AS l ON o.Medicine_id = l.Medicine_id
                JOIN Технологические_карты AS t ON l.Medicine_id = t.Medicine_id
            WHERE o.Статус = 'ожидание компонентов' AND EXISTS (
                SELECT 1 FROM Рецептуры AS r
                -- в рецептуре r этого заказа o есть компонент который только что поступил
                WHERE r.Технологическая_карта = t.Technology_id AND r.Компоненты = NEW.Component_id
            )

            LOOP
                -- у каждого найденного заказа: все ли его компоненты теперь доступны?
                -- <=> нет ни одного компонента в рецептуре для которого не существует партии с >0 количеством
                IF NOT EXISTS (
                    SELECT 1
                    FROM Рецептуры r
                    WHERE r.Технологическая_карта = order_rec.Technology_id
                      -- нет ли хотя бы одной партия с положительным остатком для конкретного компонента?
                      AND NOT EXISTS (
                        SELECT 1
                        FROM Партии_компонентов p
                        WHERE p.Component_id = r.Компоненты AND p.Quantity > 0
                    )
                ) THEN
                    UPDATE Заказы SET Статус = 'готов к производству' WHERE Order_id = order_rec.Order_id;
                END IF;
            END LOOP;

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_update_waiting_orders
    AFTER INSERT ON Партии_компонентов
    FOR EACH ROW
    EXECUTE FUNCTION update_waiting_orders();



CREATE TABLE IF NOT EXISTS Рецептуры (
    Formula_id INT PRIMARY KEY,
    Этап VARCHAR(100) NOT NULL,
    Количество DECIMAL(10,2) NOT NULL,

    Технологическая_карта INT NOT NULL,
    Компоненты INT NOT NULL,

    CONSTRAINT FK_recipe_technology FOREIGN KEY (Технологическая_карта)
        REFERENCES Технологические_карты(Technology_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_recipe_component FOREIGN KEY (Компоненты)
        REFERENCES Компоненты(Component_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT check_quantity CHECK (Количество > 0)
);



-- какие компоненты и в каком количестве зарезервированы под конкретный заказ
CREATE TABLE IF NOT EXISTS Резерв_компонентов (
    reservation_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    component_id INT NOT NULL,
    quantity_reserved DECIMAL(10,2) NOT NULL CHECK (quantity_reserved > 0),

    CONSTRAINT FK_reservation_order FOREIGN KEY (order_id)
        REFERENCES Заказы(Order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_reservation_component FOREIGN KEY (component_id)
        REFERENCES Компоненты(Component_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);