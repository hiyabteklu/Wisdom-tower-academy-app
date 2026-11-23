-- E4/E6: inactive price rows for ECE Years 4 and 5.
-- Owner price: 300 ETB for every ECE package. Database activation is deferred.
begin;
insert into public.catalog_items
  (id, name, short_name, description, price_etb, href, image, includes, enrolled_label, group_key, active, sort_order)
values
  ('ece-y4-sem-1','ECE Year 4 - Semester 1','ECE Y4 S1','Electrical & Computer Engineering 4th year - first semester.',300,'/academy/special-packages/electrical-computer-engineering/y4-sem-1','/images/special-packages/ece-sem-1.jpg',array['Semester 1','Learning hubs per course'],'Special track','special',false,170),
  ('ece-y4-sem-2','ECE Year 4 - Semester 2','ECE Y4 S2','Electrical & Computer Engineering 4th year - second semester.',300,'/academy/special-packages/electrical-computer-engineering/y4-sem-2','/images/special-packages/ece-sem-2.jpg',array['Semester 2','Stream courses','Learning hubs per course'],'Special track','special',false,180),
  ('ece-y5-sem-1','ECE Year 5 - Semester 1','ECE Y5 S1','Electrical & Computer Engineering 5th year - first semester.',300,'/academy/special-packages/electrical-computer-engineering/y5-sem-1','/images/special-packages/ece-sem-1.jpg',array['Semester 1','Stream courses','Learning hubs per course'],'Special track','special',false,190),
  ('ece-y5-sem-2','ECE Year 5 - Semester 2','ECE Y5 S2','Electrical & Computer Engineering 5th year - second semester.',300,'/academy/special-packages/electrical-computer-engineering/y5-sem-2','/images/special-packages/ece-sem-2.jpg',array['Semester 2','Stream courses','Learning hubs per course'],'Special track','special',false,200)
on conflict (id) do nothing;
select id, price_etb, active from public.catalog_items where id like 'ece-y[45]-%' order by id;
rollback;
