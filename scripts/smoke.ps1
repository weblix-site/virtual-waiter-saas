# Minimal integration smoke tests for public flows and hall plans.
# Requires: PowerShell 5+, curl (Invoke-WebRequest is ok too), python (optional).

param(
  [string]$ApiBase = $env:API_BASE,
  [string]$AdminUser = $env:ADMIN_USER,
  [string]$AdminPass = $env:ADMIN_PASS,
  [string]$TablePublicId = $env:TABLE_PUBLIC_ID,
  [string]$PhoneE164 = $env:PHONE_E164,
  [string]$Locale = $env:LOCALE
)

if (-not $ApiBase) { $ApiBase = "http://localhost:8080" }
if (-not $AdminUser) { $AdminUser = "admin1" }
if (-not $AdminPass) { $AdminPass = "demo123" }
if (-not $PhoneE164) { $PhoneE164 = "+37369000000" }
if (-not $Locale) { $Locale = "ru" }

if (-not $TablePublicId) {
  Write-Host "Set TABLE_PUBLIC_ID before running."
  exit 1
}

function Invoke-Json($method, $url, $body = $null, $headers = $null) {
  $options = @{
    Method = $method
    Uri = $url
    Headers = $headers
  }
  if ($body) {
    $options["Body"] = ($body | ConvertTo-Json -Depth 6)
    $options["ContentType"] = "application/json"
  }
  $resp = Invoke-RestMethod @options
  return $resp
}

$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$AdminUser`:$AdminPass"))
$adminHeaders = @{ Authorization = "Basic $basic" }

Write-Host "==> Fetch signed URL for table $TablePublicId"
$signed = Invoke-Json "GET" "$ApiBase/api/admin/tables/$TablePublicId/signed-url" $null $adminHeaders
$signedUrl = $signed.url
$uri = [System.Uri]$signedUrl
$query = [System.Web.HttpUtility]::ParseQueryString($uri.Query)
$sig = $query["sig"]
$ts = $query["ts"]

Write-Host "==> Start session"
$session = Invoke-Json "POST" "$ApiBase/api/public/session/start" @{
  tablePublicId = $TablePublicId
  sig = $sig
  ts = [int64]$ts
  locale = $Locale
}
$guestSessionId = $session.guestSessionId
$sessionSecret = $session.sessionSecret
$otpRequired = $session.otpRequired

if ($otpRequired -eq $true) {
  Write-Host "==> OTP send/verify"
  $otpSend = Invoke-Json "POST" "$ApiBase/api/public/otp/send" @{
    guestSessionId = $guestSessionId
    phoneE164 = $PhoneE164
    locale = $Locale
  } @{ "X-Session-Secret" = $sessionSecret }
  if ($otpSend.devCode) {
    $null = Invoke-Json "POST" "$ApiBase/api/public/otp/verify" @{
      guestSessionId = $guestSessionId
      challengeId = $otpSend.challengeId
      code = $otpSend.devCode
    } @{ "X-Session-Secret" = $sessionSecret }
  } else {
    Write-Host "OTP devCode is empty; verify manually."
  }
}

Write-Host "==> Load menu"
$menu = Invoke-Json "GET" "$ApiBase/api/public/menu?tablePublicId=$TablePublicId&sig=$sig&ts=$ts&locale=$Locale"
$firstItemId = $null
foreach ($c in $menu.categories) {
  if ($c.items -and $c.items.Count -gt 0) { $firstItemId = $c.items[0].id; break }
}
if (-not $firstItemId) { Write-Host "No menu items found."; exit 1 }

Write-Host "==> Create order"
$order = Invoke-Json "POST" "$ApiBase/api/public/orders" @{
  guestSessionId = $guestSessionId
  items = @(@{ menuItemId = $firstItemId; qty = 1 })
} @{ "X-Session-Secret" = $sessionSecret }

Write-Host "==> Create party"
$party = Invoke-Json "POST" "$ApiBase/api/public/party/create" @{
  guestSessionId = $guestSessionId
} @{ "X-Session-Secret" = $sessionSecret }

Write-Host "==> Create bill request"
$null = Invoke-Json "POST" "$ApiBase/api/public/bill-request/create" @{
  guestSessionId = $guestSessionId
  mode = "MY"
  paymentMethod = "CASH"
  tipsPercent = 5
} @{ "X-Session-Secret" = $sessionSecret }

Write-Host "==> Hall plans versions"
$halls = Invoke-Json "GET" "$ApiBase/api/admin/halls" $null $adminHeaders
if ($halls.Count -gt 0) {
  $hallId = $halls[0].id
  $plans = Invoke-Json "GET" "$ApiBase/api/admin/halls/$hallId/plans" $null $adminHeaders
  if ($plans.Count -gt 0) {
    $planId = $plans[0].id
    $null = Invoke-Json "GET" "$ApiBase/api/admin/hall-plans/$planId/versions" $null $adminHeaders
  } else {
    Write-Host "No plans found; skip versions check."
  }
} else {
  Write-Host "No halls found; skip versions check."
}

Write-Host "Smoke tests completed."
