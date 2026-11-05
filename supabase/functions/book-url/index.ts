// book-url: issues short-lived Appwrite file URLs only after the Supabase access check.
// Legacy clients omit client_version and use open_book(uuid), which applies the legacy v3 gate.
// Replacement clients send client_version and use open_book_v2(uuid, integer).
const APPWRITE_ENDPOINT = "https://fra.cloud.appwrite.io/v1";
const APPWRITE_PROJECT = "6aa9363c000f7ad5ebf7";
const PAID_BUCKET = "6ad71e3d00318c09972f";
const PAID_PREFIX = "appwrite-paid:";
const TOKEN_MINUTES = 10;

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, "Content-Type": "application/json", "Cache-Control": "no-store" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });
  if (req.method !== "POST") return json({ status: "error", message: "POST only" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.toLowerCase().startsWith("bearer ")) return json({ status: "not_signed_in" }, 401);

  let id = "";
  let clientVersion: number | null = null;
  let checkOnly = false;
  let clientType: "app" | "web" = "app";
  try {
    const body = await req.json();
    id = String(body?.id ?? "");
    checkOnly = body?.check_only === true;
    clientType = body?.client_type === "web" ? "web" : "app";
    const rawVersion = body?.client_version;
    if (rawVersion !== undefined && rawVersion !== null) {
      const parsed = Number(rawVersion);
      if (!Number.isSafeInteger(parsed) || parsed < 1) {
        return json({ status: "error", message: "Bad client version" }, 400);
      }
      clientVersion = parsed;
    }
  } catch (_) {
    return json({ status: "error", message: "Bad request" }, 400);
  }
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)) {
    return json({ status: "error", message: "Bad id" }, 400);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const appwriteKey = Deno.env.get("APPWRITE_PAID_TOKEN_KEY") ?? Deno.env.get("APPWRITE_API_KEY");
  if (!supabaseUrl || !anonKey || !appwriteKey) {
    console.error("book-url: missing configuration", {
      supabaseUrl: !!supabaseUrl, anonKey: !!anonKey, appwriteKey: !!appwriteKey,
    });
    return json({ status: "error", message: "Server not configured" }, 500);
  }

  // Ask the database with the student's own JWT, never with a service key.
  const rpcName = clientType === "web"
    ? "open_book_web"
    : clientVersion === null
      ? "open_book"
      : "open_book_v2";
  const rpcBody = clientType === "web"
    ? { p_id: id }
    : clientVersion === null
      ? { p_id: id }
      : { p_id: id, p_client_version: clientVersion };
  const gate = await fetch(`${supabaseUrl}/rest/v1/rpc/${rpcName}`, {
    method: "POST",
    headers: { apikey: anonKey, Authorization: authHeader, "Content-Type": "application/json" },
    body: JSON.stringify(rpcBody),
  });
  if (!gate.ok) {
    console.error("book-url: access RPC http", gate.status);
    return json({ status: "error", message: "Could not check access" }, 502);
  }
  const decision = await gate.json();
  if (decision?.status === "not_signed_in") return json({ status: "not_signed_in" }, 401);
  if (decision?.status === "not_found") return json({ status: "not_found" }, 404);
  if (decision?.status === "locked") return json({ status: "locked", gate: decision.gate ?? "coming_soon" }, 403);
  if (decision?.status === "device_not_active") return json({ status: "device_not_active" }, 403);
  if (decision?.status === "update_required") {
    return json({ status: "update_required", min_version_code: decision.min_version_code ?? null }, 426);
  }
  if (decision?.status !== "ok") return json({ status: "error", message: "Unexpected answer" }, 502);

  const path = String(decision.storage_path ?? "");
  if (!path.startsWith(PAID_PREFIX)) return json({ status: "not_paid_file" }, 400);
  const fileId = path.slice(PAID_PREFIX.length);
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]{0,35}$/.test(fileId)) {
    return json({ status: "error", message: "Bad file id" }, 500);
  }

  if (checkOnly) return json({ status: "ok" });

  const expires = new Date(Date.now() + TOKEN_MINUTES * 60 * 1000);
  const tokenRes = await fetch(
    `${APPWRITE_ENDPOINT}/tokens/buckets/${PAID_BUCKET}/files/${fileId}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Appwrite-Project": APPWRITE_PROJECT,
        "X-Appwrite-Key": appwriteKey,
      },
      body: JSON.stringify({ expire: expires.toISOString() }),
    },
  );
  if (!tokenRes.ok) {
    let kind = "";
    try { kind = String((await tokenRes.json())?.type ?? ""); } catch (_) { /* ignore */ }
    console.error("book-url: appwrite token http", tokenRes.status, kind);
    return json({ status: "error", message: "Could not prepare the file" }, 502);
  }
  const token = await tokenRes.json();
  const secret = String(token?.secret ?? "");
  if (!secret) {
    console.error("book-url: Appwrite response had no token secret");
    return json({ status: "error", message: "Could not prepare the file" }, 502);
  }

  const base = `${APPWRITE_ENDPOINT}/storage/buckets/${PAID_BUCKET}/files/${fileId}`;
  const query = `project=${APPWRITE_PROJECT}&token=${encodeURIComponent(secret)}`;
  return json({
    status: "ok",
    url: `${base}/view?${query}`,
    download_url: `${base}/download?${query}`,
    expires_at: expires.toISOString(),
  });
});
