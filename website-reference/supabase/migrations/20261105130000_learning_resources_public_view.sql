-- Public catalog view: expose only list metadata and mask private Appwrite file IDs.
-- SECURITY INVOKER preserves the underlying learning_resources RLS policies.

create or replace view public.learning_resources_public
with (security_invoker = true)
as
select
  id,
  package_id,
  scope_path,
  hub,
  title,
  chapter,
  sort_order,
  content_type,
  case
    when lower(coalesce(storage_path, '')) like 'appwrite-paid:%' then 'appwrite-paid:'
    else storage_path
  end as storage_path,
  published,
  created_at,
  item_count,
  size_bytes,
  description
from public.learning_resources
where published = true;

grant select on public.learning_resources_public to anon, authenticated;
