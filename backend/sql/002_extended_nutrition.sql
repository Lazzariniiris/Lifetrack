alter table public.meal_analyses add column if not exists fiber_g double precision not null default 0 check (fiber_g >= 0);
alter table public.meal_analyses add column if not exists sugars_g double precision not null default 0 check (sugars_g >= 0);
alter table public.meal_analyses add column if not exists sodium_mg double precision not null default 0 check (sodium_mg >= 0);
