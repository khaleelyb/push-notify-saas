import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

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

  // 3. Trigger FCM (Batch Send)
  // Note: This requires getting an OAuth2 token for Firebase Admin
  // For brevity, this logic assumes you have a helper to get the access token.
  const accessToken = await getAccessToken(FCM_SERVICE_ACCOUNT)

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

  // 4. Log the notification
  await supabase.from('notifications').insert({ website_id, title, body })

  return new Response(JSON.stringify({ success: true, count: tokens.length }), {
    headers: { "Content-Type": "application/json" },
  })
})

// Helper function to get Google OAuth2 Access Token
async function getAccessToken(serviceAccount: any) {
  // Implementation for generating JWT and fetching token from https://oauth2.googleapis.com/token
  // Usually involves libraries like 'google-auth-library' or custom JWT logic in Deno
  return "YOUR_GENERATED_ACCESS_TOKEN"
}
