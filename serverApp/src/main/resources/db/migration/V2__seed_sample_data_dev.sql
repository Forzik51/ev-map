
CREATE EXTENSION IF NOT EXISTS postgis;

BEGIN;

-- Delete all
TRUNCATE TABLE
    decyzja, firma, kategoria_wydarzenie, kategoria, kategoria_skarga,
  komentarz, lista_upodobanych, lokalizacja, moderator, obserwowanie,
  ocena, powiadomienie, promowanie, repost, skarga, uzytkownik_chat,
  uzytkownik, wydarzenie, zdjecie, chat, osoba, kategoria_lokalizacji
RESTART IDENTITY CASCADE;

INSERT INTO chat (id, nazwa) VALUES
                                 (1, 'Ogólny'),
                                 (2, 'Eventy');

INSERT INTO kategoria (id, nazwa) VALUES
                                      (1, 'Koncert'),
                                      (2, 'Sport'),
                                      (3, 'Sztuka');

INSERT INTO kategoria_skarga (id, nazwa) VALUES
                                             (1, 'Spam'),
                                             (2, 'Naruszenie regulaminu');


INSERT INTO kategoria_lokalizacji (id, nazwa) VALUES
                                                  (1, 'Park'),
                                                  (2, 'Stadion'),
                                                  (3, 'Galeria sztuki');

INSERT INTO osoba (id, imie, nazwisko, email, telefon, data_urodzenia) VALUES
                                                                           (1, 'Jan',   'Kowalski',    'jan.kowalski@example.com',  '+48 600 000 001', '1990-01-15'),
                                                                           (2, 'Anna',  'Nowak',       'anna.nowak@example.com',     '+48 600 000 002', '1992-05-20'),
                                                                           (3, 'Piotr', 'Zieliński',   'piotr.z@example.com',        '+48 600 000 003', '1988-09-10'),
                                                                           (4, 'Marta', 'Lewandowska', 'marta.lew@example.com',      '+48 600 000 004', '1995-03-12'),
                                                                           (5, 'Adam',  'Moderator',   'moderator@example.com',      '+48 600 000 005', '1985-07-07');

INSERT INTO uzytkownik (osoba_id, username, opis_strony, haslo) VALUES
                                                                    (1, 'janek',  'Miłośnik koncertów i biegów.', 'pass123'),
                                                                    (2, 'ania',   'Lubię sztukę i parki.',        'pass123'),
                                                                    (3, 'piotr',  'Biegacz amator.',              'pass123'),
                                                                    (4, 'marta',  'Fotografuję wydarzenia.',      'pass123');


INSERT INTO moderator (osoba_id, login, haslo, iloszcz_decyzij) VALUES
    (5, 'mod1', 'modpass', 0);


INSERT INTO lokalizacja (id, loklizacja_punkt, loklizacja_obszar, kategoria_lokalizacji_id) VALUES
                                                                                                (1,
                                                                                                 ST_SetSRID(ST_MakePoint(21.0122, 52.2297), 4326),
                                                                                                 ST_GeomFromText('POLYGON((21.0110 52.2290,21.0130 52.2290,21.0130 52.2310,21.0110 52.2310,21.0110 52.2290))',4326),
                                                                                                 1),
                                                                                                (2,
                                                                                                 ST_SetSRID(ST_MakePoint(21.0470, 52.2319), 4326),
                                                                                                 ST_GeomFromText('POLYGON((21.0455 52.2310,21.0485 52.2310,21.0485 52.2330,21.0455 52.2330,21.0455 52.2310))',4326),
                                                                                                 2),
                                                                                                (3,
                                                                                                 ST_SetSRID(ST_MakePoint(21.0190, 52.2320), 4326),
                                                                                                 ST_GeomFromText('POLYGON((21.0180 52.2314,21.0200 52.2314,21.0200 52.2326,21.0180 52.2326,21.0180 52.2314))',4326),
                                                                                                 3);

INSERT INTO wydarzenie (id, nazwa, kiedy, opis, lokalizacja_id) VALUES
                                                                    (1, 'Koncert w parku',    '2025-09-05 18:00:00', 'Plenerowy koncert muzyki filmowej.', 1),
                                                                    (2, 'Bieg na 5 km',       '2025-09-12 10:00:00', 'Trasa po bulwarach wiślanych.',      2),
                                                                    (3, 'Wystawa fotografii', '2025-10-01 12:00:00', 'Wystawa lokalnych fotografów.',      3);

INSERT INTO kategoria_wydarzenie (id, kategoria_id, wydarzenie_id) VALUES
                                                                       (1, 1, 1),  -- Koncert
                                                                       (2, 2, 2),  -- Sport
                                                                       (3, 3, 3);  -- Sztuka

INSERT INTO zdjecie (id, image_path, wydarzenie_id) VALUES
                                                        (1, '/images/events/1.jpg', 1),
                                                        (2, '/images/events/2.jpg', 2),
                                                        (3, '/images/events/3.jpg', 3);

INSERT INTO obserwowanie (id, uzytkownik_id, uzytkownik_2_id) VALUES
                                                                  (1, 1, 2),
                                                                  (2, 2, 1),
                                                                  (3, 3, 1);

INSERT INTO lista_upodobanych (id, nazwa, wydarzenie_id, uzytkownik_id) VALUES
                                                                            (1, 'Ulubione Jana', 1, 1),
                                                                            (2, 'Ulubione Ani',  3, 2),
                                                                            (3, 'Moje biegi',    2, 3);

INSERT INTO ocena (id, ocena, wydarzenie_id, uzytkownik_id) VALUES
                                                                (1, 4.5, 1, 1),
                                                                (2, 3.0, 2, 2),
                                                                (3, 5.0, 3, 4);

INSERT INTO komentarz (id, text, wydarzenie_id, komentarz_id, uzytkownik_id) VALUES
                                                                                 (1, 'Super klimat!',               1, 1, 2),
                                                                                 (2, 'Kto biegnie jutro?',          2, 1, 3),
                                                                                 (3, 'Też byłem, było świetnie.',   1, 1,    1);  -- odpowiedź do (1)


INSERT INTO repost (id, wydarzenie_id, uzytkownik_id) VALUES
                                                          (1, 1, 2),
                                                          (2, 3, 1);

INSERT INTO uzytkownik_chat (id, chat_id, uzytkownik_id2, uzytkownik_id1) VALUES
                                                                              (1, 1, 2, 1),
                                                                              (2, 2, 1, 3);

INSERT INTO powiadomienie (id, uzytkownik_chat_id, text, data) VALUES
                                                                   (1, 1, 'Nowa wiadomość od ania',       '2025-08-30 10:00:00'),
                                                                   (2, 2, 'Dołączono do czatu "Eventy"',  '2025-08-30 11:00:00');

INSERT INTO firma (id, nazwa, link, uzytkownik_id) VALUES
    (1, 'EventPromo', 'https://eventpromo.pl', 3);

INSERT INTO promowanie (id, data_poczatku, data_konca, wydarzenie_id, firma_id) VALUES
    (1, '2025-08-31 00:00:00', '2025-09-06 23:59:59', 1, 1);

INSERT INTO skarga (id, opis, kategoria_skarga_id, wydarzenie_id, uzytkownik_id) VALUES
    (1, 'Nieodpowiednie treści w opisie.', 2, 1, 2);

INSERT INTO decyzja (id, opis, blokowanie, moderator_osoba_id, skarga_id) VALUES
    (1, 'Zablokowano komentarze pod wydarzeniem na 7 dni.', '2025-09-01 12:00:00', 5, 1);

-- MAX(id) w każdej tabeli z kolumną serial
SELECT setval(pg_get_serial_sequence('chat','id'),                 COALESCE((SELECT MAX(id) FROM chat), 1), true);
SELECT setval(pg_get_serial_sequence('kategoria','id'),           COALESCE((SELECT MAX(id) FROM kategoria), 1), true);
SELECT setval(pg_get_serial_sequence('kategoria_skarga','id'),    COALESCE((SELECT MAX(id) FROM kategoria_skarga), 1), true);
SELECT setval(pg_get_serial_sequence('kategoria_wydarzenie','id'),COALESCE((SELECT MAX(id) FROM kategoria_wydarzenie), 1), true);
SELECT setval(pg_get_serial_sequence('komentarz','id'),           COALESCE((SELECT MAX(id) FROM komentarz), 1), true);
SELECT setval(pg_get_serial_sequence('lista_upodobanych','id'),   COALESCE((SELECT MAX(id) FROM lista_upodobanych), 1), true);
SELECT setval(pg_get_serial_sequence('lokalizacja','id'),         COALESCE((SELECT MAX(id) FROM lokalizacja), 1), true);
SELECT setval(pg_get_serial_sequence('moderator','osoba_id'),     COALESCE((SELECT MAX(osoba_id) FROM moderator), 1), true);
SELECT setval(pg_get_serial_sequence('obserwowanie','id'),        COALESCE((SELECT MAX(id) FROM obserwowanie), 1), true);
SELECT setval(pg_get_serial_sequence('ocena','id'),               COALESCE((SELECT MAX(id) FROM ocena), 1), true);
SELECT setval(pg_get_serial_sequence('osoba','id'),               COALESCE((SELECT MAX(id) FROM osoba), 1), true);
SELECT setval(pg_get_serial_sequence('powiadomienie','id'),       COALESCE((SELECT MAX(id) FROM powiadomienie), 1), true);
SELECT setval(pg_get_serial_sequence('promowanie','id'),          COALESCE((SELECT MAX(id) FROM promowanie), 1), true);
SELECT setval(pg_get_serial_sequence('repost','id'),              COALESCE((SELECT MAX(id) FROM repost), 1), true);
SELECT setval(pg_get_serial_sequence('skarga','id'),              COALESCE((SELECT MAX(id) FROM skarga), 1), true);
SELECT setval(pg_get_serial_sequence('uzytkownik_chat','id'),     COALESCE((SELECT MAX(id) FROM uzytkownik_chat), 1), true);
SELECT setval(pg_get_serial_sequence('uzytkownik','osoba_id'),    COALESCE((SELECT MAX(osoba_id) FROM uzytkownik), 1), true);
SELECT setval(pg_get_serial_sequence('wydarzenie','id'),          COALESCE((SELECT MAX(id) FROM wydarzenie), 1), true);
SELECT setval(pg_get_serial_sequence('zdjecie','id'),             COALESCE((SELECT MAX(id) FROM zdjecie), 1), true);
SELECT setval(pg_get_serial_sequence('firma','id'),               COALESCE((SELECT MAX(id) FROM firma), 1), true);
SELECT setval(pg_get_serial_sequence('decyzja','id'),             COALESCE((SELECT MAX(id) FROM decyzja), 1), true);

COMMIT;
