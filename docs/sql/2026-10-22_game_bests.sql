-- R1: game_bests + save_game_result (docs/GAMES_COOL_PLAN.md section 4.1)
-- STATUS: PROPOSED 2026-10-22, NOT applied yet (waiting for the owner's "yes").
-- One row per student per (game, scope_path, chapter, level). Own rows only. No guest rows.

create table if not exists public.game_bests (
  user_id uuid not null references auth.users(id) on delete cascade,
  game text not null check (game in ('defense', 'match')),
  scope_path text not null,
  chapter int not null,
  level text not null check (level in ('easy', 'medium', 'hard')),
  best_stars int not null default 0 check (best_stars between 0 and 3),
  best_score int not null default 0 check (best_score between 0 and 100000),
  plays int not null default 0,
  wins int not null default 0,
  last_stars int not null default 0 check (last_stars between 0 and 3),
  last_score int not null default 0 check (last_score between 0 and 100000),
  last_played timestamptz not null default now(),
  primary key (user_id, game, scope_path, chapter, level)
);

alter table public.game_bests enable row level security;

create policy game_bests_select_own on public.game_bests
  for select to authenticated using (user_id = (select auth.uid()));
create policy game_bests_insert_own on public.game_bests
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy game_bests_update_own on public.game_bests
  for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create or replace function public.save_game_result(
  p_game text,
  p_scope_path text,
  p_chapter int,
  p_level text,
  p_stars int,
  p_score int,
  p_won boolean
) returns void
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_stars int := greatest(0, least(coalesce(p_stars, 0), 3));
  v_score int := greatest(0, least(coalesce(p_score, 0), 100000));
begin
  if v_uid is null then
    raise exception 'not signed in';
  end if;
  insert into public.game_bests as g
    (user_id, game, scope_path, chapter, level, best_stars, best_score,
     plays, wins, last_stars, last_score, last_played)
  values
    (v_uid, p_game, p_scope_path, p_chapter, p_level, v_stars, v_score,
     1, case when p_won then 1 else 0 end, v_stars, v_score, now())
  on conflict (user_id, game, scope_path, chapter, level) do update set
    best_stars = greatest(g.best_stars, excluded.best_stars),
    best_score = greatest(g.best_score, excluded.best_score),
    plays = g.plays + 1,
    wins = g.wins + case when p_won then 1 else 0 end,
    last_stars = excluded.last_stars,
    last_score = excluded.last_score,
    last_played = now();
end;
$$;

revoke all on function public.save_game_result(text, text, int, text, int, int, boolean) from public, anon;
grant execute on function public.save_game_result(text, text, int, text, int, int, boolean) to authenticated;

-- Checks after applying: rowsecurity = true on game_bests; 3 policies; anon cannot execute save_game_result.
