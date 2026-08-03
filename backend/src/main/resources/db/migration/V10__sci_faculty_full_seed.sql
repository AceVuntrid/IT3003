-- =============================================================================
-- V10: Faculty of Science, University of Colombo — full seed per URS
--
-- This migration:
--   1. Renames placeholder departments to URS-exact names
--   2. Adds Dean's Office department
--   3. Adds a building per academic department + Dean's Office building
--   4. Adds all labs and lecture halls (3 + 3 per academic dept)
--   5. Adds Statistics Auditorium, SSC rooms, ILC Auditorium, King George Hall,
--      Quadrangle Common Area
--   6. Sets LKR booking fees per the URS pricing table
--   7. Adds physics/chem/plant-sci/zoology/electronics asset sub-categories
--   8. Seeds individually-identifiable lab equipment per department
--   9. Seeds SSC Computer Lab (40 desktops, reservable = false)
--  10. Seeds shared AV/electronics and furniture
--  11. Seeds consumable inventory
--
-- All INSERTs are idempotent (WHERE NOT EXISTS on unique codes).
-- =============================================================================

-- =============================================================================
-- PART 1: Rename existing departments to URS-exact display names
--         (codes are kept stable so DataInitializer dept-admin links still work)
-- =============================================================================
update departments set name = 'Department of Physics'
    where code = 'PHYS';

update departments set name = 'Department of Chemistry'
    where code = 'CHEM';

update departments set name = 'Department of Plant Sciences'
    where code = 'BOT';

update departments set name = 'Department of Zoology and Environmental Sciences'
    where code = 'ZOO';

update departments set name = 'Department of Mathematics'
    where code = 'MATH';

update departments set name = 'Department of Statistics'
    where code = 'STAT';

-- =============================================================================
-- PART 2: Dean's Office department
-- =============================================================================
insert into departments (id, faculty_id, code, name, description, active, created_at)
select 'a0000000-0000-0000-0000-000000000019', f.id,
       'DEAN', 'Dean''s Office',
       'Manages King George Hall and Quadrangle Common Area', true, now()
from faculties f where f.code = 'SCI'
  and not exists (select 1 from departments where code = 'DEAN');

-- =============================================================================
-- PART 3: Department buildings
--         Each academic dept and the Dean's Office gets its own building node.
--         ILC and SSC buildings already exist from V4.
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000020',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHYS-BLDG', 'Physics Department Building', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'PHYS-BLDG');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000021',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-BLDG', 'Chemistry Department Building', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'CHEM-BLDG');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000022',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-BLDG', 'Plant Sciences Department Building', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'BOT-BLDG');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000023',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-BLDG', 'Zoology Department Building', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'ZOO-BLDG');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000024',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'MATH'),
       'MATH-BLDG', 'Mathematics Department Building', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'MATH-BLDG');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000025',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'STAT'),
       'STAT-BLDG', 'Statistics Department Building', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'STAT-BLDG');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, active, created_at)
select 'b0000000-0000-0000-0000-000000000026',
       (select id from locations where code = 'MAIN'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'DEAN'),
       'DEAN-BLDG', 'Dean''s Office', 'BUILDING', true, now()
where not exists (select 1 from locations where code = 'DEAN-BLDG');

-- =============================================================================
-- PART 4: Physics — 3 laboratories + 3 lecture halls
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000030',
       (select id from locations where code = 'PHYS-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHY-ELEC-LAB', 'Electronics Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'PHY-ELEC-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000031',
       (select id from locations where code = 'PHYS-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHY-OPT-LAB', 'Optics Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'PHY-OPT-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000032',
       (select id from locations where code = 'PHYS-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHY-GEN-LAB', 'General Physics Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'PHY-GEN-LAB');

-- Lecture halls with booking fees set in Part 8
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000033',
       (select id from locations where code = 'PHYS-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHY-LH-S', 'Physics Lecture Hall (Small)', 'LECTURE_ROOM', 60, true, now()
where not exists (select 1 from locations where code = 'PHY-LH-S');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000034',
       (select id from locations where code = 'PHYS-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHY-LH-M', 'Physics Lecture Hall (Medium)', 'LECTURE_ROOM', 100, true, now()
where not exists (select 1 from locations where code = 'PHY-LH-M');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000035',
       (select id from locations where code = 'PHYS-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       'PHY-LH-L', 'Physics Lecture Hall (Large)', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'PHY-LH-L');

-- =============================================================================
-- PART 5: Chemistry — 3 laboratories + 3 lecture halls
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000040',
       (select id from locations where code = 'CHEM-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-ORG-LAB', 'Organic Chemistry Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'CHEM-ORG-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000041',
       (select id from locations where code = 'CHEM-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-INORG-LAB', 'Inorganic Chemistry Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'CHEM-INORG-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000042',
       (select id from locations where code = 'CHEM-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-ANAL-LAB', 'Analytical Chemistry Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'CHEM-ANAL-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000043',
       (select id from locations where code = 'CHEM-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-LH-S', 'Chemistry Lecture Hall (Small)', 'LECTURE_ROOM', 60, true, now()
where not exists (select 1 from locations where code = 'CHEM-LH-S');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000044',
       (select id from locations where code = 'CHEM-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-LH-M', 'Chemistry Lecture Hall (Medium)', 'LECTURE_ROOM', 100, true, now()
where not exists (select 1 from locations where code = 'CHEM-LH-M');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000045',
       (select id from locations where code = 'CHEM-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       'CHEM-LH-L', 'Chemistry Lecture Hall (Large)', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'CHEM-LH-L');

-- =============================================================================
-- PART 6: Plant Sciences — 3 laboratories + 3 lecture halls
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000050',
       (select id from locations where code = 'BOT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-PHYSIO-LAB', 'Plant Physiology Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'BOT-PHYSIO-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000051',
       (select id from locations where code = 'BOT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-MICRO-LAB', 'Microbiology Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'BOT-MICRO-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000052',
       (select id from locations where code = 'BOT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-TISSUE-LAB', 'Tissue Culture Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'BOT-TISSUE-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000053',
       (select id from locations where code = 'BOT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-LH-S', 'Plant Sciences Lecture Hall (Small)', 'LECTURE_ROOM', 60, true, now()
where not exists (select 1 from locations where code = 'BOT-LH-S');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000054',
       (select id from locations where code = 'BOT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-LH-M', 'Plant Sciences Lecture Hall (Medium)', 'LECTURE_ROOM', 100, true, now()
where not exists (select 1 from locations where code = 'BOT-LH-M');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000055',
       (select id from locations where code = 'BOT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       'BOT-LH-L', 'Plant Sciences Lecture Hall (Large)', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'BOT-LH-L');

-- =============================================================================
-- PART 7: Zoology — 3 laboratories + 3 lecture halls
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000060',
       (select id from locations where code = 'ZOO-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-ECO-LAB', 'Ecology Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'ZOO-ECO-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000061',
       (select id from locations where code = 'ZOO-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-ANIMAL-LAB', 'Animal Physiology Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'ZOO-ANIMAL-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000062',
       (select id from locations where code = 'ZOO-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-MOL-LAB', 'Molecular Biology Laboratory', 'LABORATORY', null, true, now()
where not exists (select 1 from locations where code = 'ZOO-MOL-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000063',
       (select id from locations where code = 'ZOO-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-LH-S', 'Zoology Lecture Hall (Small)', 'LECTURE_ROOM', 60, true, now()
where not exists (select 1 from locations where code = 'ZOO-LH-S');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000064',
       (select id from locations where code = 'ZOO-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-LH-M', 'Zoology Lecture Hall (Medium)', 'LECTURE_ROOM', 100, true, now()
where not exists (select 1 from locations where code = 'ZOO-LH-M');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000065',
       (select id from locations where code = 'ZOO-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       'ZOO-LH-L', 'Zoology Lecture Hall (Large)', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'ZOO-LH-L');

-- =============================================================================
-- PART 8: Mathematics — 3 lecture halls (no laboratories)
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000070',
       (select id from locations where code = 'MATH-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'MATH'),
       'MATH-LH-S', 'Mathematics Lecture Hall (Small)', 'LECTURE_ROOM', 60, true, now()
where not exists (select 1 from locations where code = 'MATH-LH-S');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000071',
       (select id from locations where code = 'MATH-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'MATH'),
       'MATH-LH-M', 'Mathematics Lecture Hall (Medium)', 'LECTURE_ROOM', 100, true, now()
where not exists (select 1 from locations where code = 'MATH-LH-M');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000072',
       (select id from locations where code = 'MATH-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'MATH'),
       'MATH-LH-L', 'Mathematics Lecture Hall (Large)', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'MATH-LH-L');

-- =============================================================================
-- PART 9: Statistics — 3 lecture halls + Statistics Auditorium (200 seats)
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000073',
       (select id from locations where code = 'STAT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'STAT'),
       'STAT-LH-S', 'Statistics Lecture Hall (Small)', 'LECTURE_ROOM', 60, true, now()
where not exists (select 1 from locations where code = 'STAT-LH-S');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000074',
       (select id from locations where code = 'STAT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'STAT'),
       'STAT-LH-M', 'Statistics Lecture Hall (Medium)', 'LECTURE_ROOM', 100, true, now()
where not exists (select 1 from locations where code = 'STAT-LH-M');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000075',
       (select id from locations where code = 'STAT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'STAT'),
       'STAT-LH-L', 'Statistics Lecture Hall (Large)', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'STAT-LH-L');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000076',
       (select id from locations where code = 'STAT-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'STAT'),
       'STAT-AUD', 'Statistics Auditorium', 'AUDITORIUM', 200, true, now()
where not exists (select 1 from locations where code = 'STAT-AUD');

-- =============================================================================
-- PART 10: SSC — Auditorium (already seeded in V4), 3 lecture halls,
--           Computer Laboratory, Common Area
-- =============================================================================
-- SSC Auditorium already exists as SSC-AUD (b16) from V4 with capacity 500.
-- Add the 3 correctly-sized lecture halls from the URS.
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000080',
       (select id from locations where code = 'SSC'),
       null, null,
       'SSC-LH-80', 'SSC Lecture Hall (80 seats)', 'LECTURE_ROOM', 80, true, now()
where not exists (select 1 from locations where code = 'SSC-LH-80');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000081',
       (select id from locations where code = 'SSC'),
       null, null,
       'SSC-LH-120', 'SSC Lecture Hall (120 seats)', 'LECTURE_ROOM', 120, true, now()
where not exists (select 1 from locations where code = 'SSC-LH-120');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000082',
       (select id from locations where code = 'SSC'),
       null, null,
       'SSC-LH-180', 'SSC Lecture Hall (180 seats)', 'LECTURE_ROOM', 180, true, now()
where not exists (select 1 from locations where code = 'SSC-LH-180');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000083',
       (select id from locations where code = 'SSC'),
       null, null,
       'SSC-COMP-LAB', 'SSC Computer Laboratory', 'LABORATORY', 40, true, now()
where not exists (select 1 from locations where code = 'SSC-COMP-LAB');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000084',
       (select id from locations where code = 'SSC'),
       null, null,
       'SSC-COMMON', 'SSC Common Area', 'ROOM', null, true, now()
where not exists (select 1 from locations where code = 'SSC-COMMON');

-- =============================================================================
-- PART 11: ILC — Auditorium + Common Area
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000090',
       (select id from locations where code = 'ILC'),
       null, null,
       'ILC-AUD', 'ILC Auditorium', 'AUDITORIUM', 400, true, now()
where not exists (select 1 from locations where code = 'ILC-AUD');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000091',
       (select id from locations where code = 'ILC'),
       null, null,
       'ILC-COMMON', 'ILC Common Area', 'ROOM', null, true, now()
where not exists (select 1 from locations where code = 'ILC-COMMON');

-- =============================================================================
-- PART 12: Dean's Office venues — King George Hall + Quadrangle Common Area
-- =============================================================================
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000092',
       (select id from locations where code = 'DEAN-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'DEAN'),
       'KGH', 'King George Hall', 'AUDITORIUM', 350, true, now()
where not exists (select 1 from locations where code = 'KGH');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000093',
       (select id from locations where code = 'DEAN-BLDG'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'DEAN'),
       'QUAD', 'Quadrangle Common Area', 'ROOM', null, true, now()
where not exists (select 1 from locations where code = 'QUAD');

-- =============================================================================
-- PART 13: Booking fees per URS pricing table
-- =============================================================================
-- Department Lecture Halls — Small (60 seats) = LKR 1,000
update locations set booking_fee = 1000.00
where code in ('PHY-LH-S','CHEM-LH-S','BOT-LH-S','ZOO-LH-S','MATH-LH-S','STAT-LH-S');

-- Department Lecture Halls — Medium (100 seats) = LKR 2,000
update locations set booking_fee = 2000.00
where code in ('PHY-LH-M','CHEM-LH-M','BOT-LH-M','ZOO-LH-M','MATH-LH-M','STAT-LH-M');

-- Department Lecture Halls — Large (150 seats) = LKR 3,000
update locations set booking_fee = 3000.00
where code in ('PHY-LH-L','CHEM-LH-L','BOT-LH-L','ZOO-LH-L','MATH-LH-L','STAT-LH-L');

-- SSC Lecture Halls (tiered by capacity)
update locations set booking_fee = 3000.00 where code = 'SSC-LH-80';
update locations set booking_fee = 4000.00 where code = 'SSC-LH-120';
update locations set booking_fee = 5000.00 where code = 'SSC-LH-180';
-- Also update original SSC-LR1 from V4
update locations set booking_fee = 3000.00 where code = 'SSC-LR1';

-- Auditoriums
update locations set booking_fee = 15000.00 where code = 'STAT-AUD';
update locations set booking_fee = 30000.00 where code = 'ILC-AUD';
update locations set booking_fee = 40000.00 where code in ('SSC-AUD', 'SSC-AUD');
update locations set booking_fee = 35000.00 where code = 'KGH';

-- Common areas are free (booking_fee = 0 / NULL)
update locations set booking_fee = 0.00
where code in ('SSC-COMMON','ILC-COMMON','QUAD');

-- =============================================================================
-- PART 14: Asset sub-categories
--
-- Top-level categories from V2 seed:
--   LAB-EQUIP (c01), COMPUTING (c02), OPTICS (c03), AV (c04),
--   FURNITURE (c05), MEASURE (c07),
--   CHEMICALS (c11), GLASSWARE (c12), LAB-SUPPLY (c13), SAFETY (c14),
--   STATIONERY (c15)
-- ELECTRONICS added in V4 with gen_random_uuid (reference by code below)
-- =============================================================================

-- ---- Physics sub-categories (children of LAB-EQUIP) ----
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'OSCILLOSCOPE', 'Oscilloscopes', 'Digital and analog oscilloscopes', 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'OSCILLOSCOPE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'DIGITAL-MULTIMETER', 'Digital Multimeters', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'DIGITAL-MULTIMETER');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'FUNCTION-GEN', 'Function Generators', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'FUNCTION-GEN');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'DC-POWER-SUPPLY', 'DC Power Supplies', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'DC-POWER-SUPPLY');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'LASER-KIT', 'Laser Kits', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'LASER-KIT');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'OPTICAL-BENCH', 'Optical Benches', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'OPTICAL-BENCH');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'MEASURE'),
       'SPECTROMETER', 'Spectrometers', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'SPECTROMETER');

-- ---- Chemistry sub-categories ----
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'MAGNETIC-STIRRER', 'Magnetic Stirrers', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'MAGNETIC-STIRRER');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'HOT-PLATE', 'Hot Plates', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'HOT-PLATE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'FUME-HOOD', 'Fume Hoods', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'FUME-HOOD');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'PH-METER', 'pH Meters', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'PH-METER');

-- ---- Plant Sciences sub-categories ----
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'INCUBATOR', 'Incubators', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'INCUBATOR');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'TISSUE-CULTURE-CABINET', 'Tissue Culture Cabinets', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'TISSUE-CULTURE-CABINET');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'AUTOCLAVE', 'Autoclaves', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'AUTOCLAVE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'GROWTH-CHAMBER', 'Growth Chambers', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'GROWTH-CHAMBER');

-- ---- Zoology sub-categories ----
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'OPTICS'),
       'STEREO-MICROSCOPE', 'Stereo Microscopes', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'STEREO-MICROSCOPE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'CENTRIFUGE', 'Centrifuges', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'CENTRIFUGE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-EQUIP'),
       'DISSECTION-KIT', 'Dissection Kits', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'DISSECTION-KIT');

-- ---- Electronics/AV sub-categories (children of AV or ELECTRONICS) ----
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'AV'),
       'PA-SYSTEM', 'Public Address Systems', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'PA-SYSTEM');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'AV'),
       'WIRELESS-MIC', 'Wireless Microphones', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'WIRELESS-MIC');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'AV'),
       'AMPLIFIER', 'Amplifiers', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'AMPLIFIER');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'AV'),
       'PORTABLE-SPEAKER', 'Portable Speakers', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'PORTABLE-SPEAKER');

-- ---- Consumable sub-categories ----
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'STATIONERY'),
       'PRINTER-PAPER', 'Printer Paper', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'PRINTER-PAPER');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'STATIONERY'),
       'TONER-CARTRIDGE', 'Toner Cartridges', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'TONER-CARTRIDGE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'STATIONERY'),
       'MARKER-PEN', 'Marker Pens', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'MARKER-PEN');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'STATIONERY'),
       'WHITEBOARD-ERASER', 'Whiteboard Erasers', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'WHITEBOARD-ERASER');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-SUPPLY'),
       'PIPETTE-TIPS', 'Pipette Tips', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'PIPETTE-TIPS');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-SUPPLY'),
       'FILTER-PAPER', 'Filter Papers', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'FILTER-PAPER');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-SUPPLY'),
       'SAMPLE-BOTTLE', 'Sample Bottles', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'SAMPLE-BOTTLE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), (select id from asset_categories where code = 'LAB-SUPPLY'),
       'CLEANING-MATERIALS', 'Cleaning Materials', null, 'CONSUMABLE', true, now()
where not exists (select 1 from asset_categories where code = 'CLEANING-MATERIALS');

-- =============================================================================
-- PART 15: Physics assets — individually identifiable per URS §7
-- =============================================================================
-- 3 × Oscilloscopes (Electronics Laboratory)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required,
                    deposit_required, deposit_amount, max_reservation_hours, created_at)
select gen_random_uuid(), 'PHY-OSC-001', 'Oscilloscope PHY-OSC-001',
       '200 MHz 4-channel digital oscilloscope', 'FIXED',
       (select id from asset_categories where code = 'OSCILLOSCOPE'),
       'Keysight', 'DSOX1204G', 'KEY-1204-001', 'ASSET:PHY-OSC-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-ELEC-LAB'),
       '2023-06-01', 385000.00, 'LKR', 308000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true,
       false, null, 48, now()
where not exists (select 1 from assets where asset_code = 'PHY-OSC-001');

insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required,
                    deposit_required, deposit_amount, max_reservation_hours, created_at)
select gen_random_uuid(), 'PHY-OSC-002', 'Oscilloscope PHY-OSC-002',
       '200 MHz 4-channel digital oscilloscope', 'FIXED',
       (select id from asset_categories where code = 'OSCILLOSCOPE'),
       'Keysight', 'DSOX1204G', 'KEY-1204-002', 'ASSET:PHY-OSC-002',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-ELEC-LAB'),
       '2023-06-01', 385000.00, 'LKR', 308000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true,
       false, null, 48, now()
where not exists (select 1 from assets where asset_code = 'PHY-OSC-002');

insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required,
                    deposit_required, deposit_amount, max_reservation_hours, created_at)
select gen_random_uuid(), 'PHY-OSC-003', 'Oscilloscope PHY-OSC-003',
       '200 MHz 4-channel digital oscilloscope', 'FIXED',
       (select id from asset_categories where code = 'OSCILLOSCOPE'),
       'Keysight', 'DSOX1204G', 'KEY-1204-003', 'ASSET:PHY-OSC-003',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-ELEC-LAB'),
       '2023-06-01', 385000.00, 'LKR', 280000.00,
       1, 1, 'NEW', 'FAIR', 'AVAILABLE', true, true,
       false, null, 48, now()
where not exists (select 1 from assets where asset_code = 'PHY-OSC-003');

-- 3 × Digital Multimeters
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-DMM-00' || n, 'Digital Multimeter PHY-DMM-00' || n,
       'True RMS digital multimeter', 'FIXED',
       (select id from asset_categories where code = 'DIGITAL-MULTIMETER'),
       'Fluke', '115', 'FLK-115-00' || n, 'ASSET:PHY-DMM-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-ELEC-LAB'),
       '2022-08-15', 45000.00, 'LKR', 32000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,3) n
where not exists (select 1 from assets where asset_code = 'PHY-DMM-00' || n);

-- 2 × Function Generators
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-FGN-00' || n, 'Function Generator PHY-FGN-00' || n,
       '20 MHz arbitrary function generator', 'FIXED',
       (select id from asset_categories where code = 'FUNCTION-GEN'),
       'Rigol', 'DG1022Z', 'RGL-DG1022-00' || n, 'ASSET:PHY-FGN-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-ELEC-LAB'),
       '2023-01-10', 95000.00, 'LKR', 75000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'PHY-FGN-00' || n);

-- 2 × DC Power Supplies
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-PSU-00' || n, 'DC Power Supply PHY-PSU-00' || n,
       'Triple-output DC bench power supply 30V/5A', 'FIXED',
       (select id from asset_categories where code = 'DC-POWER-SUPPLY'),
       'Keysight', 'E36313A', 'KEY-E363-00' || n, 'ASSET:PHY-PSU-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-ELEC-LAB'),
       '2023-01-10', 280000.00, 'LKR', 224000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'PHY-PSU-00' || n);

-- 1 × Laser Kit (Optics Laboratory)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required,
                    deposit_required, deposit_amount, max_reservation_hours, created_at)
select gen_random_uuid(), 'PHY-LSR-001', 'Laser Kit PHY-LSR-001',
       'He-Ne laser optics kit for interference and diffraction experiments', 'FIXED',
       (select id from asset_categories where code = 'LASER-KIT'),
       'Thorlabs', 'HNL020LB', 'THL-020L-001', 'ASSET:PHY-LSR-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-OPT-LAB'),
       '2022-03-20', 520000.00, 'LKR', 390000.00,
       1, 1, 'NEW', 'EXCELLENT', 'AVAILABLE', true, true,
       true, 5000.00, 24, now()
where not exists (select 1 from assets where asset_code = 'PHY-LSR-001');

-- 1 × Optical Bench
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-OBN-001', 'Optical Bench PHY-OBN-001',
       '1.5 m optical bench with carriers and mounts', 'FIXED',
       (select id from asset_categories where code = 'OPTICAL-BENCH'),
       'Leybold', 'LD-40001', 'LYB-40001-001', 'ASSET:PHY-OBN-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-OPT-LAB'),
       '2021-11-05', 185000.00, 'LKR', 120000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
where not exists (select 1 from assets where asset_code = 'PHY-OBN-001');

-- 2 × Spectrometers (General Physics Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, calibration_required, created_at)
select gen_random_uuid(), 'PHY-SPM-00' || n, 'Spectrometer PHY-SPM-00' || n,
       'Optical spectrometer for wavelength measurement', 'FIXED',
       (select id from asset_categories where code = 'SPECTROMETER'),
       'Ocean Optics', 'USB4000', 'OCN-USB4-00' || n, 'ASSET:PHY-SPM-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-GEN-LAB'),
       '2022-09-14', 320000.00, 'LKR', 240000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'PHY-SPM-00' || n);

-- Vernier Calipers (6 × in General Physics Lab, quantity-based)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-VCL-SET', 'Vernier Caliper Set',
       'Set of 6 Vernier calipers 0-150 mm', 'FIXED',
       (select id from asset_categories where code = 'MEASURE'),
       'Mitutoyo', '530-312', null, 'ASSET:PHY-VCL-SET',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-GEN-LAB'),
       '2021-04-20', 72000.00, 'LKR', 50000.00,
       6, 6, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
where not exists (select 1 from assets where asset_code = 'PHY-VCL-SET');

-- Micrometers (4 × in General Physics Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-MIC-SET', 'Micrometer Screw Gauge Set',
       'Set of 4 outside micrometers 0-25 mm', 'FIXED',
       (select id from asset_categories where code = 'MEASURE'),
       'Mitutoyo', '293-340-30', null, 'ASSET:PHY-MIC-SET',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-GEN-LAB'),
       '2021-04-20', 56000.00, 'LKR', 40000.00,
       4, 4, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
where not exists (select 1 from assets where asset_code = 'PHY-MIC-SET');

-- =============================================================================
-- PART 16: Chemistry assets
-- =============================================================================
-- 2 × Analytical Balances (Analytical Chemistry Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, calibration_required, created_at)
select gen_random_uuid(), 'CHEM-BAL-00' || n, 'Analytical Balance CHEM-BAL-00' || n,
       '0.1 mg readability analytical balance', 'FIXED',
       (select id from asset_categories where code = 'MEASURE'),
       'Mettler Toledo', 'ME204', 'MT-ME204-00' || n, 'ASSET:CHEM-BAL-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       '2022-05-10', 520000.00, 'LKR', 390000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'CHEM-BAL-00' || n);

-- 2 × Magnetic Stirrers (Organic Chemistry Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'CHEM-STR-00' || n, 'Magnetic Stirrer CHEM-STR-00' || n,
       'Magnetic stirrer with hot plate, 2 L capacity', 'FIXED',
       (select id from asset_categories where code = 'MAGNETIC-STIRRER'),
       'IKA', 'C-MAG HS 7', 'IKA-CMS7-00' || n, 'ASSET:CHEM-STR-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ORG-LAB'),
       '2023-02-14', 68000.00, 'LKR', 54000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'CHEM-STR-00' || n);

-- 2 × Hot Plates (Organic Chemistry Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'CHEM-HPL-00' || n, 'Hot Plate CHEM-HPL-00' || n,
       'Ceramic top electric hot plate 550°C max', 'FIXED',
       (select id from asset_categories where code = 'HOT-PLATE'),
       'IKA', 'C-MAG HP 7', 'IKA-HP7-00' || n, 'ASSET:CHEM-HPL-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ORG-LAB'),
       '2023-02-14', 45000.00, 'LKR', 36000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'CHEM-HPL-00' || n);

-- 2 × Fume Hoods (Analytical Lab — already existed as CHEM-LAB1 in V2, use new lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'CHEM-FHD-00' || n, 'Fume Hood CHEM-FHD-00' || n,
       'Ducted laboratory fume hood 1.2 m', 'FIXED',
       (select id from asset_categories where code = 'FUME-HOOD'),
       'Esco', 'Premier', 'ESC-PRM-00' || n, 'ASSET:CHEM-FHD-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       '2021-07-30', 850000.00, 'LKR', 595000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', false, false, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'CHEM-FHD-00' || n);

-- 2 × UV-Vis Spectrophotometers (Analytical Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required,
                    deposit_required, deposit_amount, calibration_required, created_at)
select gen_random_uuid(), 'CHEM-UVS-00' || n, 'UV-Vis Spectrophotometer CHEM-UVS-00' || n,
       'Double-beam UV-Vis spectrophotometer 190-1100 nm', 'FIXED',
       (select id from asset_categories where code = 'MEASURE'),
       'Shimadzu', 'UV-1900i', 'SHM-1900-00' || n, 'ASSET:CHEM-UVS-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       '2023-08-01', 1850000.00, 'LKR', 1480000.00,
       1, 1, 'NEW', 'EXCELLENT', 'AVAILABLE', true, true,
       true, 50000.00, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'CHEM-UVS-00' || n);

-- 2 × pH Meters (Analytical Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, calibration_required, created_at)
select gen_random_uuid(), 'CHEM-PHM-00' || n, 'pH Meter CHEM-PHM-00' || n,
       'Benchtop pH/mV/temperature meter', 'FIXED',
       (select id from asset_categories where code = 'PH-METER'),
       'Mettler Toledo', 'S210', 'MT-S210-00' || n, 'ASSET:CHEM-PHM-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       '2022-11-25', 185000.00, 'LKR', 148000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'CHEM-PHM-00' || n);

-- =============================================================================
-- PART 17: Plant Sciences assets
-- =============================================================================
-- 4 × Compound Microscopes (Plant Physiology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'BOT-MCR-00' || n, 'Compound Microscope BOT-MCR-00' || n,
       'Binocular compound microscope 40x-1000x', 'FIXED',
       (select id from asset_categories where code = 'OPTICS'),
       'Olympus', 'CX23', 'OLY-CX23-00' || n, 'ASSET:BOT-MCR-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       (select id from locations where code = 'BOT-PHYSIO-LAB'),
       '2024-01-15', 240000.00, 'LKR', 192000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,4) n
where not exists (select 1 from assets where asset_code = 'BOT-MCR-00' || n);

-- 2 × Incubators (Microbiology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'BOT-INC-00' || n, 'Incubator BOT-INC-00' || n,
       'Benchtop incubator 37°C, 56 L', 'FIXED',
       (select id from asset_categories where code = 'INCUBATOR'),
       'Memmert', 'IN55plus', 'MMT-IN55-00' || n, 'ASSET:BOT-INC-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       (select id from locations where code = 'BOT-MICRO-LAB'),
       '2023-05-22', 650000.00, 'LKR', 520000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'BOT-INC-00' || n);

-- 1 × Tissue Culture Cabinet (Tissue Culture Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'BOT-TCC-001', 'Tissue Culture Cabinet BOT-TCC-001',
       'Class II biological safety cabinet for tissue culture', 'FIXED',
       (select id from asset_categories where code = 'TISSUE-CULTURE-CABINET'),
       'Esco', 'Labculture II', 'ESC-LC2-001', 'ASSET:BOT-TCC-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       (select id from locations where code = 'BOT-TISSUE-LAB'),
       '2022-02-10', 1250000.00, 'LKR', 875000.00,
       1, 1, 'NEW', 'EXCELLENT', 'AVAILABLE', false, true, now()
where not exists (select 1 from assets where asset_code = 'BOT-TCC-001');

-- 1 × Autoclave (Tissue Culture Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'BOT-AUC-001', 'Autoclave BOT-AUC-001',
       'Vertical autoclave 23 L, 134°C', 'FIXED',
       (select id from asset_categories where code = 'AUTOCLAVE'),
       'Tuttnauer', '2840MK', 'TUT-2840-001', 'ASSET:BOT-AUC-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       (select id from locations where code = 'BOT-TISSUE-LAB'),
       '2021-09-30', 780000.00, 'LKR', 546000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', false, true, now()
where not exists (select 1 from assets where asset_code = 'BOT-AUC-001');

-- 1 × Growth Chamber (Plant Physiology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'BOT-GCH-001', 'Growth Chamber BOT-GCH-001',
       'Reach-in plant growth chamber with LED lighting and humidity control', 'FIXED',
       (select id from asset_categories where code = 'GROWTH-CHAMBER'),
       'Conviron', 'CMP6010', 'CNV-CMP6-001', 'ASSET:BOT-GCH-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       (select id from locations where code = 'BOT-PHYSIO-LAB'),
       '2023-11-08', 2850000.00, 'LKR', 2280000.00,
       1, 1, 'NEW', 'EXCELLENT', 'AVAILABLE', true, true, now()
where not exists (select 1 from assets where asset_code = 'BOT-GCH-001');

-- =============================================================================
-- PART 18: Zoology assets
-- =============================================================================
-- 3 × Compound Microscopes (Animal Physiology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'ZOO-MCR-00' || n, 'Compound Microscope ZOO-MCR-00' || n,
       'Binocular compound microscope 40x-1000x', 'FIXED',
       (select id from asset_categories where code = 'OPTICS'),
       'Olympus', 'CX23', 'OLY-CX23-Z0' || n, 'ASSET:ZOO-MCR-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       (select id from locations where code = 'ZOO-ANIMAL-LAB'),
       '2023-03-15', 240000.00, 'LKR', 192000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,3) n
where not exists (select 1 from assets where asset_code = 'ZOO-MCR-00' || n);

-- 2 × Stereo Microscopes (Ecology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'ZOO-STE-00' || n, 'Stereo Microscope ZOO-STE-00' || n,
       'Stereo zoom microscope 7x-45x with LED illuminator', 'FIXED',
       (select id from asset_categories where code = 'STEREO-MICROSCOPE'),
       'Nikon', 'SMZ445', 'NKN-SMZ445-00' || n, 'ASSET:ZOO-STE-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       (select id from locations where code = 'ZOO-ECO-LAB'),
       '2022-07-20', 185000.00, 'LKR', 148000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'ZOO-STE-00' || n);

-- 2 × Centrifuges (Molecular Biology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'ZOO-CFG-00' || n, 'Centrifuge ZOO-CFG-00' || n,
       'Benchtop microcentrifuge 15,000 rpm', 'FIXED',
       (select id from asset_categories where code = 'CENTRIFUGE'),
       'Eppendorf', '5425R', 'EPP-5425-00' || n, 'ASSET:ZOO-CFG-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       (select id from locations where code = 'ZOO-MOL-LAB'),
       '2023-09-12', 480000.00, 'LKR', 384000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, now()
from generate_series(1,2) n
where not exists (select 1 from assets where asset_code = 'ZOO-CFG-00' || n);

-- 5 × Dissection Kits (Ecology Lab)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'ZOO-DKT-00' || n, 'Dissection Kit ZOO-DKT-00' || n,
       '14-piece stainless steel dissection kit with case', 'FIXED',
       (select id from asset_categories where code = 'DISSECTION-KIT'),
       'Carolina Biological', 'Premium', null, 'ASSET:ZOO-DKT-00' || n,
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'ZOO'),
       (select id from locations where code = 'ZOO-ECO-LAB'),
       '2022-01-10', 8500.00, 'LKR', 5950.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
from generate_series(1,5) n
where not exists (select 1 from assets where asset_code = 'ZOO-DKT-00' || n);

-- =============================================================================
-- PART 19: SSC Computer Laboratory — 40 desktops (reservable = false)
--          URS: "Desktop computers located in the SSC Computer Laboratory shall
--               be marked as On-Site Use Only and shall not be reservable"
-- =============================================================================
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'SSC-PC-SET', 'SSC Computer Lab — Desktop Computers (40 units)',
       'Dell OptiPlex 7010 SFF, i5-13500, 16 GB RAM, 512 GB SSD. On-site use only.',
       'FIXED',
       (select id from asset_categories where code = 'COMPUTING'),
       'Dell', 'OptiPlex 7010 SFF', null, 'ASSET:SSC-PC-SET',
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC-COMP-LAB'),
       '2025-01-20', 12000000.00, 'LKR', 10800000.00,
       40, 40, 'NEW', 'GOOD', 'AVAILABLE',
       false,   -- NOT reservable (on-site use only)
       false, now()
where not exists (select 1 from assets where asset_code = 'SSC-PC-SET');

-- =============================================================================
-- PART 20: Shared AV/Electronics — projectors and PA systems
-- =============================================================================
-- Multimedia Projectors (one per dept building, plus extras for SSC/ILC)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-PRJ-001', 'Multimedia Projector PHY-PRJ-001',
       '4K laser projector for lecture rooms', 'FIXED',
       (select id from asset_categories where code = 'AV'),
       'Epson', 'EB-PQ2010B', 'EPS-PQ2010-001', 'ASSET:PHY-PRJ-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-LH-L'),
       '2024-06-01', 320000.00, 'LKR', 288000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
where not exists (select 1 from assets where asset_code = 'PHY-PRJ-001');

insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'CHEM-PRJ-001', 'Multimedia Projector CHEM-PRJ-001',
       '4K laser projector for lecture rooms', 'FIXED',
       (select id from asset_categories where code = 'AV'),
       'Epson', 'EB-PQ2010B', 'EPS-PQ2010-002', 'ASSET:CHEM-PRJ-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-LH-L'),
       '2024-06-01', 320000.00, 'LKR', 288000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
where not exists (select 1 from assets where asset_code = 'CHEM-PRJ-001');

-- Portable PA System (shared, stored in SSC)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, max_reservation_hours, created_at)
select gen_random_uuid(), 'SSC-PAS-001', 'Public Address System SSC-PAS-001',
       'Portable PA system — 2 × wireless mics, amplifier, 2 × speakers', 'FIXED',
       (select id from asset_categories where code = 'PA-SYSTEM'),
       'Bose', 'L1 Pro16', 'BSE-L1P16-001', 'ASSET:SSC-PAS-001',
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       '2024-03-10', 750000.00, 'LKR', 675000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, true, 12, now()
where not exists (select 1 from assets where asset_code = 'SSC-PAS-001');

-- Wireless Microphones (2 × portable, stored at SSC)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, max_reservation_hours, created_at)
select gen_random_uuid(), 'SSC-WMC-00' || n, 'Wireless Microphone SSC-WMC-00' || n,
       'UHF wireless handheld microphone system', 'FIXED',
       (select id from asset_categories where code = 'WIRELESS-MIC'),
       'Sennheiser', 'XSW 2-835', 'SEN-XSW-00' || n, 'ASSET:SSC-WMC-00' || n,
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       '2024-03-10', 95000.00, 'LKR', 85000.00,
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, 12, now()
from generate_series(1,4) n
where not exists (select 1 from assets where asset_code = 'SSC-WMC-00' || n);

-- =============================================================================
-- PART 21: Furniture — representative items per department
-- =============================================================================
-- Whiteboards (1 per lecture hall — use Physics as example)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), 'PHY-WBD-001', 'Whiteboard PHY-WBD-001',
       '1800 × 1200 mm magnetic whiteboard', 'FIXED',
       (select id from asset_categories where code = 'FURNITURE'),
       'Quartet', 'Classic', 'ASSET:PHY-WBD-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-LH-L'),
       '2021-06-01', 28000.00, 'LKR',
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', false, false, now()
where not exists (select 1 from assets where asset_code = 'PHY-WBD-001');

-- Podiums (1 per dept, portable)
insert into assets (id, asset_code, name, description, asset_type, category_id,
                    brand, model, qr_code,
                    faculty_id, department_id, location_id,
                    purchase_date, purchase_price, currency,
                    quantity, available_quantity, initial_condition, condition,
                    status, reservable, approval_required, created_at)
select gen_random_uuid(), dept_code || '-PDM-001',
       'Podium ' || dept_code || '-PDM-001',
       'Height-adjustable portable lectern', 'FIXED',
       (select id from asset_categories where code = 'FURNITURE'),
       'Aarco', 'WPS-44', 'ASSET:' || dept_code || '-PDM-001',
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = dept_code),
       (select id from locations where code = dept_code || '-LH-L'),
       '2022-08-01', 38000.00, 'LKR',
       1, 1, 'NEW', 'GOOD', 'AVAILABLE', true, false, now()
from (values ('PHY'),('CHEM'),('BOT'),('ZOO'),('MATH'),('STAT')) as t(dept_code)
where not exists (
    select 1 from assets where asset_code = dept_code || '-PDM-001'
);

-- =============================================================================
-- PART 22: Consumable inventory per URS §6
-- =============================================================================
-- Storage: use CHEM-ANAL-LAB for chemistry consumables, PHY-ELEC-LAB for lab supplies

-- Printer Paper (SSC store)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-PPR-001', 'Printer Paper A4 80gsm',
       'White A4 80gsm copy paper, 500 sheets/ream',
       (select id from asset_categories where code = 'PRINTER-PAPER'),
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       'ream', 200, 0, 50, 850.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-PPR-001');

-- Toner Cartridges (SSC)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-TNR-001', 'Laser Toner Cartridge (Black)',
       'Compatible black laser toner cartridge, approx 3500 page yield',
       (select id from asset_categories where code = 'TONER-CARTRIDGE'),
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       'pcs', 30, 0, 10, 4800.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-TNR-001');

-- Marker Pens (shared — SSC store)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-MKR-001', 'Whiteboard Marker Pens (Assorted)',
       'Assorted-colour dry-erase whiteboard markers, box of 12',
       (select id from asset_categories where code = 'MARKER-PEN'),
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       'box', 60, 0, 20, 320.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-MKR-001');

-- Whiteboard Erasers (shared)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-WER-001', 'Whiteboard Erasers',
       'Magnetic felt whiteboard eraser',
       (select id from asset_categories where code = 'WHITEBOARD-ERASER'),
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       'pcs', 40, 0, 15, 180.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-WER-001');

-- Laboratory Gloves — Nitrile (Chemistry store — already partially in V2 as CON-00002)
-- Add Physics dept gloves separately
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-GLV-PHY', 'Nitrile Gloves (M) — Physics',
       'Powder-free nitrile examination gloves, medium, box of 100',
       (select id from asset_categories where code = 'SAFETY'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'PHYS'),
       (select id from locations where code = 'PHY-GEN-LAB'),
       'box', 20, 0, 10, 1200.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-GLV-PHY');

-- Face Masks (shared)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-MSK-001', 'Disposable Face Masks',
       '3-ply disposable face masks, box of 50',
       (select id from asset_categories where code = 'SAFETY'),
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       'box', 100, 0, 30, 650.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-MSK-001');

-- Pipette Tips (Plant Sciences)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-PTP-BOT', 'Pipette Tips 1000 µL — Plant Sciences',
       'Universal 1000 µL pipette tips, rack of 96',
       (select id from asset_categories where code = 'PIPETTE-TIPS'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'BOT'),
       (select id from locations where code = 'BOT-MICRO-LAB'),
       'rack', 50, 0, 20, 750.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-PTP-BOT');

-- Filter Papers (Chemistry)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-FPR-CHEM', 'Filter Paper Grade 1 (125 mm)',
       'Whatman Grade 1 qualitative filter paper, 125 mm diameter, 100/pk',
       (select id from asset_categories where code = 'FILTER-PAPER'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       'pack', 30, 0, 10, 1850.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-FPR-CHEM');

-- Sample Bottles (Chemistry)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-SBT-CHEM', 'Sample Bottles 100 mL (Glass)',
       'Borosilicate glass sample/storage bottles with screw cap, 100 mL',
       (select id from asset_categories where code = 'SAMPLE-BOTTLE'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       'pcs', 120, 0, 40, 280.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-SBT-CHEM');

-- NaOH (Chemistry — new chemical alongside existing HCl in V2)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous,
                              chemical_classification, storage_instructions, active, created_at)
select gen_random_uuid(), 'CON-NAOH-001', 'Sodium Hydroxide pellets (NaOH)',
       'Laboratory grade sodium hydroxide pellets ≥97%',
       (select id from asset_categories where code = 'CHEMICALS'),
       (select id from faculties where code = 'SCI'),
       (select id from departments where code = 'CHEM'),
       (select id from locations where code = 'CHEM-ANAL-LAB'),
       'kg', 5, 0, 2, 2200.00, true, 'Corrosive, Class 8',
       'Store in cool, dry, well-ventilated area away from acids', true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-NAOH-001');

-- Cleaning Materials (SSC)
insert into consumable_items (id, item_code, name, description, category_id,
                              faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity,
                              reorder_level, unit_cost, hazardous, active, created_at)
select gen_random_uuid(), 'CON-CLN-001', 'Laboratory Cleaning Kit',
       'All-purpose laboratory surface cleaner and lint-free wipes',
       (select id from asset_categories where code = 'CLEANING-MATERIALS'),
       (select id from faculties where code = 'SCI'), null,
       (select id from locations where code = 'SSC'),
       'kit', 25, 0, 10, 1100.00, false, true, now()
where not exists (select 1 from consumable_items where item_code = 'CON-CLN-001');
