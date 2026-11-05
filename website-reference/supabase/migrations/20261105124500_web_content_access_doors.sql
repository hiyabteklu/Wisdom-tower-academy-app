-- Website-only checked access doors. Unlike the app-specific v2 doors, these do not
-- apply an APK minimum-version gate; they still enforce sign-in, device state,
-- publication, admin status and the same content_guard entitlement rules.

create or replace function public.open_resource_web(p_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  r public.learning_resources%rowtype;
  v_mode text;
begin
  if auth.uid() is null then
    return jsonb_build_object('status', 'not_signed_in');
  end if;

  if not private.is_admin()
     and exists (select 1 from public.account_devices d where d.user_id = auth.uid())
     and not private.is_active_device_session() then
    return jsonb_build_object('status', 'device_not_active');
  end if;

  select * into r
  from public.learning_resources
  where id = p_id and published = true;

  if not found then
    return jsonb_build_object('status', 'not_found');
  end if;

  if private.is_admin()
     or content_guard.resource_visible(r.package_id, r.scope_path, r.hub, r.chapter, r.id) then
    return jsonb_build_object('status', 'ok', 'row', to_jsonb(r));
  end if;

  v_mode := content_guard.effective_mode(r.package_id, r.scope_path, r.hub, r.chapter, r.id);
  return jsonb_build_object(
    'status', 'locked',
    'gate', case when v_mode = 'require_purchase' then 'package_required' else 'coming_soon' end
  );
end;
$function$;

create or replace function public.open_book_web(p_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  r public.learning_resources%rowtype;
  v_mode text;
begin
  if auth.uid() is null then
    return jsonb_build_object('status', 'not_signed_in');
  end if;

  if not private.is_admin()
     and exists (select 1 from public.account_devices d where d.user_id = auth.uid())
     and not private.is_active_device_session() then
    return jsonb_build_object('status', 'device_not_active');
  end if;

  select * into r
  from public.learning_resources
  where id = p_id and published = true;

  if not found or r.storage_path is null then
    return jsonb_build_object('status', 'not_found');
  end if;

  if private.is_admin()
     or content_guard.resource_visible(r.package_id, r.scope_path, r.hub, r.chapter, r.id) then
    return jsonb_build_object('status', 'ok', 'storage_path', r.storage_path);
  end if;

  v_mode := content_guard.effective_mode(r.package_id, r.scope_path, r.hub, r.chapter, r.id);
  return jsonb_build_object(
    'status', 'locked',
    'gate', case when v_mode = 'require_purchase' then 'package_required' else 'coming_soon' end
  );
end;
$function$;

revoke all on function public.open_resource_web(uuid) from public, anon, service_role;
revoke all on function public.open_book_web(uuid) from public, anon, service_role;
grant execute on function public.open_resource_web(uuid) to authenticated;
grant execute on function public.open_book_web(uuid) to authenticated;
