import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { create, getNumericDate } from "https://deno.land/x/djwt@v3.0.2/mod.ts"

const FCM_PROJECT_ID = Deno.env.get('FCM_PROJECT_ID')
const FCM_SERVICE_ACCOUNT = JSON.parse(Deno.env.get('FCM_SERVICE_ACCOUNT') || '{}')

serve(async (req) => {
  const { website_id, title, body } = await req.json()

  // 1. Initialize Supabase Admin
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  )

  // 2. Fetch subscribers
  const { data: subscribers, error } = await supabase
    .from('subscribers')
    .select('fcm_token')
    .eq('website_id', website_id)

  if (error || !subscribers) {
    return new Response(JSON.stringify({ error: 'Failed to fetch subscribers' }), { status: 500 })
  }

  const tokens = subscribers.map(s => s.fcm_token)

  // 3. Get OAuth2 access token and send via FCM HTTP v1 API
  let accessToken: string
  try {
    accessToken = await getAccessToken(FCM_SERVICE_ACCOUNT)
  } catch (e) {
    return new Response(JSON.stringify({ error: `Failed to get FCM access token: ${e.message}` }), { status: 500 })
  }

  const results = await Promise.all(tokens.map(token =>
    fetch(`https://fcm.googleapis.com/v1/projects/${FCM_PROJECT_ID}/messages:send`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: {
          token: token,
          notification: { title, body },
          data: { website_id }
        }
      })
    })
  ))

  const failed = results.filter(r => !r.ok).length

  // 4. Log the notification
  await supabase.from('notifications').insert({ website_id, title, body })

  return new Response(JSON.stringify({ success: true, count: tokens.length, failed }), {
    headers: { "Content-Type": "application/json" },
  })
})

/**
 * Generates a short-lived Google OAuth2 access token using a service account
 * private key by creating a signed JWT and exchanging it at Google's token endpoint.
 */
async function getAccessToken(serviceAccount: {
  client_email: string
  private_key: string
}): Promise<string> {
  const now = getNumericDate(0)
  const exp = getNumericDate(60 * 60) // 1 hour

  // Import the RSA private key from PEM
  const pemHeader = "-----BEGIN PRIVATE KEY-----"
  const pemFooter = "-----END PRIVATE KEY-----"
  const pemContents = serviceAccount.private_key
    .replace(pemHeader, "")
    .replace(pemFooter, "")
    .replace(/\s/g, "")

  const binaryDer = Uint8Array.from(atob(pemContents), c => c.charCodeAt(0))

  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  )

  const jwt = await create(
    { alg: "RS256", typ: "JWT" },
    {
      iss: serviceAccount.client_email,
      sub: serviceAccount.client_email,
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
    },
    privateKey
  )

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  })

  if (!tokenResponse.ok) {
    const err = await tokenResponse.text()
    throw new Error(`Token exchange failed: ${err}`)
  }

  const { access_token } = await tokenResponse.json()
  return access_token
}