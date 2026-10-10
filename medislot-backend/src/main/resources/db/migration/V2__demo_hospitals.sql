-- DEMO DATA: fictional hospitals for the college demonstration. Not real facilities.
INSERT INTO hospitals (name, address_line, city, state, postal_code, country, latitude, longitude, phone) VALUES
 ('MediCare Hospital',                  'Road No. 12, Banjara Hills',  'Hyderabad', 'Telangana', '500034', 'India', 17.4126000, 78.4482000, '+914000000001'),
 ('City Health Hospital',               'Punjagutta Main Road',        'Hyderabad', 'Telangana', '500082', 'India', 17.4270000, 78.4550000, '+914000000002'),
 ('Lakeview Multispeciality Hospital',  'Madhapur',                    'Hyderabad', 'Telangana', '500081', 'India', 17.4486000, 78.3908000, '+914000000003'),
 ('Gachibowli Care Centre',             'Gachibowli',                  'Hyderabad', 'Telangana', '500032', 'India', 17.4401000, 78.3489000, '+914000000004'),
 ('Kukatpally Community Hospital',      'Kukatpally',                  'Hyderabad', 'Telangana', '500072', 'India', 17.4948000, 78.3996000, '+914000000005'),
 ('Secunderabad General Hospital',      'Secunderabad',                'Hyderabad', 'Telangana', '500003', 'India', 17.4399000, 78.4983000, '+914000000006'),
 ('Jubilee Hills Heart Institute',      'Jubilee Hills',               'Hyderabad', 'Telangana', '500033', 'India', 17.4326000, 78.4071000, '+914000000007'),
 ('Charminar Family Clinic',            'Charminar',                   'Hyderabad', 'Telangana', '500002', 'India', 17.3616000, 78.4747000, '+914000000008'),
 ('Warangal District Hospital',         'Hanamkonda',                  'Warangal',  'Telangana', '506001', 'India', 17.9689000, 79.5941000, '+914000000009');

INSERT INTO departments (hospital_id, name)
SELECT h.id, d.dept
FROM hospitals h
JOIN (VALUES
 ('MediCare Hospital', 'Cardiology'), ('MediCare Hospital', 'ENT'), ('MediCare Hospital', 'General Medicine'), ('MediCare Hospital', 'Orthopedics'),
 ('City Health Hospital', 'General Medicine'), ('City Health Hospital', 'Pediatrics'), ('City Health Hospital', 'Dermatology'),
 ('Lakeview Multispeciality Hospital', 'Cardiology'), ('Lakeview Multispeciality Hospital', 'Neurology'), ('Lakeview Multispeciality Hospital', 'General Medicine'),
 ('Gachibowli Care Centre', 'General Medicine'), ('Gachibowli Care Centre', 'ENT'),
 ('Kukatpally Community Hospital', 'General Medicine'), ('Kukatpally Community Hospital', 'Pediatrics'), ('Kukatpally Community Hospital', 'Gynecology'),
 ('Secunderabad General Hospital', 'General Medicine'), ('Secunderabad General Hospital', 'Orthopedics'), ('Secunderabad General Hospital', 'Pulmonology'),
 ('Jubilee Hills Heart Institute', 'Cardiology'), ('Jubilee Hills Heart Institute', 'Cardiac Surgery'),
 ('Charminar Family Clinic', 'General Medicine'), ('Charminar Family Clinic', 'Pediatrics'),
 ('Warangal District Hospital', 'General Medicine'), ('Warangal District Hospital', 'Surgery')
) AS d(hospital, dept) ON d.hospital = h.name;