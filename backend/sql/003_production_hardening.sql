begin;

alter table public.profiles add column if not exists timezone text not null default 'UTC';
alter table public.profiles add column if not exists locale text not null default 'en-US';
alter table public.profiles add column if not exists measurement_system text not null default 'metric';
alter table public.profiles add column if not exists daily_water_goal_ml integer not null default 2000;
alter table public.profiles add column if not exists daily_calorie_goal integer;
alter table public.profiles add column if not exists health_goal text;
alter table public.profiles add column if not exists weight_kg double precision;
alter table public.profiles add column if not exists height_cm double precision;
alter table public.profiles add column if not exists activity_level text;
alter table public.profiles add column if not exists nutrition_preferences jsonb not null default '{}'::jsonb;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'profiles_timezone_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_timezone_check check (char_length(timezone) between 1 and 64);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_locale_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_locale_check check (char_length(locale) between 2 and 16);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_measurement_system_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_measurement_system_check check (measurement_system in ('metric', 'imperial'));
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_daily_water_goal_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_daily_water_goal_check check (daily_water_goal_ml between 250 and 10000);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_daily_calorie_goal_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_daily_calorie_goal_check check (daily_calorie_goal is null or daily_calorie_goal between 500 and 10000);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_health_goal_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_health_goal_check check (health_goal is null or char_length(health_goal) between 1 and 200);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_weight_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_weight_check check (weight_kg is null or weight_kg between 20 and 500);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_height_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_height_check check (height_cm is null or height_cm between 80 and 250);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_activity_check' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_activity_check check (activity_level is null or activity_level in ('sedentary','light','moderate','active','very_active'));
  end if;
end $$;

create unique index if not exists habits_id_user_unique_idx on public.habits(id, user_id);

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'habit_logs_habit_owner_fkey' and conrelid = 'public.habit_logs'::regclass) then
    alter table public.habit_logs
      add constraint habit_logs_habit_owner_fkey
      foreign key (habit_id, user_id) references public.habits(id, user_id)
      on delete cascade not valid;
  end if;
end $$;
alter table public.habit_logs drop constraint if exists habit_logs_habit_id_fkey;

drop policy if exists "Users select own rows" on public.habit_logs;
drop policy if exists "Users insert own rows" on public.habit_logs;
drop policy if exists "Users update own rows" on public.habit_logs;
drop policy if exists "Users delete own rows" on public.habit_logs;
create policy "Users select own rows" on public.habit_logs for select
  using (auth.uid() = user_id);
create policy "Users insert own rows" on public.habit_logs for insert
  with check (
    auth.uid() = user_id
    and exists (select 1 from public.habits where habits.id = habit_id and habits.user_id = auth.uid())
  );
create policy "Users update own rows" on public.habit_logs for update
  using (auth.uid() = user_id)
  with check (
    auth.uid() = user_id
    and exists (select 1 from public.habits where habits.id = habit_id and habits.user_id = auth.uid())
  );
create policy "Users delete own rows" on public.habit_logs for delete
  using (auth.uid() = user_id);

alter table public.meal_analyses add column if not exists status text not null default 'completed';
alter table public.meal_analyses add column if not exists photo_path text;
alter table public.meal_analyses add column if not exists confidence double precision not null default 0;
alter table public.meal_analyses add column if not exists observations jsonb not null default '[]'::jsonb;
alter table public.meal_analyses add column if not exists corrections_json jsonb not null default '{}'::jsonb;
alter table public.meal_analyses alter column calories drop not null;
alter table public.meal_analyses alter column protein_g drop not null;
alter table public.meal_analyses alter column carbs_g drop not null;
alter table public.meal_analyses alter column fat_g drop not null;
alter table public.meal_analyses alter column fiber_g drop not null;
alter table public.meal_analyses alter column sugars_g drop not null;
alter table public.meal_analyses alter column sodium_mg drop not null;

update public.meal_analyses
set observations = foods_json -> 'observations'
where jsonb_typeof(foods_json) = 'object'
  and jsonb_typeof(foods_json -> 'observations') = 'array'
  and observations = '[]'::jsonb;

update public.meal_analyses
set confidence = least(1, greatest(0, (foods_json ->> 'confidence')::double precision))
where jsonb_typeof(foods_json) = 'object'
  and (foods_json ->> 'confidence') ~ '^[0-9]+([.][0-9]+)?$';

update public.meal_analyses
set foods_json = foods_json -> 'foods'
where jsonb_typeof(foods_json) = 'object'
  and jsonb_typeof(foods_json -> 'foods') = 'array';

do $$ begin
  alter table public.meal_analyses drop constraint if exists meal_analyses_status_check;
  alter table public.meal_analyses add constraint meal_analyses_status_check check (status in ('pending', 'completed', 'corrected', 'failed'));
  if not exists (select 1 from pg_constraint where conname = 'meal_analyses_confidence_check' and conrelid = 'public.meal_analyses'::regclass) then
    alter table public.meal_analyses add constraint meal_analyses_confidence_check check (confidence between 0 and 1);
  end if;
  alter table public.meal_analyses drop constraint if exists meal_analyses_foods_json_check;
  alter table public.meal_analyses add constraint meal_analyses_foods_json_check check (
    jsonb_typeof(foods_json) = 'array'
    and ((status = 'pending' and jsonb_array_length(foods_json) = 0) or (status <> 'pending' and jsonb_array_length(foods_json) between 1 and 25))
  ) not valid;
  if not exists (select 1 from pg_constraint where conname = 'meal_analyses_observations_check' and conrelid = 'public.meal_analyses'::regclass) then
    alter table public.meal_analyses add constraint meal_analyses_observations_check check (jsonb_typeof(observations) = 'array' and jsonb_array_length(observations) <= 10);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'meal_analyses_corrections_check' and conrelid = 'public.meal_analyses'::regclass) then
    alter table public.meal_analyses add constraint meal_analyses_corrections_check check (jsonb_typeof(corrections_json) = 'object');
  end if;
  if not exists (select 1 from pg_constraint where conname = 'meal_analyses_photo_path_check' and conrelid = 'public.meal_analyses'::regclass) then
    alter table public.meal_analyses add constraint meal_analyses_photo_path_check check (
      photo_path is null
      or (
        char_length(photo_path) between 3 and 512
        and photo_path like user_id::text || '/%'
        and position('..' in photo_path) = 0
        and position(E'\\' in photo_path) = 0
      )
    );
  end if;
end $$;

create index if not exists meal_analyses_user_status_created_idx
  on public.meal_analyses(user_id, status, created_at desc)
  where deleted_at is null;

create table if not exists public.meal_analysis_quotas (
  user_id uuid not null references auth.users(id) on delete cascade,
  quota_date date not null default current_date,
  request_count integer not null default 0 check (request_count >= 0),
  updated_at timestamptz not null default now(),
  primary key (user_id, quota_date)
);
alter table public.meal_analysis_quotas enable row level security;
drop policy if exists "Users read own analysis quota" on public.meal_analysis_quotas;
create policy "Users read own analysis quota" on public.meal_analysis_quotas for select to authenticated using (auth.uid() = user_id);

create or replace function public.consume_meal_analysis_quota(limit_value integer default 50)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  current_user_id uuid := auth.uid();
  accepted uuid;
begin
  if current_user_id is null or limit_value < 1 or limit_value > 1000 then
    return false;
  end if;
  insert into public.meal_analysis_quotas(user_id, quota_date, request_count, updated_at)
  values (current_user_id, current_date, 1, now())
  on conflict (user_id, quota_date) do update
    set request_count = public.meal_analysis_quotas.request_count + 1, updated_at = now()
    where public.meal_analysis_quotas.request_count < limit_value
  returning user_id into accepted;
  return accepted is not null;
end;
$$;
revoke all on function public.consume_meal_analysis_quota(integer) from public;
grant execute on function public.consume_meal_analysis_quota(integer) to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'meal-images',
  'meal-images',
  false,
  8000000,
  array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update set
  public = false,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Users read own meal images" on storage.objects;
drop policy if exists "Users upload own meal images" on storage.objects;
drop policy if exists "Users update own meal images" on storage.objects;
drop policy if exists "Users delete own meal images" on storage.objects;
create policy "Users read own meal images" on storage.objects for select to authenticated
  using (bucket_id = 'meal-images' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "Users upload own meal images" on storage.objects for insert to authenticated
  with check (bucket_id = 'meal-images' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "Users update own meal images" on storage.objects for update to authenticated
  using (bucket_id = 'meal-images' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'meal-images' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "Users delete own meal images" on storage.objects for delete to authenticated
  using (bucket_id = 'meal-images' and (storage.foldername(name))[1] = auth.uid()::text);

commit;
