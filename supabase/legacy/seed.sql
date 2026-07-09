-- ════════════════════════════════════════════════════════════════════
--  JL ENTERPRISES — seed data (run AFTER schema.sql in the JL project)
-- ════════════════════════════════════════════════════════════════════

insert into categories (slug, name, emoji, sort_order) values
  ('air-conditioners', 'Air Conditioners', '❄️', 1),
  ('televisions',      'Televisions',       '📺', 2),
  ('refrigerators',    'Refrigerators',     '🧊', 3),
  ('washing-machines', 'Washing Machines',  '🌀', 4),
  ('home-theatre',     'Home Theatre',      '🔊', 5),
  ('kitchen',          'Kitchen & Stove',   '🍳', 6),
  ('furniture',        'Furniture',         '🛋️', 7)
on conflict (slug) do nothing;

-- Products (category_id resolved by slug)
insert into products
  (slug, name, brand, category_id, emoji, description, specs, price, mrp, stock, rating, review_count, emi_per_month, is_active, is_featured)
values
  ('voltas-1-5t-3star-inverter-ac', '1.5 Ton 3-Star Inverter Split AC', 'Voltas',
    (select id from categories where slug='air-conditioners'), '❄️',
    'Energy-efficient inverter AC with copper condenser, turbo cooling and free installation.',
    '{"Capacity":"1.5 Ton","Energy Rating":"3 Star Inverter","Warranty":"1 yr + 10 yr compressor","Installation":"Free"}',
    34990, 51500, 2, 4.6, 214, 1649, true, true),
  ('sony-55-4k-google-tv', '55" 4K Ultra HD Smart Google TV', 'Sony',
    (select id from categories where slug='televisions'), '📺',
    'Crisp 4K HDR picture with built-in Google TV, voice remote and Dolby Audio.',
    '{"Display":"55\" 4K UHD","Smart OS":"Google TV","Warranty":"1 year","Connectivity":"Wi-Fi, 3x HDMI"}',
    62990, 87900, 12, 4.4, 176, 2950, true, true),
  ('godrej-340l-double-door-fridge', '340L Double-Door Frost-Free Fridge', 'Godrej',
    (select id from categories where slug='refrigerators'), '🧊',
    'Spacious frost-free refrigerator with inverter compressor and toughened glass shelves.',
    '{"Capacity":"340 L","Type":"Double Door Frost-Free","Warranty":"1 yr + 10 yr compressor","Energy Rating":"3 Star"}',
    41200, 54000, 8, 4.7, 98, 1930, true, true),
  ('lg-7kg-front-load-wm', '7Kg Fully Automatic Front-Load Washing Machine', 'LG',
    (select id from categories where slug='washing-machines'), '🌀',
    'Front-load washer with inverter direct drive, steam wash and 6 motion technology.',
    '{"Capacity":"7 Kg","Type":"Front Load","Warranty":"2 yr + 10 yr motor","Wash Programs":"10"}',
    28500, 43900, 6, 4.5, 143, 1340, true, true),
  ('jl-home-5-seater-sofa', '5-Seater Premium Fabric Sofa Set', 'JL Home',
    (select id from categories where slug='furniture'), '🛋️',
    'Comfortable 3+2 fabric sofa set with solid wood frame and high-density foam.',
    '{"Seating":"5-Seater (3+2)","Material":"Premium Fabric","Frame":"Solid Wood","Warranty":"1 year"}',
    38000, 49000, 4, 4.5, 64, 1780, true, false),
  ('jl-home-queen-bed-storage', 'Queen Size Bed with Storage', 'JL Home',
    (select id from categories where slug='furniture'), '🛏️',
    'Engineered-wood queen bed with hydraulic storage and a sturdy headboard.',
    '{"Size":"Queen","Storage":"Hydraulic","Material":"Engineered Wood","Warranty":"1 year"}',
    24500, 31000, 5, 4.3, 41, 1150, true, false),
  ('preethi-mixer-stove-combo', 'Mixer Grinder + Gas Stove Combo', 'Preethi',
    (select id from categories where slug='kitchen'), '🍳',
    '750W mixer grinder with 3 jars plus a 2-burner toughened-glass gas stove.',
    '{"Mixer Power":"750 W","Jars":"3","Stove":"2 Burner Glass-Top","Warranty":"2 years"}',
    9450, 13200, 15, 4.6, 187, 790, true, false),
  ('sony-5-1-home-theatre', '5.1 Channel Home Theatre System', 'Sony',
    (select id from categories where slug='home-theatre'), '🔊',
    'Immersive 5.1 surround sound with Bluetooth, USB and a powerful subwoofer.',
    '{"Channels":"5.1","Power":"1000W","Connectivity":"Bluetooth, USB, HDMI","Warranty":"1 year"}',
    18990, 26500, 3, 4.4, 72, 890, true, false)
on conflict (slug) do nothing;

-- A few offers for the admin panel
insert into offers (title, type, value_text, is_active, ends_on) values
  ('Summer AC Sale', 'CATEGORY', 'Up to 40% off', true, '2026-06-30'),
  ('Exchange Bonus', 'BUYBACK', '+₹3,000 off', true, '2026-07-15'),
  ('Furniture Fest', 'CATEGORY', '22% off', true, '2026-07-05')
on conflict do nothing;
