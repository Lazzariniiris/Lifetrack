create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger language plpgsql security invoker set search_path = '' as $$
begin
  new.updated_at = now();
  new.version = old.version + 1;
  return new;
end;
$$;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  version bigint not null default 1
);

create table if not exists public.habits (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 80),
  description text,
  target_type text not null check (target_type in ('YES_NO','QUANTITY','DURATION','REPETITIONS')),
  target_value integer not null check (target_value between 1 and 10000),
  color bigint not null,
  is_active boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version bigint not null default 1
);

create table if not exists public.habit_logs (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  habit_id uuid not null references public.habits(id) on delete cascade,
  logged_at timestamptz not null,
  value integer not null check (value between 1 and 10000),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version bigint not null default 1
);

create table if not exists public.water_entries (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  amount_ml integer not null check (amount_ml between 1 and 2000),
  logged_at timestamptz not null,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version bigint not null default 1
);

create table if not exists public.sleep_entries (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  bedtime timestamptz not null,
  wake_time timestamptz not null,
  quality integer not null check (quality between 1 and 5),
  notes text,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version bigint not null default 1,
  check (wake_time > bedtime and wake_time <= bedtime + interval '24 hours')
);

create table if not exists public.meal_analyses (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  foods_json jsonb not null,
  calories double precision not null check (calories >= 0),
  protein_g double precision not null check (protein_g >= 0),
  carbs_g double precision not null check (carbs_g >= 0),
  fat_g double precision not null check (fat_g >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version bigint not null default 1
);

create index if not exists habits_user_updated_idx on public.habits(user_id, updated_at desc);
create index if not exists habit_logs_user_updated_idx on public.habit_logs(user_id, updated_at desc);
create index if not exists habit_logs_habit_logged_idx on public.habit_logs(habit_id, logged_at desc);
create index if not exists water_entries_user_logged_idx on public.water_entries(user_id, logged_at desc);
create index if not exists sleep_entries_user_wake_idx on public.sleep_entries(user_id, wake_time desc);
create index if not exists meal_analyses_user_created_idx on public.meal_analyses(user_id, created_at desc);

do $$ declare table_name text; begin
  foreach table_name in array array['profiles','habits','habit_logs','water_entries','sleep_entries','meal_analyses'] loop
    execute format('alter table public.%I enable row level security', table_name);
    execute format('drop policy if exists "Users select own rows" on public.%I', table_name);
    execute format('drop policy if exists "Users insert own rows" on public.%I', table_name);
    execute format('drop policy if exists "Users update own rows" on public.%I', table_name);
    execute format('drop policy if exists "Users delete own rows" on public.%I', table_name);
    if table_name = 'profiles' then
      execute format('create policy "Users select own rows" on public.%I for select using (auth.uid() = id)', table_name);
      execute format('create policy "Users insert own rows" on public.%I for insert with check (auth.uid() = id)', table_name);
      execute format('create policy "Users update own rows" on public.%I for update using (auth.uid() = id) with check (auth.uid() = id)', table_name);
      execute format('create policy "Users delete own rows" on public.%I for delete using (auth.uid() = id)', table_name);
    else
      execute format('create policy "Users select own rows" on public.%I for select using (auth.uid() = user_id)', table_name);
      execute format('create policy "Users insert own rows" on public.%I for insert with check (auth.uid() = user_id)', table_name);
      execute format('create policy "Users update own rows" on public.%I for update using (auth.uid() = user_id) with check (auth.uid() = user_id)', table_name);
      execute format('create policy "Users delete own rows" on public.%I for delete using (auth.uid() = user_id)', table_name);
    end if;
  end loop;
end $$;

do $$ declare table_name text; begin
  foreach table_name in array array['profiles','habits','habit_logs','water_entries','sleep_entries','meal_analyses'] loop
    execute format('drop trigger if exists set_%I_updated_at on public.%I', table_name, table_name);
    execute format('create trigger set_%I_updated_at before update on public.%I for each row execute function public.set_updated_at()', table_name, table_name);
  end loop;
end $$;

create or replace function public.create_profile_for_new_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles(id, display_name) values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1))) on conflict (id) do nothing;
  return new;
end;
$$;
drop trigger if exists create_profile_after_signup on auth.users;
create trigger create_profile_after_signup after insert on auth.users for each row execute function public.create_profile_for_new_user();
