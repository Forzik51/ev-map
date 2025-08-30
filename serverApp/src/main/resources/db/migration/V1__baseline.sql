-- Last modification date: 2025-08-30 18:44:53.416

-- tables
-- Table: Chat
CREATE TABLE Chat (
                      id bigint  NOT NULL,
                      nazwa varchar(20)  NOT NULL,
                      CONSTRAINT Chat_pk PRIMARY KEY (id)
);

-- Table: Decyzja
CREATE TABLE Decyzja (
                         Skarga_id bigint  NOT NULL,
                         id bigint  NOT NULL,
                         opis varchar(100)  NOT NULL,
                         blokowanie timestamp  NOT NULL,
                         Moderator_Osoba_id bigint  NOT NULL,
                         CONSTRAINT Decyzja_pk PRIMARY KEY (id)
);

-- Table: Firma
CREATE TABLE Firma (
                       id bigint  NOT NULL,
                       nazwa varchar(30)  NOT NULL,
                       link varchar(50)  NOT NULL,
                       Uzytkownik_id bigint  NOT NULL,
                       CONSTRAINT Firma_pk PRIMARY KEY (id)
);

-- Table: Kategoria
CREATE TABLE Kategoria (
                           id bigint  NOT NULL,
                           nazwa varchar(20)  NOT NULL,
                           CONSTRAINT Kategoria_pk PRIMARY KEY (id)
);

-- Table: Kategoria_Skarga
CREATE TABLE Kategoria_Skarga (
                                  id bigint  NOT NULL,
                                  nazwa varchar(30)  NOT NULL,
                                  CONSTRAINT Kategoria_Skarga_pk PRIMARY KEY (id)
);

-- Table: Kategoria_lokalizacji
CREATE TABLE Kategoria_lokalizacji (
                                       id bigint  NOT NULL,
                                       nazwa varchar(20)  NOT NULL,
                                       CONSTRAINT Kategoria_lokalizacji_pk PRIMARY KEY (id)
);

-- Table: Kategoria_wydarzenie
CREATE TABLE Kategoria_wydarzenie (
                                      id bigint  NOT NULL,
                                      Kategoria_id bigint  NOT NULL,
                                      Wydarzenie_id bigint  NOT NULL,
                                      CONSTRAINT Kategoria_wydarzenie_pk PRIMARY KEY (id)
);

-- Table: Komentarz
CREATE TABLE Komentarz (
                           id bigint  NOT NULL,
                           text varchar(100)  NOT NULL,
                           Wydarzenie_id bigint  NOT NULL,
                           Komentarz_id bigint  NOT NULL,
                           Uzytkownik_id bigint  NOT NULL,
                           CONSTRAINT Komentarz_pk PRIMARY KEY (id)
);

-- Table: Lista_upodobanych
CREATE TABLE Lista_upodobanych (
                                   id bigint  NOT NULL,
                                   nazwa varchar(20)  NOT NULL,
                                   Wydarzenie_id bigint  NOT NULL,
                                   Uzytkownik_id bigint  NOT NULL,
                                   CONSTRAINT Lista_upodobanych_pk PRIMARY KEY (id)
);

-- Table: Lokalizacja
CREATE TABLE Lokalizacja (
                             id bigint  NOT NULL,
                             loklizacja_punkt geometry(Point, 4326)  NOT NULL,
                             loklizacja_obszar geometry(Polygon, 4326)  NOT NULL,
                             Kategoria_lokalizacji_id bigint  NOT NULL,
                             CONSTRAINT Lokalizacja_pk PRIMARY KEY (id)
);

-- Table: Moderator
CREATE TABLE Moderator (
                           Osoba_id bigint  NOT NULL,
                           login varchar(20)  NOT NULL,
                           haslo varchar(20)  NOT NULL,
                           iloszcz_decyzij smallint  NOT NULL,
                           CONSTRAINT Moderator_pk PRIMARY KEY (Osoba_id)
);

-- Table: Obserwowanie
CREATE TABLE Obserwowanie (
                              id bigint  NOT NULL,
                              Uzytkownik_id bigint  NOT NULL,
                              Uzytkownik_2_id bigint  NOT NULL,
                              CONSTRAINT Obserwowanie_pk PRIMARY KEY (id)
);

-- Table: Ocena
CREATE TABLE Ocena (
                       id bigint  NOT NULL,
                       ocena decimal(2,1)  NOT NULL,
                       Wydarzenie_id bigint  NOT NULL,
                       Uzytkownik_id bigint  NOT NULL,
                       CONSTRAINT Ocena_pk PRIMARY KEY (id)
);

-- Table: Osoba
CREATE TABLE Osoba (
                       id bigint  NOT NULL,
                       imie varchar(20)  NOT NULL,
                       nazwisko varchar(20)  NOT NULL,
                       email varchar(30)  NOT NULL,
                       telefon varchar(15)  NOT NULL,
                       data_urodzenia date  NOT NULL,
                       CONSTRAINT Osoba_pk PRIMARY KEY (id)
);

-- Table: Powiadomienie
CREATE TABLE Powiadomienie (
                               id bigint  NOT NULL,
                               Uzytkownik_Chat_id bigint  NOT NULL,
                               text varchar(50)  NOT NULL,
                               data timestamp  NOT NULL,
                               CONSTRAINT Powiadomienie_pk PRIMARY KEY (id)
);

-- Table: Promowanie
CREATE TABLE Promowanie (
                            id bigint  NOT NULL,
                            data_poczatku timestamp  NOT NULL,
                            data_konca timestamp  NOT NULL,
                            Wydarzenie_id bigint  NOT NULL,
                            Firma_id bigint  NOT NULL,
                            CONSTRAINT Promowanie_pk PRIMARY KEY (id)
);

-- Table: Repost
CREATE TABLE Repost (
                        id bigint  NOT NULL,
                        Wydarzenie_id bigint  NOT NULL,
                        Uzytkownik_id bigint  NOT NULL,
                        CONSTRAINT Repost_pk PRIMARY KEY (id)
);

-- Table: Skarga
CREATE TABLE Skarga (
                        id bigint  NOT NULL,
                        opis varchar(100)  NOT NULL,
                        Kategoria_Skarga_id bigint  NOT NULL,
                        Wydarzenie_id bigint  NOT NULL,
                        Uzytkownik_id bigint  NOT NULL,
                        CONSTRAINT Skarga_pk PRIMARY KEY (id)
);

-- Table: Uzytkownik
CREATE TABLE Uzytkownik (
                            Osoba_id bigint  NOT NULL,
                            username varchar(20)  NOT NULL,
                            opis_strony varchar(100)  NOT NULL,
                            haslo varchar(30)  NOT NULL,
                            CONSTRAINT Uzytkownik_pk PRIMARY KEY (Osoba_id)
);

-- Table: Uzytkownik_Chat
CREATE TABLE Uzytkownik_Chat (
                                 id bigint  NOT NULL,
                                 Chat_id bigint  NOT NULL,
                                 Uzytkownik_id2 bigint  NOT NULL,
                                 Uzytkownik_id1 bigint  NOT NULL,
                                 CONSTRAINT Uzytkownik_Chat_pk PRIMARY KEY (id)
);

-- Table: Wydarzenie
CREATE TABLE Wydarzenie (
                            id bigint  NOT NULL,
                            nazwa varchar(30)  NOT NULL,
                            kiedy timestamp  NOT NULL,
                            opis varchar(500)  NOT NULL,
                            Lokalizacja_id bigint  NOT NULL,
                            CONSTRAINT Wydarzenie_pk PRIMARY KEY (id)
);

-- Table: Zdjecie
CREATE TABLE Zdjecie (
                         id bigint  NOT NULL,
                         image_path varchar(255)  NOT NULL,
                         Wydarzenie_id bigint  NOT NULL,
                         CONSTRAINT Zdjecie_pk PRIMARY KEY (id)
);

-- foreign keys
-- Reference: Decyzja_Moderator (table: Decyzja)
ALTER TABLE Decyzja ADD CONSTRAINT Decyzja_Moderator
    FOREIGN KEY (Moderator_Osoba_id)
        REFERENCES Moderator (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Firma_Uzytkownik (table: Firma)
ALTER TABLE Firma ADD CONSTRAINT Firma_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Kategoria_wydarzenie (table: Kategoria_wydarzenie)
ALTER TABLE Kategoria_wydarzenie ADD CONSTRAINT Kategoria_wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Komentarz_Komentarz (table: Komentarz)
ALTER TABLE Komentarz ADD CONSTRAINT Komentarz_Komentarz
    FOREIGN KEY (Komentarz_id)
        REFERENCES Komentarz (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Komentarz_Uzytkownik (table: Komentarz)
ALTER TABLE Komentarz ADD CONSTRAINT Komentarz_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Komentarz_Wydarzenie (table: Komentarz)
ALTER TABLE Komentarz ADD CONSTRAINT Komentarz_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Lista_upodobanych_Uzytkownik (table: Lista_upodobanych)
ALTER TABLE Lista_upodobanych ADD CONSTRAINT Lista_upodobanych_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Lista_upodobanych_Wydarzenie (table: Lista_upodobanych)
ALTER TABLE Lista_upodobanych ADD CONSTRAINT Lista_upodobanych_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Lokalizacja_Kategoria_lokalizacji (table: Lokalizacja)
ALTER TABLE Lokalizacja ADD CONSTRAINT Lokalizacja_Kategoria_lokalizacji
    FOREIGN KEY (Kategoria_lokalizacji_id)
        REFERENCES Kategoria_lokalizacji (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Moderator_Osoba (table: Moderator)
ALTER TABLE Moderator ADD CONSTRAINT Moderator_Osoba
    FOREIGN KEY (Osoba_id)
        REFERENCES Osoba (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Obserwowanie_Uzytkownik (table: Obserwowanie)
ALTER TABLE Obserwowanie ADD CONSTRAINT Obserwowanie_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Obserwowanie_Uzytkownik_2 (table: Obserwowanie)
ALTER TABLE Obserwowanie ADD CONSTRAINT Obserwowanie_Uzytkownik_2
    FOREIGN KEY (Uzytkownik_2_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Ocena_Uzytkownik (table: Ocena)
ALTER TABLE Ocena ADD CONSTRAINT Ocena_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Ocena_Wydarzenie (table: Ocena)
ALTER TABLE Ocena ADD CONSTRAINT Ocena_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Promowanie_Firma (table: Promowanie)
ALTER TABLE Promowanie ADD CONSTRAINT Promowanie_Firma
    FOREIGN KEY (Firma_id)
        REFERENCES Firma (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Promowanie_Wydarzenie (table: Promowanie)
ALTER TABLE Promowanie ADD CONSTRAINT Promowanie_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Repost_Uzytkownik (table: Repost)
ALTER TABLE Repost ADD CONSTRAINT Repost_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Repost_Wydarzenie (table: Repost)
ALTER TABLE Repost ADD CONSTRAINT Repost_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Skarga_Kategoria_Skarga (table: Skarga)
ALTER TABLE Skarga ADD CONSTRAINT Skarga_Kategoria_Skarga
    FOREIGN KEY (Kategoria_Skarga_id)
        REFERENCES Kategoria_Skarga (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Skarga_Uzytkownik (table: Skarga)
ALTER TABLE Skarga ADD CONSTRAINT Skarga_Uzytkownik
    FOREIGN KEY (Uzytkownik_id)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Skarga_Wydarzenie (table: Skarga)
ALTER TABLE Skarga ADD CONSTRAINT Skarga_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Table_11_Uzytkownik_Chat (table: Powiadomienie)
ALTER TABLE Powiadomienie ADD CONSTRAINT Table_11_Uzytkownik_Chat
    FOREIGN KEY (Uzytkownik_Chat_id)
        REFERENCES Uzytkownik_Chat (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Table_8_Skarga (table: Decyzja)
ALTER TABLE Decyzja ADD CONSTRAINT Table_8_Skarga
    FOREIGN KEY (Skarga_id)
        REFERENCES Skarga (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Uzytkownik_Chat_Chat (table: Uzytkownik_Chat)
ALTER TABLE Uzytkownik_Chat ADD CONSTRAINT Uzytkownik_Chat_Chat
    FOREIGN KEY (Chat_id)
        REFERENCES Chat (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Uzytkownik_Chat_id1 (table: Uzytkownik_Chat)
ALTER TABLE Uzytkownik_Chat ADD CONSTRAINT Uzytkownik_Chat_id1
    FOREIGN KEY (Uzytkownik_id2)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Uzytkownik_Chat_id2 (table: Uzytkownik_Chat)
ALTER TABLE Uzytkownik_Chat ADD CONSTRAINT Uzytkownik_Chat_id2
    FOREIGN KEY (Uzytkownik_id1)
        REFERENCES Uzytkownik (Osoba_id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Uzytkownik_Osoba (table: Uzytkownik)
ALTER TABLE Uzytkownik ADD CONSTRAINT Uzytkownik_Osoba
    FOREIGN KEY (Osoba_id)
        REFERENCES Osoba (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Wydarzenie_Lokalizacja (table: Wydarzenie)
ALTER TABLE Wydarzenie ADD CONSTRAINT Wydarzenie_Lokalizacja
    FOREIGN KEY (Lokalizacja_id)
        REFERENCES Lokalizacja (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: Zdjecie_Wydarzenie (table: Zdjecie)
ALTER TABLE Zdjecie ADD CONSTRAINT Zdjecie_Wydarzenie
    FOREIGN KEY (Wydarzenie_id)
        REFERENCES Wydarzenie (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- Reference: _Kategoria (table: Kategoria_wydarzenie)
ALTER TABLE Kategoria_wydarzenie ADD CONSTRAINT _Kategoria
    FOREIGN KEY (Kategoria_id)
        REFERENCES Kategoria (id)
        NOT DEFERRABLE
            INITIALLY IMMEDIATE
;

-- End of file.

