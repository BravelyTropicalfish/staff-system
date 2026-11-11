-- Fixed seed, atdd-design.md §4: five roles, six skills with their role domains, one
-- coordinator and two leads. Every statement inserts only what is missing, so a restart on the
-- file database re-applies the seed without overwriting a catalog edit the coordinator made
-- (a renamed domain or a deactivated item survives) and without duplicating a row.
--
-- Demo accounts only: fake people, one documented demo password, BCrypt with a per-row salt.
-- Password for all three accounts: staffing-demo (see atdd-design.md §4).

MERGE INTO role t
USING (VALUES ('PM'), ('BA'), ('DEV'), ('QA'), ('UX')) s(name)
ON t.name = s.name
WHEN NOT MATCHED THEN INSERT (name, active) VALUES (s.name, TRUE);

MERGE INTO skill t
USING (
    VALUES
        ('后端开发', 'DEV'),
        ('前端开发', 'DEV'),
        ('项目管理', 'PM'),
        ('业务分析', 'BA'),
        ('测试', 'QA'),
        ('UI设计', 'UX')
) s(name, domain)
ON t.name = s.name
WHEN NOT MATCHED THEN
    INSERT (name, role_domain_id, active)
    VALUES (s.name, (SELECT r.id FROM role r WHERE r.name = s.domain), TRUE);

MERGE INTO app_user t
USING (
    VALUES
        ('coordinator', 'COORDINATOR', 'Resource coordinator',
            '$2a$10$4RT8SigSVGBz4QwxE/O.Ne7I8e6HoWxoR0uxsdKNIV/qD.roIMO3K'),
        ('lead.one', 'LEAD', 'Project lead one',
            '$2a$10$aj6lMq.6lQvCOYCjElCI1uR4crzWqenK/1paGZ/N7k7Rp2V5ADXPi'),
        ('lead.two', 'LEAD', 'Project lead two',
            '$2a$10$2.ur37aHvaDvS0nLkjtl1uM3EtwHza6gEGSm2bEYO9345fca6N7ca')
) s(username, role, display_name, password_hash)
ON t.username = s.username
WHEN NOT MATCHED THEN
    INSERT (username, password_hash, role, display_name)
    VALUES (s.username, s.password_hash, s.role, s.display_name);
