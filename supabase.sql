-- 1) 基础扩展（Supabase 通常已启用）
create extension if not exists pgcrypto;

-- 2) spaces 表
create table if not exists public.spaces (
  id uuid primary key default gen_random_uuid(),
  code text not null,
  name text,
  owner_id uuid not null references auth.users(id) on delete cascade,
  enabled boolean not null default true,
  created_at timestamptz not null default now()
);

-- code 唯一
create unique index if not exists spaces_code_unique on public.spaces (code);

-- code 仅允许 6 位字母数字（不排除易混淆字符，留给应用层）
alter table public.spaces
  add constraint spaces_code_format
  check (length(code) = 6 and code ~ '^[A-Za-z0-9]{6}$');

-- 3) 成员表
create table if not exists public.space_memberships (
  id uuid primary key default gen_random_uuid(),
  space_id uuid not null references public.spaces(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'member',
  joined_at timestamptz not null default now()
);

-- 一个用户对同一个 space 只能加入一次
create unique index if not exists space_memberships_unique
  on public.space_memberships (space_id, user_id);

-- 4) 自动把创建者加入成员表
create or replace function public.add_space_owner_membership()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.space_memberships(space_id, user_id, role)
  values (new.id, new.owner_id, 'owner')
  on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists trg_add_space_owner_membership on public.spaces;

create trigger trg_add_space_owner_membership
after insert on public.spaces
for each row
execute function public.add_space_owner_membership();

-- 5) RLS
alter table public.spaces enable row level security;
alter table public.space_memberships enable row level security;

-- spaces: 只允许看“自己创建的”或“自己加入的”
create policy "spaces_select"
on public.spaces
for select
using (
  owner_id = auth.uid()
  or exists (
    select 1 from public.space_memberships m
    where m.space_id = spaces.id and m.user_id = auth.uid()
  )
);

-- spaces: 只允许自己创建
create policy "spaces_insert"
on public.spaces
for insert
with check (owner_id = auth.uid());

-- 可选：只允许创建者更新（比如改名/启用）
create policy "spaces_update"
on public.spaces
for update
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

-- memberships: 允许自己查看
create policy "memberships_select"
on public.space_memberships
for select
using (user_id = auth.uid());

-- memberships: 允许自己加入启用的 space
create policy "memberships_insert"
on public.space_memberships
for insert
with check (
  user_id = auth.uid()
  and exists (
    select 1 from public.spaces s
    where s.id = space_id and s.enabled = true
  )
);

-- memberships: 允许自己退出
create policy "memberships_delete"
on public.space_memberships
for delete
using (user_id = auth.uid());
